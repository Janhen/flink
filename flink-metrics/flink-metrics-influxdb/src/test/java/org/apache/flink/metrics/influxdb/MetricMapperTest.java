/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.metrics.influxdb;

import okhttp3.OkHttpClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import org.apache.commons.lang3.time.DateUtils;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.metrics.util.TestHistogram;
import org.apache.flink.metrics.util.TestMeter;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogram;

import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;

import org.apache.flink.util.TestLogger;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.metrics.influxdb.InfluxdbReporterOptions.*;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link MetricMapper} checking that metrics are converted to InfluxDB client objects as
 * expected.
 */
public class MetricMapperTest extends TestLogger {

    private static final String NAME = "a-metric-name";
    private static final MeasurementInfo INFO = getMeasurementInfo(NAME);
    private static final Instant TIMESTAMP = Instant.now();

    @Test
    public void testMapGauge() {
        verifyPoint(MetricMapper.map(INFO, TIMESTAMP, (Gauge<Number>) () -> 42), "value=42");

        verifyPoint(MetricMapper.map(INFO, TIMESTAMP, (Gauge<Number>) () -> null), "value=null");

        verifyPoint(
                MetricMapper.map(INFO, TIMESTAMP, (Gauge<String>) () -> "hello"), "value=hello");

        verifyPoint(MetricMapper.map(INFO, TIMESTAMP, (Gauge<Long>) () -> 42L), "value=42");
    }

    @Test
    public void testMapCounter() {
        Counter counter = new SimpleCounter();
        counter.inc(42L);

        verifyPoint(MetricMapper.map(INFO, TIMESTAMP, counter), "count=42");
    }

    @Test
    public void testMapHistogram() {
        Histogram histogram = new TestHistogram();

        verifyPoint(
                MetricMapper.map(INFO, TIMESTAMP, histogram),
                "count=3",
                "max=6",
                "mean=4.0",
                "min=7",
                "p50=0.5",
                "p75=0.75",
                "p95=0.95",
                "p98=0.98",
                "p99=0.99",
                "p999=0.999",
                "stddev=5.0");
    }

    @Test
    public void testMapMeter() {
        Meter meter = new TestMeter();

        verifyPoint(MetricMapper.map(INFO, TIMESTAMP, meter), "count=100", "rate=5.0");
    }

    private void verifyPoint(Point point, String... expectedFields) {
        // Most methods of Point are package private. We use toString() method to check that values
        // are as expected.
        // An alternative can be to call lineProtocol() method, which additionally escapes values
        // for InfluxDB format.
        assertEquals(
                "Point [name="
                        + NAME
                        + ", time="
                        + TIMESTAMP.toEpochMilli()
                        + ", tags={tag-1=42, tag-2=green}"
                        + ", precision=MILLISECONDS"
                        + ", fields={"
                        + String.join(", ", expectedFields)
                        + "}"
                        + "]",
                point.toString());
    }

    private static MeasurementInfo getMeasurementInfo(String name) {
        Map<String, String> tags = new HashMap<>();
        tags.put("tag-1", "42");
        tags.put("tag-2", "green");
        return new MeasurementInfo(name, tags);
    }

    @Test
    public void testInsertInfluxDB() throws Exception {
        List<String> appIds = com.google.common.collect.Lists.newArrayList(
                "100099733532",
                "100706958691",
                "101725348225",
                "516489121312",
                "600769507487"
        );
        List<Date> dates = geneDates(120);
        for (String appId : appIds) {
            for (Date date : dates) {
                // 对应的累加器实现 ...
                int eventCnt = 10 + (int) (Math.random() * 90);
                Histogram histogram = new DescriptiveStatisticsHistogram(eventCnt);
                for (int k = 0; k < eventCnt ; k ++) {
                    long l = (long)(Math.random() * 3000);
                    histogram.update(l);
                }

                Map<String, String> tags = new HashMap<>();
                tags.put("appId", appId);
                MeasurementInfo measurementInfo = new MeasurementInfo("log_gap", tags);
                Instant instant = date.toInstant();
                Point point = MetricMapper.map(measurementInfo, instant, histogram);

                String database = "metric";
                String retentionPolicy = null;
                InfluxDB.ConsistencyLevel consistency = InfluxDB.ConsistencyLevel.ONE;
                InfluxDB influxDB;
//        influxDB = InfluxDBFactory.connect(url, username, password, client);
                influxDB = getInfluxDB();
                influxDB.write(database, retentionPolicy, point);
            }
            System.out.println("appId: " + appId);
        }
    }

    private List<Date> geneDates(int count) throws Exception {
        List<Date> dates = Lists.newArrayList();
        Date now = new Date();
        for (int i = 0; i < count; i ++) {
            Date date = DateUtils.addMinutes(now, -i);
            dates.add(date);
        }
        return dates;
    }

    InfluxDB getInfluxDB() throws Exception {
        String database = "metric";
        String retentionPolicy = null;
        InfluxDB.ConsistencyLevel consistency = InfluxDB.ConsistencyLevel.ONE;
        InfluxDB influxDB;

        String host = "test01";
        int port = 28086;
        String url = String.format("http://%s:%d", host, port);
        String username = "";
        String password = "";

        int connectTimeout = 2000;
        int writeTimeout = 2000;

        OkHttpClient.Builder client =
                new OkHttpClient.Builder()
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
//        influxDB = InfluxDBFactory.connect(url, username, password, client);
        influxDB = InfluxDBFactory.connect(url, client);
        return influxDB;
    }

    @Test
    public void testQuery() throws Exception {
        InfluxDB influxDB = getInfluxDB();
//        Query query = new Query("SHOW MEASUREMENTS ON _internal");
//        Query query = new Query("SELECT * FROM \"_internal\"..\"database\" LIMIT 10");
//        Query query = new Query("SELECT * FROM \"_internal\"..\"database\" LIMIT 10");
        // SELECT "mean", "max" FROM "sdk-gap" WHERE time >= now() - 6h and time <= now()
        Query query = new Query("SELECT max, mean, p50 FROM \"sdk-gap\" WHERE time >= now() - 6h and time <= now()", "metric");
        QueryResult query1 = influxDB.query(query);
        if (query1.getResults() != null && CollectionUtils.isNotEmpty(query1.getResults())) {
            query1.getResults().forEach(e -> {
                if (e != null && CollectionUtils.isNotEmpty(e.getSeries())) {
                    e.getSeries().forEach(System.out::println);
                } else {
                    System.out.println("result series no data ...");
                }
            });
        } else {
            System.out.println("no data ... ");
        }
    }

    @Test
    public void testDelete() throws Exception {
        InfluxDB influxDB = getInfluxDB();
    }
}
