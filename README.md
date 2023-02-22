# SEPA-generator

![Hex.pm](https://img.shields.io/hexpm/l/plug)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/pierrecariou/SEPA-generator)

A SEPA Generator following the ISO 20022 pain.001.001.02 XML Format

![image](https://user-images.githubusercontent.com/46349842/220659342-0b696b42-76ca-4d7a-971c-f3b694a7dad9.png)


### About the Software

Simple tool that will generate a SEPA valid XML File from a CSV File (or directly from an Excel file - .xls or .xlsx) \
It will validate/transform the inputs based on the ISO 20022 restrictions, thus ensuring the validity of the generated XML sepa file.

## Usage

1. You should have provided the Debtor infos inside the JSON file DebtorInformations.json; at the root of the solution.

2. The input file shoud be under a specific format: CSV or XLS or XLSX. \
The input file should have detailed the Creditors infos under those colums names:

| name          | IBAN          | BIC           | amount        | end_to_end_id | information   |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  | Content Cell  |
| ...           | ...           | ...           | ...           | ...           | ...           |

The column order does not matter.
Some error messages will be displayed if you're not respeccting the right formatting for the infos.

3. Select the Execution Date for the operations.

4. The ouptut (XML) file will be created at the location of your choice.

## Authors

* [Pierre Cariou github](https://github.com/pierrecariou)
* email: pierrecariou@outlook.fr

## License

This project is licensed under the MIT License
