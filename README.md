# SEPA-generator

A SEPA Generator following the ISO 20022 pain.001.001.02 XML Format

### About the project

This is a simple tool that will generate a SEPA valid XML File from a CSV File (or directly from an Excel file - .xls or .xlsx)

## Getting Started

This project is build with Java, thus it should be runnable under any OS.

### Prerequisites

Java should be installed by default on your machine.

### Installation

1. Clone the repo
```
git clone git@github.com:pierrecariou/SEPA-generator.git
cd SEPA-generator
```
2. (OPTIONAL - You can skip this part) The Project is already built but feel free to recompile it with maven: \
Install maven if necessary
```
mvn clean package
```
## Usage

1. You should have provided the Debtor infos inside the JSON file DebtorInformations.json; at the root of the project.

2. The input file shoud be under a specific format: CSV or XLS or XLSX. \
The input file should have detailed the Creditors infos under those colums names:

| name          | IBAN          | BIC           | amount        | end_to_end_id | information   |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  |
| ...           | ...           | ...           | ...           | ...           | ...           |

The order does not matter.
Some error messages will be displayed if you're not respeccting the right formatting for the infos.
But as it's the Version 1.0 I may have forgotten some restrictions. Please check the validity of the XML File before using it: \
[sepa_xml_validation](https://www.mobilefish.com/services/sepa_xml_validation/sepa_xml_validation.php)

3. Running cmd:
```
java -jar generator/target/generator-1.0.jar [file.csv or file.xls or file.xlsx] output_file.xml
```

4. The output file will be created under the [output_file.xml] name (if the entry file was valid).

### Example
```
java -jar generator/target/generator-1.0.jar  Payments-template-example.xlsx payments-2023-01-04.xml
```

## Built With
* [Maven](https://maven.apache.org/) - Dependency Management 

## Authors

* [Pierre Cariou](https://github.com/pierrecariou)

See also the list of [contributors](https://github.com/pierrecariou/SEPA-generator/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
