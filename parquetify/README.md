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

To generate your first Parquet file you can use our pre-made example JSON located in our [schema-example](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/example.json) folder.

```shell
parquetify -j example.json -o /path/to/output/parquet/file.parquet
```
> [!WARNING]
> The tool allows you to specify any kind of structure for the Parquet file, including incorrect ones, in that case the 
> file will be generated but the tool or DBMS you are trying to read it with will not be able to read from it.
> 
### How to Build the JSON for Parquetify and 

`Parquetify` uses JSON file to determine the file structure and the values that will be written to the Parquet file. 
Because the JSON can be built in many different way we have [specific JSON schema](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/schema.json)
that defines the structure and validation rules for a JSON document.

# Missing Functionality

**Generating Parquet files with:**

* encodings: `DELTA_BYTE_ARRAY`, `DELTA_LENGTH_BYTE_ARRAY`, `RLE`, `BIT_PACKED`, `DELTA_BINARY_PACKED`
* inserting data into `FLOAT16` columns
* encryption on any level
* compressions: `GZIP`, `LZO`, `BROTLI`, `LZ4`