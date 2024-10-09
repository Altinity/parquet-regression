import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.format.LogicalType;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.schema.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenerateParquet {

  public static void main(String[] args) {
    CommandLine cmd = parseCommandLineArguments(args);
    if (cmd == null) return;

    try {
      String content = new String(Files.readAllBytes(Paths.get(cmd.getOptionValue("json"))));
      JSONObject configJson = new JSONObject(content);
      String outputPath = cmd.getOptionValue("output");
      generateParquet(configJson, outputPath);
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

      Types.Builder<?, ?> columnBuilder;
      if (physicalType == null) {
        columnBuilder = addGroupSchemaType(builder, schemaType, field.getJSONArray("fields"));
      } else {
        columnBuilder = addSchemaType(builder, schemaType, physicalType);
        addLogicalType(columnBuilder, logicalType);
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

      Types.Builder<?, ?> fieldBuilder;
      if (fieldPhysicalType == null) {
        fieldBuilder = addGroupSchemaType(groupBuilder, fieldSchemaType, field.getJSONArray("fields"));
      } else {
        fieldBuilder = addSchemaType(groupBuilder, fieldSchemaType, fieldPhysicalType);
        addLogicalType(fieldBuilder, fieldLogicalType);
      }

      fieldBuilder.named(name);
    }
    return groupBuilder;
  }

  private static Types.Builder<?, ?> addSchemaType(Types.GroupBuilder<?> builder, String schemaType, String physicalType) {
    PrimitiveType.PrimitiveTypeName primitiveType = PrimitiveType.PrimitiveTypeName.valueOf(physicalType);

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

  private static void addLogicalType(Types.Builder<?, ?> columnBuilder, String logicalType) {
    switch (logicalType.toUpperCase()) {
      case "UTF8":
      case "STRING":
        columnBuilder.as(LogicalTypeAnnotation.stringType());
        break;
      case "DECIMAL":
        // You can adjust precision and scale as per requirements.
        columnBuilder.as(LogicalTypeAnnotation.decimalType(10, 2));
        break;
      case "DATE":
        columnBuilder.as(LogicalTypeAnnotation.dateType());
        break;
      case "TIME_MILLIS":
        columnBuilder.as(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS));
        break;
      case "TIME_MICROS":
        columnBuilder.as(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MICROS));
        break;
      case "TIMESTAMP_MILLIS":
        columnBuilder.as(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS));
        break;
      case "TIMESTAMP_MICROS":
        columnBuilder.as(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MICROS));
        break;
      case "UUID":
        columnBuilder.as(LogicalTypeAnnotation.uuidType());
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
        group.add(name, (String) value);
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
}
