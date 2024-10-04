# Altinity Parquet Regression Tests


This project provides a tool for generating Parquet files from JSON schema definitions. It includes functionality for specifying encoding types, enabling bloom filters, and handling various data types. The tool is built using Java and Maven, and it is designed to facilitate regression testing for Parquet file generation.

## How To Generate Parquet Files

In order to generate parquet files you need to build the project and provide the JSON schema file that describes the structure of the parquet file. The tool will then generate the parquet file based on the schema provided.

### Dependencies

- Java 11
- Maven

### Steps To Build the Project and Generate Parquet Files

1. Clone the repository

```shell
git clone https://github.com/Altinity/parquet-regression.git
````

2. Install `java 11`

```shell
sudo apt install openjdk-11-jre-headless
```

3. Install `maven`

```shell
sudo apt install maven
```

4. From the root directory of the project, run the following command to build the project

```shell
mvn clean package
```

5. Navigate to the newly created `target` directory and run the following command to generate the parquet files

```shell
java -jar parquet-generator.jar --json ../src/schema-example/json/exampleSchema.json --output test.parquet
```
> [!NOTE]
> The `--json` flag is used to specify the JSON schema file and the `--output` flag is used to specify the output file name.

## Example JSON Schema for Parquet File

The example of the JSON schema file can be found [here](https://github.com/Altinity/parquet-regression/blob/main/src/schema-example/json/exampleSchema.json)
