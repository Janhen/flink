package org.apache.flink.table.utils;

import org.apache.flink.table.runtime.util.JsonUtils;

import org.junit.Test;

/**
 * @author zhangjiang
 */
public class JsonTest {

    final String track_new_json = "{\"logId\":\"1acac5097b223e7ea36bf6a7bd05cc3d\",\"logSessionId\":1627555005508,\"logRn\":0,\"logFlushTime\":1627555005649,\"orionVer\":\"1.1.0\",\"debug\":1,\"eventName\":\"#appStart\",\"eventTime\":1627555005513,\"serverTime\":1627555005626,\"event\":{\"openTime\":1627555005512},\"cp\":{\"#cpUserId\":-500,\"#appId\":\"516489121312\",\"#appVer\":\"1.0.0\",\"#cpChannelId\":1},\"publish\":{\"channelId\":1,\"subChannelId\":0,\"sdkVer\":\"1.0.0\",\"authUserId\":\"-500\",\"platformUserId\":-500},\"device\":{\"platform\":1,\"os\":\"Android\",\"osVer\":\"6.0.1\",\"brand\":\"Android\",\"board\":\"unknown\",\"model\":\"MuMu\",\"hardware\":\"cancro\",\"manufacturer\":\"Netease\",\"abis\":\"x86,armeabi-v7a,armeabi\",\"product\":\"cancro\",\"display\":\"V417IR release-keys\",\"fingerprint\":\"OnePlus/OnePlus2/OnePlus2:6.0.1/MMB29M/1447841200:user/release-keys\",\"device\":\"x86\",\"imei\":\"540000000196516\",\"imei2\":\"540000000196516\",\"oaidSupport\":0,\"oaid\":\"\",\"serial\":\"ZX1G42CPJD\",\"mac\":\"08:00:27:e2:d0:ac\",\"androidId\":\"bb905fb0be6c5dad\",\"imsi\":\"\",\"scrWidth\":2000,\"scrHeight\":1125,\"scrNotch\":0,\"orientation\":2,\"wifi\":1,\"cellId\":0,\"mcc\":\"\",\"mnc\":\"\",\"lac\":\"\",\"signal\":4,\"carrier\":\"\",\"networkType\":0,\"ip\":\"100.122.52.148\",\"timezone\":28800,\"locAllow\":1,\"latitude\":39.90719,\"longitude\":116.391075,\"language\":\"zh_CN\",\"battery\":0,\"availRam\":2200350720,\"totalRam\":3182264320,\"availRom\":133592690688,\"totalRom\":135148310528,\"gyro\":\"0.1,9.7,0.1\"}}";

//    @Test
//    public void testJsonParse() throws Exception {
//        JsonUtils.getInstance().getJsonObject()
//    }

    @Test
    public void testJson() throws Exception {
        JsonUtils TrackUtil = JsonUtils.getInstance();
        System.out.println("logId: " + TrackUtil.getJsonObject(track_new_json, "logId"));
        System.out.println("event.openTime: " + TrackUtil.getJsonObject(track_new_json, "event.openTime"));
        Object jsonObject1 = TrackUtil.getJsonObject(track_new_json, "event.openTime");
        System.out.println("jsonObject1: " + jsonObject1);

        String jsonString = TrackUtil.getJsonObject(track_new_json, "device.os");
        System.out.println("jsonString: " + jsonString);

        String typeVal1 = TrackUtil.getJsonObject(track_new_json, "event.openTime");
        System.out.println("type1: " + typeVal1);
    }
}
