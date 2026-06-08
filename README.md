<p align="center">
  <img src="view/resources/sepa_generator_logo.png" alt="SEPA Generator" width="420">
</p>

<p align="center">
  <img src="https://img.shields.io/hexpm/l/plug" alt="License">
  <img src="https://img.shields.io/github/v/release/pierrecariou/SEPA-generator" alt="Latest release">
</p>

# SEPA Generator

A desktop tool that generates a **SEPA Credit Transfer Initiation** XML file from a CSV or Excel
spreadsheet, following the **ISO 20022 `pain.001.001.02`** standard.

It validates and transforms your inputs against the ISO 20022 restrictions (IBAN, BIC, amounts,
SIRET, execution date), so the generated XML is valid and ready to be uploaded to your bank.

## Features

- 🖥️ **Swing desktop UI** – pick a file, choose an execution date, click *Generate*.
- 📥 **Multiple input formats** – `.csv`, `.xls` and `.xlsx`.
- ⚙️ **Settings panel** – configure your debtor and initiating-party details once; they are stored
  locally and reused for every generation.
- ✅ **Built-in validation** – IBAN (format + MOD-97 checksum), BIC, SIRET (14 digits) and a
  future execution date are all checked before generation.
- 📊 **Result summary** – after a successful run, a summary shows the number of transactions, the
  control sum and the execution date.
- 🧮 **Command-line mode** – run headless for scripting / batch jobs.

## Requirements

- Java 8 or later
- Apache Maven (to build from source)

## Build

This is a multi-module Maven project (`model`, `service`, `view`, `generator`):

```bash
mvn clean package
```

The runnable application is produced by the `generator` module.

## Usage (Desktop UI)

1. **Configure your settings.** Open **Settings** (gear icon in the header) and fill in:

   | Field | Description |
   | ----- | ----------- |
   | Debtor name | Your company / legal name |
   | Debtor IBAN | Your account IBAN (validated with MOD-97) |
   | Debtor BIC | Your bank's BIC/SWIFT code |
   | Initiating party name | Legal entity initiating the batch |
   | SIRET | 14-digit French company identifier |

   Settings are saved to `~/.sepa-generator-config.json`
   (`%USERPROFILE%\.sepa-generator-config.json` on Windows).

2. **Prepare your input file** (CSV / XLS / XLSX) with the following columns. The column order does
   not matter:

   | name | IBAN | BIC | amount | end_to_end_id | information |
   | ---- | ---- | --- | ------ | ------------- | ----------- |
   | Karlson GmbH | GB29NWBK60161331926819 | BANKNL2A | 3000.00 | 127A | furnitures |
   | ... | ... | ... | ... | ... | ... |

   See `Payments-template-example.csv` for a working example.

3. **Select the input file**, choose the **execution date** (must be in the future), and click
   **Generate**.

4. The output **XML** file is written to the location of your choice, and a summary of the batch is
   displayed. Rows or fields that don't respect the ISO 20022 formatting produce explicit error
   messages.

## Usage (Command line)

```bash
java -jar generator/target/generator.jar <input.csv|xls|xlsx> <output.xml>
```

The input and output paths must be different, and the output must end in `.xml`.

## ISO 20022 Format

The generated document targets the **`pain.001.001.02`** SEPA Credit Transfer Initiation schema
(XML namespace `urn:iso:std:iso:20022:tech:xsd:pain.001.001.02`).

## Project Structure

| Module | Responsibility |
| ------ | -------------- |
| `model` | JAXB-annotated ISO 20022 `pain.001.001.02` data model + CSV bindings & validation constraints |
| `service` | CSV/Excel reading, validation and XML marshalling (`CsvToBeans`, `BeansToXml`) |
| `view` | Swing user interface (main window, settings, status bar) |
| `generator` | Application entry point wiring the view and service together |

## Documentation

- [Usage guide](docs/usage.md)

## Authors

- [Pierre Cariou (GitHub)](https://github.com/pierrecariou)
- Email: pierrecariou@outlook.fr

## License

This project is licensed under the Apache 2.0 License
(https://www.apache.org/licenses/LICENSE-2.0).
