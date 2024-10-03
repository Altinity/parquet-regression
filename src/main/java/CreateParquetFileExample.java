import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.schema.*;

import java.io.IOException;

public class CreateParquetFileExample {

    public static void main(String[] args){
        String outputFilePath = "/home/david/parquet/parquet-try-3/example_integer.parquet";
        try {
            writeParquetFileWithAllIntegers(outputFilePath, true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeParquetFile(String outputFilePath, MessageType schema, DataInserter dataInserter, Boolean bloomFilter) throws IOException {
        Configuration conf = new Configuration();
        GroupWriteSupport.setSchema(schema, conf);
        Path path = new Path(outputFilePath);
        SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);

        try (ParquetWriter<Group> writer = ExampleParquetWriter.builder(HadoopOutputFile.fromPath(path, conf))
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
                .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
                .withDictionaryPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
                .withDictionaryEncoding(ParquetProperties.DEFAULT_IS_DICTIONARY_ENABLED)
                .withValidation(ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED)
                .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_1_0)
                .withConf(conf)
                .withBloomFilterEnabled("int8_logical", bloomFilter)
                .build()) {
        }
    }

    public static void writeParquetFileWithAllIntegers(String outputFilePath, Boolean bloomFilter) throws IOException {
        MessageType schema = Types.buildMessage()
                .required(PrimitiveType.PrimitiveTypeName.INT32).as(LogicalTypeAnnotation.intType(8)).named("int8_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT32).as(LogicalTypeAnnotation.intType(16)).named("int16_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT32).as(LogicalTypeAnnotation.intType(32)).named("int32_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT64).as(LogicalTypeAnnotation.intType(64)).named("int64_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT32).as(LogicalTypeAnnotation.intType(16, false)).named("uint16_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT32).as(LogicalTypeAnnotation.intType(32, false)).named("uint32_logical")
                .required(PrimitiveType.PrimitiveTypeName.INT64).as(LogicalTypeAnnotation.intType(64, false)).named("uint64_logical")
                .named("MySchema");

        writeParquetFile(outputFilePath, schema, (group, i) -> {
            group.append("int8_logical", i)
                    .append("int16_logical", i * 2)
                    .append("int32_logical", i * 3)
                    .append("int64_logical", i * 4L)
                    .append("uint16_logical", i * 5)
                    .append("uint32_logical", i * 6)
                    .append("uint64_logical", i * 7L);
        }, bloomFilter);
    }

    public static void writeParquetFileWithBoolean(String outputFilePath, Boolean bloomFilter) throws IOException {
        MessageType schema = Types.buildMessage()
                .required(PrimitiveType.PrimitiveTypeName.BOOLEAN).named("some_boolean")
                .named("MySchema");

        writeParquetFile(outputFilePath, schema, (group, i) -> {
            group.append("some_boolean", i % 2 == 0);
        }, bloomFilter);
    }

    public static void writeParquetFileWithString(String outputFilePath, Boolean bloomFilter) throws IOException {
        MessageType schema = Types.buildMessage()
                .required(PrimitiveType.PrimitiveTypeName.BINARY).as(OriginalType.UTF8).named("byte_array")
                .named("MySchema");

        writeParquetFile(outputFilePath, schema, (group, i) -> {
            group.append("byte_array", "string" + i);
        }, bloomFilter);
    }

    @FunctionalInterface
    private interface DataInserter {
        void insertData(Group group, int i);
    }
}