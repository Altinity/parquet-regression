# Altinity Parquet Regression Tests

## How to use parquet-generator

1. Install java 11

```shell
apt install openjdk-11-jre-headless
```

2. Install maven

```shell
apt install maven
```

3. From the root directory of the project, run the following command to build the project

```shell
mvn clean package
```

4. Navigate to the generated `target` directory and run the following command to generate the parquet files

```shell
java -jar parquet-generator.jar --json ../src/schema-example/json/exampleSchema.json --output test.parquet
```
> [!NOTE]
> The `--json` flag is used to specify the schema file and the `--output` flag is used to specify the output file name.