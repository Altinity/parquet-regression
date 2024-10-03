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
import java.lang.reflect.Array;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;


public class GenerateParquet {

    private static void writeParquetFile(JSONObject configJson) throws IOException {
        String outputFilePath = configJson.getString("fileName");
        MessageType schema = buildSchema(configJson.getJSONArray("schema"));

        Configuration conf = new Configuration();
        GroupWriteSupport.setSchema(schema, conf);

        Path path = new Path(outputFilePath);
        SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);

        // Extracting configuration parameters from JSON
        String compressionCodec = configJson.optString("compression", "snappy").toUpperCase();
        String writerVersion = configJson.optString("writerVersion", "1.0");
        String rowGroupSize = configJson.optString("rowGroupSize", "default");
        String pageSize = configJson.optString("pageSize", "default");
        JSONArray encodings = configJson.optJSONArray("encodings");
        boolean bloomFilter = configJson.optBoolean("bloomFilter", false);

        // Building writer with optional parameters
        ExampleParquetWriter.Builder builder = ExampleParquetWriter.builder(HadoopOutputFile.fromPath(path, conf))
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withCompressionCodec(CompressionCodecName.valueOf(compressionCodec))
                .withRowGroupSize(rowGroupSize.equals("default") ? ParquetWriter.DEFAULT_BLOCK_SIZE : Integer.parseInt(rowGroupSize))
                .withPageSize(pageSize.equals("default") ? ParquetWriter.DEFAULT_PAGE_SIZE : Integer.parseInt(pageSize))
                .withValidation(ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED)
                .withWriterVersion(writerVersion.equals("1.0") ? ParquetProperties.WriterVersion.PARQUET_1_0 : ParquetProperties.WriterVersion.PARQUET_2_0)
                .withConf(conf);

        if (encodings != null) {
            for (int i = 0; i < encodings.length(); i++) {
                String encoding = encodings.getString(i);
                if ("DICTIONARY".equals(encoding)) {
                    builder.withDictionaryPageSize(ParquetWriter.DEFAULT_PAGE_SIZE);
                    builder.withDictionaryEncoding(true);
                } else if ("BYTE_STREAM_SPLIT".equals(encoding)) {
                    builder.withByteStreamSplitEncoding(true);
                } else {
                    throw new IllegalArgumentException("Invalid encoding type: " + encoding);
                }
            }
        }

        if (bloomFilter) {
            builder.withBloomFilterEnabled(true);
        }

        try (ParquetWriter<Group> writer = builder.build()) {
        }
    }

    private static MessageType buildSchema(JSONArray schemaArray) {
        Types.MessageTypeBuilder builder = Types.buildMessage();

        for (int i = 0; i < schemaArray.length(); i++) {
            JSONObject field = schemaArray.getJSONObject(i);
            String name = field.getString("name");
            String type = field.getString("type");

            switch (type.toLowerCase()) {
                case "uint8":
                case "uint16":
                case "uint32":
                case "uint64":
                    int UintBitWidth = Integer.parseInt(type.substring(4));
                    builder.required(PrimitiveType.PrimitiveTypeName.INT32)
                            .as(LogicalTypeAnnotation.intType(UintBitWidth, false))
                            .named(name);
                    break;
                case "int8":
                case "int16":
                case "int32":
                case "int64":
                    int intBitWidth = Integer.parseInt(type.substring(3));
                    builder.required(PrimitiveType.PrimitiveTypeName.INT32)
                            .as(LogicalTypeAnnotation.intType(intBitWidth, false))
                            .named(name);
                    break;
                case "string":
                    builder.required(PrimitiveType.PrimitiveTypeName.BINARY)
                            .as(LogicalTypeAnnotation.stringType())
                            .named(name);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }

        return builder.named("MySchema");
    }
}