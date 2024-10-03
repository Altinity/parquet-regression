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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.json.JSONArray;

public class GenerateParquet {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java GenerateParquet <config-json-file> <output-path>");
            System.exit(1);
        }

        String configFilePath = args[0];
        String outputPath = args[1];
        try {
            String content = new String(Files.readAllBytes(Paths.get(configFilePath)));
            JSONObject configJson = new JSONObject(content);
            writeParquetFile(configJson, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeParquetFile(JSONObject configJson, String filePath) throws IOException {
        MessageType schema = buildSchema(configJson.getJSONArray("schema"));

        Configuration conf = new Configuration();
        GroupWriteSupport.setSchema(schema, conf);

        Path path = new Path(filePath);
        SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);

        JSONObject options = configJson.getJSONObject("options");
        String compressionCodec = options.optString("compression", "snappy").toUpperCase();
        String writerVersion = options.optString("writerVersion", "1.0");
        String rowGroupSize = options.optString("rowGroupSize", "default");
        String pageSize = options.optString("pageSize", "default");
        JSONArray encodings = options.optJSONArray("encodings");
        String bloomFilterOption = configJson.optString("bloomFilter", "none");

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

        if ("all".equalsIgnoreCase(bloomFilterOption)) {
            builder.withBloomFilterEnabled(true);
        } else if (!"none".equalsIgnoreCase(bloomFilterOption)) {
            JSONArray bloomFilterColumns = configJson.getJSONArray("bloomFilter");
            for (int i = 0; i < bloomFilterColumns.length(); i++) {
                String column = bloomFilterColumns.getString(i);
                builder.withBloomFilterEnabled(column, true);
            }
        }

        //FIXME NEED TO DETERMINE HOW TO HANDLE DIFFERENCE IN "DATA" ARRAY LENGTHS IN JSON FOR EACH COLUMN
        JSONArray schemaArray = configJson.getJSONArray("schema");
        int numRows = Integer.MAX_VALUE;
        for (int i = 0; i < schemaArray.length(); i++) {
            int currentLength = schemaArray.getJSONObject(i).getJSONArray("data").length();
            if (currentLength < numRows) {
                numRows = currentLength;
            }
        }

        try (ParquetWriter<Group> writer = builder.build()) {
            for (int i = 0; i < numRows; i++) {
                Group group = groupFactory.newGroup();
                insertData(group, schemaArray, i);
                writer.write(group);
            }
        }
    }

    private static MessageType buildSchema(JSONArray schemaArray) {
        Types.MessageTypeBuilder builder = Types.buildMessage();

        for (int i = 0; i < schemaArray.length(); i++) {
            JSONObject field = schemaArray.getJSONObject(i);
            String name = field.getString("name");
            String type = field.getString("type");

            //TODO ADD SUPPORT FO ALL REMAINING TYPES
            switch (type.toLowerCase()) {
                case "uint8":
                case "uint16":
                case "uint32":
                case "uint64":
                    int uintBitWidth = Integer.parseInt(type.substring(4));
                    builder.required(uintBitWidth == 64 ? PrimitiveType.PrimitiveTypeName.INT64 : PrimitiveType.PrimitiveTypeName.INT32)
                            .as(LogicalTypeAnnotation.intType(uintBitWidth, false))
                            .named(name);
                    break;
                case "int8":
                case "int16":
                case "int32":
                case "int64":
                    int intBitWidth = Integer.parseInt(type.substring(3));
                    builder.required(intBitWidth == 64 ? PrimitiveType.PrimitiveTypeName.INT64 : PrimitiveType.PrimitiveTypeName.INT32)
                            .as(LogicalTypeAnnotation.intType(intBitWidth))
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
    private static void insertData(Group group, JSONArray schemaArray, int rowIndex) {
        for (int i = 0; i < schemaArray.length(); i++) {
            JSONObject field = schemaArray.getJSONObject(i);
            String name = field.getString("name");
            JSONArray dataArray = field.getJSONArray("data");
            Object value = dataArray.get(rowIndex);

            //TODO ADD SUPPORT FO ALL REMAINING TYPES
            if (value instanceof Integer) {
                group.append(name, (Integer) value);
            } else if (value instanceof Long) {
                group.append(name, (Long) value);
            } else if (value instanceof String) {
                group.append(name, (String) value);
            } else if (value instanceof Boolean) {
                group.append(name, (Boolean) value);
            } else {
                throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
            }
        }
    }
}