# SEPA-generator

A SEPA Generator following the ISO 20022 pain.001.001.02 XML Format

### About the Software

Simple tool that will generate a SEPA valid XML File from a CSV File (or directly from an Excel file - .xls or .xlsx)

## Usage

1. You should have provided the Debtor infos inside the JSON file DebtorInformations.json; at the root of the solution.

2. The input file shoud be under a specific format: CSV or XLS or XLSX. \
The input file should have detailed the Creditors infos under those colums names:

| name          | IBAN          | BIC           | amount        | end_to_end_id | information   |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  |
| ...           | ...           | ...           | ...           | ...           | ...           |

The order does not matter.
Some error messages will be displayed if you're not respeccting the right formatting for the infos.

3. The ouptut (XML) file will be created at the location of your choice. 

## Authors

* [Pierre Cariou github](https://github.com/pierrecariou)
* email: pierrecariou@outlook.fr

## License

This project is licensed under the MIT License
