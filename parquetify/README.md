<img align=right style="width:10em;" src="https://github.com/user-attachments/assets/afce991a-3f2e-4847-857d-f46f4bb60f9e">

<br><br><br><br>

# 🧩 Parquetify

**Parquetify** is a lightweight tool leveraging the [parquet-java](https://github.com/apache/parquet-java) library to generate [Apache Parquet](https://parquet.apache.org/) files based on the file definition provided in a JSON file.

<p align="center">
  <img src="https://github.com/user-attachments/assets/f63888b6-59db-46ce-9d75-c17fd88833d1">
</p>

# 🌟 Features

| Feature | Description |
|---|---|
| **Physical Data Types:** | All physical data types: `INT32`, `INT64`, `BOOLEAN`, `FLOAT`, `DOUBLE`, `BINARY`, `FIXED_LEN_BYTE_ARRAY`. |
| **Logical Data Types:** | Most logical types (except for `FLOAT16`): `UTF8`, `DECIMAL`, `DATE`, `TIME_MILLIS`, `TIME_MICROS`, `TIMESTAMP_MILLIS`, `TIMESTAMP_MICROS`, `ENUM`, `NONE`, `MAP`, `LIST`, `STRING`, `MAP_KEY_VALUE`, `TIME`, `INTEGER`, `JSON`, `BSON`, `UUID`, `INTERVAL`, `UINT_8`, `UINT_16`, `UINT_32`, `UINT_64`, `INT_8`, `INT_16`, `INT_32`, `INT_64` |
| **Precision & Scale:** | Precision and scale for `DECIMAL` types. |
| **Compression:** | `NONE`, `SNAPPY`, `GZIP`, `LZO`, `BROTLI`, `LZ4`, `ZSTD`. |
| **Encodings:** | Automatically set by the writer for a given column. |
| **Bloom Filter:** | Apply a bloom filter to specific columns or all columns (including those within groups). |
| **Writer Version:** | Specify writer version (`1.0`, `2.0`). |
| **Customizable Sizes:** | Row group and page sizes. |

---

# 📝 Table of Contents

- [Installation](#-installation) 
- [Create Parquet File](#%EF%B8%8F-create-parquet-file)
- [JSON File Definition](#-json-file-definition)
   - [Regular Types](#regular-types)
   - [Nested Types](#nested-types)
- [Missing Functionality](#-missing-functionality)

---

# 💾 [Installation](#table-of-contents)

1. Download the latest release from the [Releases](https://github.com/Altinity/parquet-regression/releases):

    ```
    sudo apt update
    wget https://github.com/Altinity/parquet-regression/releases/download/1.0.3/parquetify_1.0.3_amd64.deb
    ```
    
    💡 **Note:** Ensure that you download the package corresponding to your system architecture. Both **ARM** and **x86_64** are supported.

2. Install the `.deb` package:

    ```bash
    sudo apt install ./parquetify_1.0.3_amd64.deb
    ```

3. Confirm the installation, run the following command:

    ```bash
    parquetify
    ```
    
    If successful, you will see usage instructions like:
    
    ```bash
    Error parsing command line arguments: Missing required options: j, o
    usage: GenerateParquet
     -j,--json <arg>     Path to the JSON file
     -o,--output <arg>   Output path for the Parquet file
    ```

---    

# ⚙️ [Create Parquet File](#table-of-contents)

To generate your first Parquet file, use the provided example JSON available in our [schema-example folder](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/example.json):

```bash
parquetify -j example.json -o /path/to/output/file.parquet
```
> [!WARNING]
> Parquetify allows you to specify any structure, including incorrect ones. If the structure is invalid, the Parquet file may be generated, but it may not be readable by tools or databases.

---

# 📝 [JSON File Definition](#table-of-contents)

Parquetify uses a JSON schema to define the file structure and values that will populate your Parquet file. The schema follows a specific format, which is outlined [here](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/schema.json).

A simple JSON structure looks like:

```json
{
  "fileName": "example_output.parquet",
  "options": {
    "writerVersion": "1.0",
    "compression": "UNCOMPRESSED",
    "rowGroupSize": "default",
    "pageSize": "default",
    "encodings": ["PLAIN"],
    "bloomFilter": "all"
  },
  "schema": [
    {
      "name": "id",
      "schemaType": "required",
      "physicalType": "INT32",
      "logicalType": "INT8",
      "data": [1, 2, 3, 4, 5]
    },
    {
      "name": "person",
      "schemaType": "repeatedGroup",
      "fields": [
        {
          "name": "name",
          "schemaType": "optional",
          "physicalType": "BINARY",
          "logicalType": "STRING"
        },
        {
          "name": "age",
          "schemaType": "required",
          "physicalType": "INT32"
        }
      ],
      "data": [
        {
          "name": "Alice",
          "age": 30
        }
        ]
    }
  ]
}
```

## [Regular Types](#table-of-contents)

A typical example for handling a simple column type (`INT32`) looks like this:

```json
{
  "name": "id",
  "schemaType": "required",
  "physicalType": "INT32",
  "logicalType": "INT8",
  "data": [1, 2, 3, 4, 5]
}
```

- `name`: Column name.
- `schemaType`: Specifies whether the column allows null values (`required` means no null values).
- `physicalType`: Defines the physical data type.
- `logicalType`: Defines the logical type for better data interpretation.
- `data`: An array of values to populate the column.

## [Nested Types](#table-of-contents)

You can define nested types as follows:

```json
{
  "name": "person",
  "schemaType": "repeatedGroup",
  "fields": [
    {
      "name": "name",
      "schemaType": "optional",
      "physicalType": "BINARY",
      "logicalType": "STRING"
    },
    {
      "name": "age",
      "schemaType": "required",
      "physicalType": "INT32"
    }
  ],
  "data": [
    {
      "name": "Alice",
      "age": 30
    },
    {
      "name": "Bob",
      "age": 25
    }
  ]
}
```

- `repeatedGroup`: Defines an array of objects.
- `requiredGroup` and `optionalGroup`: Define tuple-like structures.

---

# 🚧 [Missing Functionality](#table-of-contents)

- Additional encodings (`DELTA_BYTE_ARRAY`, `DELTA_LENGTH_BYTE_ARRAY`, `RLE`, etc.)
- Data insertion into `FLOAT16` columns (planned for next release)
- Parquet encryption
- Compression algorithms (`GZIP`, `LZO`, `BROTLI`, `LZ4`)

