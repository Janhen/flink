package org.apache.flink.formats.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import org.junit.Test;

import java.io.IOException;

/**
 * @author janhen
 */
public class JTest {

    /**
     * Read the parquet schema from a parquet File
     */
    private MessageType readSchemaFromDataFile(Path parquetFilePath) throws IOException {
        // LOG.info("Reading schema from " + parquetFilePath);

        Configuration conf = new Configuration();
//        if (!fs.exists(parquetFilePath)) {
//            throw new IllegalArgumentException(
//                    "Failed to read schema from data file " + parquetFilePath + ". File does not exist.");
//        }
        ParquetMetadata fileFooter = ParquetFileReader.readFooter(conf, parquetFilePath,
                ParquetMetadataConverter.NO_FILTER);
        return fileFooter.getFileMetaData().getSchema();
    }

//    @Test
//    public void testSchema() throws Exception {
//        ParquetMetadata parquetMetadata = MetadataReader.readFooter(inputStream, path, fileSize);
//        FileMetaData fileMetaData = parquetMetadata.getFileMetaData();
//        MessageType fileSchema = fileMetaData.getSchema();
//    }

    @Test
    public void testSchema2() throws Exception {
        MessageType messageType = readSchemaFromDataFile(new Path(
                "/Users/apple/Documents/_sany/_env/_data/sec/230815/017#_2023-07-31_2023-08-01.parquet"));
        System.out.println(messageType);
    }
}
