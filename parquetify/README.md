# Parquetify: Generate Parquet Files from JSON

Parquetify is a lightweight tool leveraging the [parquet-java](https://github.com/apache/parquet-java) library to generate Parquet files based on the structure provided in a JSON file.

## Features

- **Physical Data Types:** Customize the physical data types for your Parquet files.
- **Logical Data Types:** Support for a wide range of logical types.
- **Precision & Scale:** Define precision and scale for `DECIMAL` types.
- **Compression:** Choose from `SNAPPY`, `ZSTD`, or `UNCOMPRESSED`.
- **Encodings:** Includes `DICTIONARY`, `BYTE_STREAM_SPLIT`, and `PLAIN`.
- **Bloom Filter:** Apply a bloom filter to specific columns or all columns (including those within groups).
- **Writer Version:** Specify writer version (`1.0`, `2.0`).
- **Customizable Sizes:** Set specific row group and page sizes.

## Table of Contents

- [Quick Start](#quick-start)
    - [Installation](#installation) 
    - [Creating Your First Parquet File](#creating-your-first-parquet-file)
    - [Building JSON for Parquetify](#building-json-for-parquetify)
        - [Handling Regular Types](#handling-regular-types)
        - [Handling Nested Types](#handling-nested-types)
- [Missing Functionality](#missing-functionality)

## [Quick Start](#table-of-contents)

To get started with Parquetify, download the latest release from the [Releases page](https://github.com/Altinity/parquet-regression/releases):

### [Installation](#table-of-contents)

```bash
sudo apt update
wget https://github.com/Altinity/parquet-regression/releases/download/1.0.3/parquetify_1.0.3_amd64.deb
```

> [!NOTE] 
> Ensure that you download the package corresponding to your system architecture. Both ARM and x86_64 are supported.

Install the `.deb` package:

```bash
sudo apt install ./parquetify_1.0.3_amd64.deb
```

To confirm the installation, run the following command:

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

### [Creating Your First Parquet File](#table-of-contents)

To generate your first Parquet file, use the provided example JSON available in our [schema-example folder](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/example.json):

```bash
parquetify -j example.json -o /path/to/output/file.parquet
```

> [!WARNING]
> Parquetify allows you to specify any structure, including incorrect ones. If the structure is invalid, the Parquet file may be generated, but it may not be readable by tools or databases.

### [Building JSON for Parquetify](#table-of-contents)

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
      ]
    }
  ]
}
```

### [Handling Regular Types](#table-of-contents)

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

### [Handling Nested Types](#table-of-contents)

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

## [Missing Functionality](#table-of-contents)

- Additional encodings (`DELTA_BYTE_ARRAY`, `DELTA_LENGTH_BYTE_ARRAY`, `RLE`, etc.)
- Data insertion into `FLOAT16` columns (planned for next release)
- Parquet encryption
- Compression algorithms (`GZIP`, `LZO`, `BROTLI`, `LZ4`)

