# How To Generate Parquet Files

In order to generate parquet files you need to build the project and provide the JSON schema file that describes the structure of the parquet file. The tool will then generate the parquet file based on the provided schema.

# Dependencies

- Java 11
- Maven

# Steps To Build the Project and Generate Parquet Files

1. Clone the repository and move to the `parquetify` directory

```shell
git clone https://github.com/Altinity/parquet-regression.git
cd parquet-regression/parquetify
````

2. Install `java 11`

```shell
sudo apt install openjdk-11-jre-headless
```

3. Install `maven`

```shell
sudo apt install maven
```

4. From the `parquetify` directory of the project, run the following command to build the project

```shell
mvn clean package
```

5. Run the following command to install the project from the generated `.deb`

```shell
sudo dpkg -i parquetify_0.0.6-1_amd64.deb
```
> [!WARNING]
> There might be an issue with some dependencies on ubuntu 24.04 so for that run the following command:
> ```shell
> sudo apt-get install -f
>  ```
> Once done repeat the step 5.

6. To use the tool, run the following command

```shell
parquetify -j <path_to_json_schema> -o <output_directory>
```
> [!NOTE]
> The `--json` flag is used to specify the JSON schema file and the `--output` flag is used to specify the output file name.


## Example JSON Schema for Parquet File

The example of the JSON schema file can be found [here](https://github.com/Altinity/parquet-regression/blob/main/parquetify/src/schema-example/json/schema.json)

# Functionality That Is Not Yet Supported

- Setting different encoding on different columns
- Some encodings: `DELTA_BYTE_ARRAY`, `DELTA_LENGTH_BYTE_ARRAY`
- Inserting data into `FLOAT16` columns
- Creating parquet files with `Page v2` header (need to investigate what `Page v2` header implies)
- Encryption on any level
- The following compressions not supported `["GZIP", "LZO", "BROTLI", "LZ4"]`