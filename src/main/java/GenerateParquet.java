import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    try {
      MessageType schema = buildSchema(configJson.getJSONArray("schema"));
      Configuration conf = new Configuration();
      GroupWriteSupport.setSchema(schema, conf);
      SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);
      ParquetWriter<Group> writer = createParquetWriter(filePath, conf, configJson.getJSONObject("options"));
      writeData(writer, groupFactory, configJson.getJSONArray("schema"));
    } catch (IOException e) {
      System.err.println("Error generating Parquet file: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static ParquetWriter<Group> createParquetWriter(String filePath, Configuration conf, JSONObject options) throws IOException {
    Path path = new Path(filePath);
    String compressionCodec = options.optString("compression", "SNAPPY").toUpperCase();
    String writerVersion = options.optString("writerVersion", "1.0");
    String rowGroupSize = options.optString("rowGroupSize", "default");
    String pageSize = options.optString("pageSize", "default");
    JSONArray encodings = options.optJSONArray("encodings");
    String bloomFilterOption = options.optString("bloomFilter", "none");

    ExampleParquetWriter.Builder builder = ExampleParquetWriter.builder(HadoopOutputFile.fromPath(path, conf))
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
            .withCompressionCodec(CompressionCodecName.valueOf(compressionCodec))
            .withRowGroupSize(rowGroupSize.equals("default") ? ParquetWriter.DEFAULT_BLOCK_SIZE : Integer.parseInt(rowGroupSize))
            .withPageSize(pageSize.equals("default") ? ParquetWriter.DEFAULT_PAGE_SIZE : Integer.parseInt(pageSize))
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
      String type = field.getString("type");
      addFieldToSchema(builder, name, type);
    }
    return builder.named("MySchema");
  }

  private static void addFieldToSchema(Types.MessageTypeBuilder builder, String name, String type) {
    switch (type.toLowerCase()) {
      case "uint8":
      case "uint16":
      case "uint32":
      case "uint64":
        int uintBitWidth = Integer.parseInt(type.substring(4));
        builder.required(uintBitWidth == 64 ? PrimitiveType.PrimitiveTypeName.INT64 : PrimitiveType.PrimitiveTypeName.INT32)
                .as(LogicalTypeAnnotation.intType(uintBitWidth, false)).named(name);
        break;
      case "int8":
      case "int16":
      case "int32":
      case "int64":
        int intBitWidth = Integer.parseInt(type.substring(3));
        builder.required(intBitWidth == 64 ? PrimitiveType.PrimitiveTypeName.INT64 : PrimitiveType.PrimitiveTypeName.INT32)
                .as(LogicalTypeAnnotation.intType(intBitWidth)).named(name);
        break;
      case "string":
        builder.required(PrimitiveType.PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named(name);
        break;
      default:
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
  }

  private static void writeData(ParquetWriter<Group> writer, SimpleGroupFactory groupFactory, JSONArray schemaArray) throws IOException {
    int numRows = calculateNumRows(schemaArray);
    for (int i = 0; i < numRows; i++) {
      Group group = groupFactory.newGroup();
      insertDataIntoGroup(group, schemaArray, i);
      writer.write(group);
    }
  }

  private static int calculateNumRows(JSONArray schemaArray) {
    int numRows = Integer.MAX_VALUE;
    for (int i = 0; i < schemaArray.length(); i++) {
      int currentLength = schemaArray.getJSONObject(i).getJSONArray("data").length();
      if (currentLength < numRows) {
        numRows = currentLength;
      }
    }
    return numRows;
  }

  private static void insertDataIntoGroup(Group group, JSONArray schemaArray, int rowIndex) {
    for (int i = 0; i < schemaArray.length(); i++) {
      JSONObject field = schemaArray.getJSONObject(i);
      String name = field.getString("name");
      JSONArray dataArray = field.getJSONArray("data");
      Object value = dataArray.get(rowIndex);
      appendValueToGroup(group, name, value);
    }
  }

  private static void appendValueToGroup(Group group, String name, Object value) {
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