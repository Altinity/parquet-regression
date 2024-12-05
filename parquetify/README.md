<img align=right style="width: 5em;" src="https://github.com/user-attachments/assets/1e97270f-7925-4cc2-8791-8d0cc77fe512">

[![License](http://img.shields.io/:license-apache%202.0-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Release](https://github.com/Altinity/parquet-regression/actions/workflows/release.yml/badge.svg)](https://github.com/Altinity/parquet-regression/actions/workflows/release.yml)

# 🧩 Parquetify

**Parquetify** is a lightweight tool leveraging the [parquet-java](https://github.com/apache/parquet-java) library to generate [Apache Parquet](https://parquet.apache.org/) files based on the file definition provided in a JSON file.

<p align="center">
  <img src="https://github.com/user-attachments/assets/f63888b6-59db-46ce-9d75-c17fd88833d1">
</p>

# 🌟 Features

| Feature                  | Description                                                                                                                                                                                                                                                                                                                                   |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Physical Data Types:** | All physical data types: `INT32`, `INT64`, `BOOLEAN`, `FLOAT`, `DOUBLE`, `BINARY`, `FIXED_LEN_BYTE_ARRAY`.                                                                                                                                                                                                                                    |
| **Logical Data Types:**  | Most logical types (except for `FLOAT16`): `UTF8`, `DECIMAL`, `DATE`, `TIME_MILLIS`, `TIME_MICROS`, `TIMESTAMP_MILLIS`, `TIMESTAMP_MICROS`, `ENUM`, `NONE`, `MAP`, `LIST`, `STRING`, `MAP_KEY_VALUE`, `TIME`, `INTEGER`, `JSON`, `BSON`, `UUID`, `INTERVAL`, `UINT_8`, `UINT_16`, `UINT_32`, `UINT_64`, `INT_8`, `INT_16`, `INT_32`, `INT_64` |
| **Precision & Scale:**   | Precision and scale for `DECIMAL` types.                                                                                                                                                                                                                                                                                                      |
| **Compression:**         | `NONE`, `SNAPPY`, `GZIP`, `LZO`, `BROTLI`, `LZ4`, `ZSTD`.                                                                                                                                                                                                                                                                                     |
| **Encodings:**           | Automatically set by the writer for a given column.                                                                                                                                                                                                                                                                                           |
| **Bloom Filter:**        | Apply a bloom filter to specific columns or all columns (including those within groups).                                                                                                                                                                                                                                                      |
| **Writer Version:**      | Specify writer version (`1.0`, `2.0`).                                                                                                                                                                                                                                                                                                        |
| **Customizable Sizes:**  | Row group and page sizes.                                                                                                                                                                                                                                                                                                                     |

---

# 📝 Table of Contents

- 📚 [Full documentation](#-full-documentation)
- 💾 [Installation](#-installation) 
- 🚀 [Creating Parquet File](#-creating-parquet-file)
- 📘 [Parquet File Schema Documentation](#-parquet-file-schema-documentation)
  - 📖 [Overview](#overview)
  - 📋 [Fields](#fields)
    - 📁 [File Definition](#1-file-definition)
    - ✍️ [Writer Options](#2-writer-options)
    - 📐 [Schema Definition](#3-schema-definition)
- 🚧 [Missing Functionality](#-missing-functionality)


---

# 📚 [Full documentation](#-table-of-contents)

🔍 See [wiki](https://github.com/Altinity/parquet-regression/wiki) for the full documentation. 

# 💾 [Installation](#-table-of-contents)

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

# 🚀 [Creating Parquet File](#-table-of-contents)

To generate your first Parquet file, use the provided example JSON available in our [schema-example folder](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/example.json):

```bash
parquetify -j example.json -o /path/to/output/file.parquet
```
> [!WARNING]
> Parquetify allows you to specify any structure, including incorrect ones. If the structure is invalid, the Parquet file may be generated, but it may not be readable by tools or databases.

---

# 📐 [Parquet File Schema Documentation](#-table-of-contents)

This document provides guidelines for defining the structure and properties of a Parquet file using a JSON schema. This schema aligns with Parquet-Java API terms, supporting complex types, including nested values in MAP structures.

## [Overview](#-table-of-contents)

- **Schema Version**: Draft-07 JSON Schema
- **Title**: Parquet File Schema
- **Description**: Defines Parquet file configuration with options for file metadata, writer settings, and column definitions.

## [Fields](#-table-of-contents)

### 1. File Definition

- **`fileName`** (string, required):  
  Specifies the name of the output Parquet file.
  
### 2. Writer Options

Contains additional options for configuring the Parquet writer.

- **`writerVersion`** (string):  
  Version of the Parquet writer. Defaults to `"1.0"`.
  - Options: `"1.0"`, `"2.0"`

- **`compression`** (string):  
  Compression codec to use. Defaults to `"SNAPPY"`.
  - Options: `"NONE"`, `"SNAPPY"`, `"GZIP"`, `"LZO"`, `"BROTLI"`, `"LZ4"`, `"ZSTD"`

- **`rowGroupSize`** (integer):  
  Size of row groups in bytes. Defaults to `134217728`.

- **`pageSize`** (integer):  
  Page size in bytes. Defaults to `1048576`.

- **`bloomFilter`** (string):  
  Bloom filter algorithm for columns. Defaults to `"none"`.
  - Options: `"none"`, `"all"`, `["column1", "column2"]` (specific columns)

### 3. Schema Definition

Defines the structure and properties of each column in the Parquet file. It includes column data types, nesting, and complex structures such as MAP.

- **`name`** (string, required):  
  Name of the column.

- **`schemaType`** (string, required):  
  Schema type for the column, aligning with Parquet-Java API.
  - Options: `"optional"`, `"required"`, `"repeated"`, `"optionalGroup"`, `"requiredGroup"`, `"repeatedGroup"`

- **`physicalType`** (string):  
  Physical data type of the column.
  - Options: `"INT32"`, `"INT64"`, `"BOOLEAN"`, `"FLOAT"`, `"DOUBLE"`, `"BINARY"`, `"FIXED_LEN_BYTE_ARRAY"`

- **`logicalType`** (string):  
  Logical data type, aligning with Parquet-Java OriginalType.
  - Options: `"UTF8"`, `"DECIMAL"`, `"DATE"`, `"TIME_MILLIS"`, `"TIME_MICROS"`, `"TIMESTAMP_MILLIS"`, `"TIMESTAMP_MICROS"`, `"ENUM"`, `"NONE"`, `"MAP"`, `"LIST"`, `"STRING"`, `"MAP_KEY_VALUE"`, `"TIME"`, `"INTEGER"`, `"JSON"`, `"BSON"`, `"UUID"`, `"INTERVAL"`, `"FLOAT16"`, `"UINT8"`, `"UINT16"`, `"UINT32"`, `"UINT64"`, `"INT8"`, `"INT16"`, `"INT32"`, `"INT64"`

#### Numeric Type Specifications

These properties are relevant for the `DECIMAL` logical type or `FIXED_LEN_BYTE_ARRAY` physical type:

- **`precision`** (integer):  
  Precision for `DECIMAL`. Minimum value is 1.
  
- **`scale`** (integer):  
  Scale for `DECIMAL`. Minimum value is 0.

- **`length`** (integer):  
  Length for `FIXED_LEN_BYTE_ARRAY`. Minimum value is 1.

#### Nested Fields and Complex Types

Defines nested structures or fields for group types:

- **`fields`** (array of objects):  
  Additional fields for grouped or nested columns (used with `optionalGroup`, `requiredGroup`, or `repeatedGroup` types).

#### MAP Column Key and Value Types

If a column has a MAP type, key and value schemas are specified separately.

- **`keyType`** (object):  
  Schema for MAP key:
  - **`physicalType`** (string): Physical type, options include `"INT32"`, `"INT64"`, `"BINARY"`.
  - **`logicalType`** (string): Logical type, options are `"UTF8"` or `"NONE"`.

- **`valueType`** (object):  
  Schema for MAP value, supporting complex nested types:
  - **`physicalType`** (string): Options include `"INT32"`, `"INT64"`, `"BINARY"`, `"BOOLEAN"`, `"FLOAT"`, `"DOUBLE"`, `"MAP"`, `"GROUP"`.
  - **`logicalType`** (string): Options include `"UTF8"`, `"DECIMAL"`, `"NONE"`.
  - **`fields`** (array of objects): Additional fields if `valueType` is a complex type, such as `GROUP` or nested `MAP`.

### [Example Usage](#-table-of-contents)

```json
{
  "fileName": "example.parquet",
  "options": {
    "writerVersion": "2.0",
    "compression": "GZIP",
    "rowGroupSize": 128000000,
    "pageSize": 1024000,
    "bloomFilter": "all"
  },
  "schema": [
    {
      "name": "id",
      "schemaType": "required",
      "physicalType": "INT32",
      "logicalType": "INTEGER"
    },
    {
      "name": "data",
      "schemaType": "optionalGroup",
      "fields": [
        {
          "name": "value",
          "schemaType": "optional",
          "physicalType": "BINARY",
          "logicalType": "UTF8"
        }
      ]
    }
  ]
}
```
---
