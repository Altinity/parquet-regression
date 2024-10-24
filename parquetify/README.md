# Generating Parquet Files From JSON With the Parquetify

A simple tool that utilizes the [parquet-java](https://github.com/apache/parquet-java) library to generate Parquet files based on the structure provided from a JSON file.

# Features

**Generating Parquet files with:**

* specific physical data types
* specific logical data types
* specific precision and scale for `DECIMAL`
* compressions: `SNAPPY`, `ZSTD` or leaving the file `UNCOMPRESSED`
* encodings: `DICTIONARY`, `BYTE_STREAM_SPLIT`, `PLAIN`
* bloom filter on a specific column or on all columns including columns inside a group
* specifying a writer version: `1.0`, `2.0`
* specific row group size
* specific page size


# Table of Contents

* [Quick Start](#quick-start)
  * [Create Your First Parquet File](#create-your-first-parquet-file) 
  * [How to Correctly Build the JSON for Parquetify](#how-to-correctly-build-the-json-for-parquetify)
    * [How to Build Parquet with regular types](#how-to-build-parquet-with-regular-types)
    * [How to Build Parquet with nested types](#how-to-build-parquet-with-nested-types)
* [Missing Functionality](#missing-functionality)


# Quick Start

To use the `parqeutify` tool you can simply download the latest release from the [releases page](https://github.com/Altinity/parquet-regression/releases)

```shell
sudo apt update
wget https://github.com/Altinity/parquet-regression/releases/download/1.0.3/parquetify_1.0.3_amd64.deb
```

> [!NOTE]
> Download the package corresponding to your system architecture. ARM and x86_64 are supported.

After downloading the release you will have a `.deb` package that you can install with the following command:

```shell
sudo apt install ./parquetify_1.0.3_amd64.deb
```

Run the `parquetify` in your terminal to check if it was installed

```shell
root@eb1f5ede14df: parquetify
Error parsing command line arguments: Missing required options: j, o
usage: GenerateParquet
 -j,--json <arg>     Path to the JSON file
 -o,--output <arg>   Output path for the Parquet file\
```

Now you can pass the JSON file to the `parquetify` tool to generate Parquet files based on the values in the given JSON.

## Create Your First Parquet File

To generate your first Parquet file you can use our pre-made example JSON located in our [schema-example](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/example.json) folder.

```shell
parquetify -j example.json -o /path/to/output/parquet/file.parquet
```
![Peek 2024-10-24 17-24](https://github.com/user-attachments/assets/d7c27bff-ba00-40bc-987b-c446ed8c7bd0)

> [!WARNING]
> The tool allows you to specify any kind of structure for the Parquet file, including incorrect ones, in that case the 
> file will be generated but the tool or DBMS you are trying to read it with will not be able to read from it.

## How to Correctly Build the JSON for Parquetify

`Parquetify` uses JSON file to determine the file structure and the values that will be written to the Parquet file. 
Because the JSON can be built in many different ways we have [specific JSON schema](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/schema.json)
that defines the structure and validation rules for a JSON document.

simple structure of JSON looks like this:

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

> [!IMPORTANT]
> 
> The values inside the `schema` in JSON take care of the actual schema of the Parquet file. The data about a single
column should be enclosed inside the `{}` symbols.

### How to Build Parquet with regular types

A simple example with `INT32` would look like this:

```json
    {
      "name": "id",
      "schemaType": "required",
      "physicalType": "INT32",
      "logicalType": "INT8",
      "data": [1, 2, 3, 4, 5]
    }
```

Where `name` is a name of the column, `required` in `schemType` indicates that there are no null values allowed for this field.
`physicalType` is the actual physical type of the column, `logicalType` is the logical type of the column. In order to 
insert data into the column, you need to provide the `data` field with an array of values.

The idea here is that you can specify only `physicalType` and you will get the column with the INT32 physical type and logical type `NONE`. 
But only specifying `logicalType` will not work. 

```json
    {
      "name": "id",
      "schemaType": "required",
      "physicalType": "INT32",
      "data": [1, 2, 3, 4, 5]
    }
```

> [!NOTE]
> Read more about parquet datatypes [here](https://parquet.apache.org/docs/file-format/types/)

### How to Build Parquet with nested types

A simple example with a nested type would look like this:

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

Here the `"schemaType": "repeatedGroup"` indicates that we want to create an `array`. `requiredGroup` and `optionalGroup` 
would create a field with `tuple` datatype.

# Missing Functionality

**Generating Parquet files with:**

* encodings: `DELTA_BYTE_ARRAY`, `DELTA_LENGTH_BYTE_ARRAY`, `RLE`, `BIT_PACKED`, `DELTA_BINARY_PACKED`
* inserting data into `FLOAT16` columns
* encryption on any level
* compressions: `GZIP`, `LZO`, `BROTLI`, `LZ4`
