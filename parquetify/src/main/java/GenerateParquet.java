import org.apache.commons.cli.*;
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
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Types;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateParquet {

    public static void main(String[] args) {
        CommandLine cmd = parseCommandLineArguments(args);
        if (cmd == null) return;

        try {
            String content = new String(Files.readAllBytes(Paths.get(cmd.getOptionValue("json"))));
            JSONObject configJson = new JSONObject(content);
            String fileName = configJson.getString("fileName");
            String outputPath = cmd.getOptionValue("output");
            generateParquet(configJson, outputPath + "/" + fileName);
        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static CommandLine parseCommandLineArguments(String[] args) {
        Options options = new Options();

        Option configFilePath = new Option("j", "json", true, "Path to the JSON file");
        configFilePath.setRequired(true);
        options.addOption(configFilePath);

        Option outputPath = new Option("o", "output", true, "Output path for the Parquet file");
        outputPath.setRequired(true);
        options.addOption(outputPath);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("GenerateParquet", options);
            return null;
        }
    }

    private static void generateParquet(JSONObject configJson, String filePath) {
        ParquetWriter<Group> writer = null;
        try {
            MessageType schema = buildSchema(configJson.getJSONArray("schema"));
            Configuration conf = new Configuration();
            GroupWriteSupport.setSchema(schema, conf);
            SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);
            writer = createParquetWriter(filePath, conf, configJson.getJSONObject("options"));
            writeData(writer, groupFactory, configJson.getJSONArray("schema"));
        } catch (IOException e) {
            System.err.println("Error generating Parquet file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing Parquet writer: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static ParquetWriter<Group> createParquetWriter(String filePath, Configuration conf, JSONObject options) throws IOException {
        Path path = new Path(filePath);
        String compressionCodec = options.optString("compression", "SNAPPY").toUpperCase();
        String writerVersion = options.optString("writerVersion", "1.0");
        int rowGroupSize = options.optInt("rowGroupSize", 128 * 1024 * 1024);
        int pageSize = options.optInt("pageSize", 1024 * 1024);
        JSONArray encodings = options.optJSONArray("encodings");
        String bloomFilterOption = options.optString("bloomFilter", "none");

        ExampleParquetWriter.Builder builder = ExampleParquetWriter.builder(HadoopOutputFile.fromPath(path, conf))
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withCompressionCodec(CompressionCodecName.valueOf(compressionCodec))
                .withRowGroupSize(rowGroupSize)
                .withPageSize(pageSize)
                .withValidation(ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED)
                .withWriterVersion(writerVersion.equals("1.0") ? ParquetProperties.WriterVersion.PARQUET_1_0 : ParquetProperties.WriterVersion.PARQUET_2_0)
                .withConf(conf);

        configureEncodings(builder, encodings);
        configureBloomFilters(builder, bloomFilterOption, options);

        return builder.build();
    }

    private static void configureEncodings(ExampleParquetWriter.Builder builder, JSONArray encodings) {
        if (encodings == null) return;

        for (int i = 0; i < encodings.length(); i++) {
            String encoding = encodings.getString(i);
            switch (encoding.toUpperCase()) {
                case "DICTIONARY":
                    builder.withDictionaryPageSize(ParquetWriter.DEFAULT_PAGE_SIZE);
                    builder.withDictionaryEncoding(true);
                    break;
                case "BYTE_STREAM_SPLIT":
                    builder.withByteStreamSplitEncoding(true);
                    break;
                case "PLAIN":
                    builder.withDictionaryEncoding(false);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid encoding type: " + encoding);
            }
        }
    }

    private static void configureBloomFilters(ExampleParquetWriter.Builder builder, String bloomFilterOption, JSONObject options) {
        if ("all".equalsIgnoreCase(bloomFilterOption)) {
            builder.withBloomFilterEnabled(true);
        } else if (!"none".equalsIgnoreCase(bloomFilterOption)) {
            JSONArray bloomFilterColumns = options.optJSONArray("bloomFilter");
            if (bloomFilterColumns != null) {
                for (int i = 0; i < bloomFilterColumns.length(); i++) {
                    builder.withBloomFilterEnabled(bloomFilterColumns.getString(i), true);
                }
            }
        }
    }

    private static MessageType buildSchema(JSONArray schemaArray) {
        Types.MessageTypeBuilder builder = Types.buildMessage();
        for (int i = 0; i < schemaArray.length(); i++) {
            JSONObject field = schemaArray.getJSONObject(i);
            String name = field.getString("name");
            String schemaType = field.getString("schemaType");
            String physicalType = field.optString("physicalType", null);
            String logicalType = field.optString("logicalType", "NONE");
            int length = field.optInt("length", 0);

            Types.Builder<?, ?> columnBuilder;
            if (physicalType == null) {
                columnBuilder = addGroupSchemaType(builder, schemaType, field.getJSONArray("fields"));
            } else {
                columnBuilder = addSchemaType(builder, schemaType, physicalType, length);
                addLogicalType(columnBuilder, logicalType, physicalType, length, field);
            }

            columnBuilder.named(name);
        }
        return builder.named("MySchema");
    }

    private static Types.Builder<?, ?> addGroupSchemaType(Types.GroupBuilder<?> builder, String schemaType, JSONArray fields) {
        Types.GroupBuilder<?> groupBuilder;
        switch (schemaType) {
            case "optionalGroup":
                groupBuilder = builder.optionalGroup();
                break;
            case "requiredGroup":
                groupBuilder = builder.requiredGroup();
                break;
            case "repeatedGroup":
                groupBuilder = builder.repeatedGroup();
                break;
            default:
                throw new IllegalArgumentException("Unsupported schema type for group: " + schemaType);
        }

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String name = field.getString("name");
            String fieldSchemaType = field.getString("schemaType");
            String fieldPhysicalType = field.optString("physicalType", null);
            String fieldLogicalType = field.optString("logicalType", "NONE");
            int length = field.optInt("length", 0);

            Types.Builder<?, ?> fieldBuilder;
            if (fieldPhysicalType == null) {
                fieldBuilder = addGroupSchemaType(groupBuilder, fieldSchemaType, field.getJSONArray("fields"));
            } else {
                fieldBuilder = addSchemaType(groupBuilder, fieldSchemaType, fieldPhysicalType, length);
                addLogicalType(fieldBuilder, fieldLogicalType, fieldPhysicalType, length, field);
            }

            fieldBuilder.named(name);
        }
        return groupBuilder;
    }

    private static Types.Builder<?, ?> addSchemaType(Types.GroupBuilder<?> builder, String schemaType, String physicalType, int length) {
        PrimitiveType.PrimitiveTypeName primitiveType = PrimitiveType.PrimitiveTypeName.valueOf(physicalType);

        switch (primitiveType) {
            case FIXED_LEN_BYTE_ARRAY:
                if (length <= 0) {
                    throw new IllegalArgumentException("Invalid FIXED_LEN_BYTE_ARRAY length: " + length);
                }
                switch (schemaType) {
                    case "optional":
                        return builder.optional(primitiveType).length(length);
                    case "required":
                        return builder.required(primitiveType).length(length);
                    case "repeated":
                        return builder.repeated(primitiveType).length(length);
                    default:
                        throw new IllegalArgumentException("Unsupported schema type: " + schemaType);
                }
            default:
                switch (schemaType) {
                    case "optional":
                        return builder.optional(primitiveType);
                    case "required":
                        return builder.required(primitiveType);
                    case "repeated":
                        return builder.repeated(primitiveType);
                    default:
                        throw new IllegalArgumentException("Unsupported schema type: " + schemaType);
                }
        }
    }

    private static void addLogicalType(Types.Builder<?, ?> columnBuilder, String logicalType, String physicalType, int length, JSONObject field) {
        switch (logicalType.toUpperCase()) {
            case "MAP":
                columnBuilder.as(LogicalTypeAnnotation.mapType());
                break;
            case "LIST":
                columnBuilder.as(LogicalTypeAnnotation.listType());
                break;
            case "STRING":
            case "UTF8":
                columnBuilder.as(LogicalTypeAnnotation.stringType());
                break;
            case "MAP_KEY_VALUE":
                columnBuilder.as(LogicalTypeAnnotation.mapType());
                break;
            case "ENUM":
                columnBuilder.as(LogicalTypeAnnotation.enumType());
                break;
            case "DECIMAL":
                int precision = field.optInt("precision", 10);
                int scale = field.optInt("scale", 2);

                columnBuilder.as(LogicalTypeAnnotation.decimalType(scale, precision));
                break;
            case "DATE":
                columnBuilder.as(LogicalTypeAnnotation.dateType());
                break;
            case "TIME":
            case "TIME_MILLIS":
                columnBuilder.as(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS));
                break;
            case "TIME_MICROS":
                columnBuilder.as(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MICROS));
                break;
            case "TIMESTAMP":
            case "TIMESTAMP_MILLIS":
                columnBuilder.as(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS));
                break;
            case "TIMESTAMP_MICROS":
                columnBuilder.as(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MICROS));
                break;
            case "INT8":
                columnBuilder.as(LogicalTypeAnnotation.intType(8, true));
                break;
            case "INT16":
                columnBuilder.as(LogicalTypeAnnotation.intType(16, true));
                break;
            case "INT32":
                columnBuilder.as(LogicalTypeAnnotation.intType(32, true));
                break;
            case "INT64":
                columnBuilder.as(LogicalTypeAnnotation.intType(64, true));
                break;
            case "UINT8":
                columnBuilder.as(LogicalTypeAnnotation.intType(8, false));
                break;
            case "UINT16":
                columnBuilder.as(LogicalTypeAnnotation.intType(16, false));
                break;
            case "UINT32":
                columnBuilder.as(LogicalTypeAnnotation.intType(32, false));
                break;
            case "UINT64":
                columnBuilder.as(LogicalTypeAnnotation.intType(64, false));
                break;
            case "BSON":
                columnBuilder.as(LogicalTypeAnnotation.bsonType());
                break;
            case "UUID":
                columnBuilder.as(LogicalTypeAnnotation.uuidType());
                break;
            case "INTERVAL":
                columnBuilder.as(LogicalTypeAnnotation.intervalType());
                break;
            case "FLOAT16":
                if (!physicalType.equals("FIXED_LEN_BYTE_ARRAY") || length != 2) {
                    throw new IllegalArgumentException("FLOAT16 can only annotate FIXED_LEN_BYTE_ARRAY with length 2");
                }
                columnBuilder.as(LogicalTypeAnnotation.float16Type());
                break;
            case "JSON":
                columnBuilder.as(LogicalTypeAnnotation.jsonType());
                break;
            case "NONE":
                // No logical type
                break;
            default:
                throw new IllegalArgumentException("Unsupported logical type: " + logicalType);
        }
    }

    private static void writeData(ParquetWriter<Group> writer, SimpleGroupFactory groupFactory, JSONArray schemaArray) throws IOException {
        int numRows = calculateNumRows(schemaArray);
        System.out.println("Number of rows to write: " + numRows); // Logging number of rows to write
        for (int i = 0; i < numRows; i++) {
            Group group = groupFactory.newGroup();
            insertDataIntoGroup(group, schemaArray, i);
            System.out.println("Writing row " + (i + 1)); // Logging each row being written
            writer.write(group);
        }
    }

    private static int calculateNumRows(JSONArray schemaArray) {
        int numRows = Integer.MAX_VALUE;
        for (int i = 0; i < schemaArray.length(); i++) {
            int currentLength = schemaArray.getJSONObject(i).optJSONArray("data") != null ? schemaArray.getJSONObject(i).getJSONArray("data").length() : 0;
            if (currentLength > 0 && currentLength < numRows) {
                numRows = currentLength;
            }
        }
        return numRows;
    }

    private static void insertDataIntoGroup(Group group, JSONArray schemaArray, int rowIndex) {
        for (int i = 0; i < schemaArray.length(); i++) {
            JSONObject field = schemaArray.getJSONObject(i);
            if (!field.has("data")) {
                // Handle nested groups
                String name = field.getString("name");
                Group nestedGroup = group.addGroup(name);
                JSONArray groupDataArray = field.getJSONArray("data");
                JSONObject groupData = groupDataArray.getJSONObject(rowIndex);
                JSONArray fields = field.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject nestedField = fields.getJSONObject(j);
                    String nestedFieldName = nestedField.getString("name");
                    Object nestedValue = groupData.get(nestedFieldName);
                    appendValueToGroup(nestedGroup, nestedFieldName, nestedValue);
                }
            } else {
                String name = field.getString("name");
                JSONArray dataArray = field.getJSONArray("data");
                Object value = dataArray.get(rowIndex);
                appendValueToGroup(group, name, value);
            }
        }
    }

    private static void appendValueToGroup(Group group, String name, Object value) {
        try {
            if (value instanceof Integer) {
                group.add(name, (Integer) value);
            } else if (value instanceof Long) {
                group.add(name, (Long) value);
            } else if (value instanceof String) {
                // Handle UUID string by converting to 16-byte array if the field is UUID
                if (isUUID(name)) {
                    byte[] uuidBytes = hexStringToByteArray((String) value);
                    group.add(name, org.apache.parquet.io.api.Binary.fromConstantByteArray(uuidBytes));
                } else {
                    group.add(name, (String) value);
                }
            } else if (value instanceof Boolean) {
                group.add(name, (Boolean) value);
            } else if (value instanceof Double) {
                group.add(name, (Double) value);
            } else if (value instanceof Float) {
                group.add(name, (Float) value);
            } else if (value instanceof BigDecimal) {
                group.add(name, ((BigDecimal) value).doubleValue());
            } else if (value instanceof byte[]) {
                group.add(name, org.apache.parquet.io.api.Binary.fromConstantByteArray((byte[]) value));
            } else if (value instanceof JSONObject) {
                Group nestedGroup = group.addGroup(name);
                JSONObject jsonObject = (JSONObject) value;
                for (String key : jsonObject.keySet()) {
                    Object nestedValue = jsonObject.get(key);
                    appendValueToGroup(nestedGroup, key, nestedValue);
                }
            } else {
                throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Error adding value to group. Value type mismatch for column: " + name, e);
        }
    }

    private static boolean isUUID(String fieldName) {
        // We add logic to identify if a particular field is a UUID
        // For simplicity, let's assume field names ending with '_uuid' indicate UUID
        return fieldName.toLowerCase().contains("uuid");
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
