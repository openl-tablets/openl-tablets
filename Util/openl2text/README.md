# OpenL2Text

OpenL2Text is a CLI tool that converts OpenL Tablets projects or single Excel files to text format.

## Features

- Converts OpenL Tablets projects or single Excel files to text format.
- Provides various options to customize the output.
- Supports processing of Excel files, zip files, and directories.

## Requirements

- Java 8 or higher
- Maven

## Usage

You can run the tool using the following command:

```bash
java -jar openl2text.jar [options] <input files or folders>

[options]
-o, --output-dir <path>: Specifies the output directory path.
--omit-types: Specifies whether to exclude details of table arguments in the output.
--types-deep <number>: Specifies how many levels of types to include in the output.
--aliases-as-base: Specifies whether to replace Alias types with base type in the output.
--include-dimensional-properties: Specifies whether to include dimensional properties in the output.
--omit-method-refs: Specifies whether to exclude details of referenced methods in the output.
--include-all-rules-methods: Specifies whether to include all rules methods details in the output.
--table-as-code: Present tables as pseudo-code.
--omit-dispatching-methods: If specified only one method from dispatching methods will be included in the output.
--only-method-cells: If specified only method cells will be included in the output. This option is ignored if --table-as-code is not specified.
--max-rows <number>: Specifies maximum number of rows to include in the output. If not specified all rows will be included.
--parsing-mode: If specified the tables will not be compiled. This option can be used if Excel file contains non OpenL Tablets rules.
```

## Building
To build the project, navigate to the project directory and run the following command:

```bash
mvn clean install
```
This will generate a jar file in the target directory.

## Contributing
Contributions are welcome. Please open a pull request with your changes.