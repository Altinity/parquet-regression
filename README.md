# Altinity Parquet Regression Tests

This project is designated for testing Parquet files. It includes tools for generating and validating Parquet files to ensure they meet specific requirements and standards.

## Available Tools

- [Parquetify](#-parquetify)
  - [How To Generate Parquet Files](#how-to-generate-parquet-files)
    - [Dependencies](#dependencies)
    - [Steps To Build the Project and Generate Parquet Files](#steps-to-build-the-project-and-generate-parquet-files)
  - [Example JSON Schema for Parquet File](#example-json-schema-for-parquet-file)

## 📊 Parquetify

Parquetify provides functionality for generating Parquet files from JSON schema definitions. It includes features for specifying encoding types, enabling bloom filters, and handling both primitive and logical data types. Parquetify is built using Java and Maven, and it is specifically designed to facilitate regression testing for Parquet file generation.

### How To Generate Parquet Files

In order to generate parquet files you need to build the project and provide the JSON schema file that describes the structure of the parquet file. The tool will then generate the parquet file based on the provided schema.

### Dependencies

- Java 11
- Maven

### Steps To Build the Project and Generate Parquet Files

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

The example of the JSON schema file can be found [here](https://github.com/Altinity/parquet-regression/blob/main/src/schema-example/json/exampleSchema.json)
