<p align="center">
  <img src="docs/assets/sepa-generator-logo.png" alt="SEPA Generator logo" width="96" />
</p>

<h1 align="center">SEPA Generator</h1>

<p align="center">
  Generate SEPA Credit Transfer XML files from CSV or Excel spreadsheets.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-Apache--2.0-blue" alt="License: Apache 2.0" />
  <img src="https://img.shields.io/badge/release-v1.3-teal" alt="Release v1.3" />
  <img src="https://img.shields.io/badge/format-pain.001.001.02-lightgrey" alt="pain.001.001.02" />
</p>

---

## Overview

**SEPA Generator** is a local desktop application that generates **SEPA Credit Transfer Initiation XML** files from a CSV or Excel input file.

It targets the ISO 20022 `pain.001.001.02` format and helps transform payment data into a structured SEPA XML file.

The application is designed to stay simple:

1. Configure debtor information.
2. Select a payment input file.
3. Choose an execution date.
4. Generate the SEPA XML file.

> Always review and validate generated payment files before submitting them to your bank. Bank acceptance may depend on your bank, country, upload channel, and required `pain.001` version.

---

## Screenshots

### Light theme

![SEPA Generator light theme](docs/assets/sepa-generator-light.png)

### Dark theme

![SEPA Generator dark theme](docs/assets/sepa-generator-dark.png)

### Settings

![SEPA Generator settings](docs/assets/sepa-generator-settings.png)

### Error handling

![SEPA Generator error handling](docs/assets/sepa-generator-error.png)

---

## Features

* Desktop UI built with Java Swing and FlatLaf.
* Generate SEPA Credit Transfer XML files in `pain.001.001.02` format.
* Import payments from:

  * `.csv`
  * `.xls`
  * `.xlsx`
* Configure debtor and initiating party information in the settings panel.
* Validate key fields before generation:

  * IBAN format and checksum
  * BIC format
  * SIRET format
  * execution date
  * payment amounts
* Display clear status messages.
* Show a generation summary after successful generation:

  * transaction count
  * total amount
  * execution date
* Open the generated file or output folder directly from the app.
* Remember the last used input directory.
* Light and dark themes.
* Command-line mode for simple batch usage.
* Fully local Community Edition.

---

## Requirements

* Java 8 or later
* Apache Maven, if building from source

---

## Build

This is a multi-module Maven project:

* `model` — JAXB-annotated ISO 20022 `pain.001.001.02` model and CSV bindings
* `service` — CSV/Excel reading, validation, and XML generation
* `view` — Swing desktop user interface
* `generator` — application entry point and wiring

Build the project with:

```bash
mvn clean package
```

The runnable application is produced by the `generator` module.

---

## Usage — Desktop Application

### 1. Configure settings

Open the settings window from the header icon and fill in the debtor and initiating party information.

| Field                 | Description                               |
| --------------------- | ----------------------------------------- |
| Debtor name           | Your company or legal name                |
| Debtor IBAN           | Your debtor account IBAN                  |
| Debtor BIC            | Your bank BIC/SWIFT code                  |
| Initiating party name | Legal entity initiating the payment batch |
| SIRET                 | 14-digit French company identifier        |

Settings are stored locally and reused for future generations.

Default config location:

| OS            | Location                                    |
| ------------- | ------------------------------------------- |
| Windows       | `%USERPROFILE%\.sepa-generator-config.json` |
| macOS / Linux | `~/.sepa-generator-config.json`             |

---

### 2. Prepare your input file

The input file can be a CSV or Excel spreadsheet.

Supported formats:

```text
.csv
.xls
.xlsx
```

The expected columns are:

| Column          | Description                      |
| --------------- | -------------------------------- |
| `name`          | Creditor name                    |
| `IBAN`          | Creditor IBAN                    |
| `BIC`           | Creditor BIC/SWIFT code          |
| `amount`        | Transfer amount                  |
| `end_to_end_id` | End-to-end payment identifier    |
| `information`   | Remittance / payment information |

The column order does not matter.

A working example is available in the repository:

```text
Payments-template-example.csv
```

---

### 3. Generate the XML

In the desktop application:

1. Select the input file.
2. Select the execution date.
3. Click **Generate**.
4. Review the generated XML file.
5. Submit the file to your bank only after validation.

After successful generation, the app displays a summary with:

* number of transactions
* total amount
* execution date
* generated file link
* output folder link

---

## Usage — Command Line

The project also supports command-line generation.

Example:

```bash
java -jar generator/target/generator.jar <input.csv|xls|xlsx> <output.xml>
```

The input and output paths must be different, and the output file must end with:

```text
.xml
```

---

## ISO 20022 Format

The generated document targets:

```text
pain.001.001.02
```

Namespace:

```text
urn:iso:std:iso:20022:tech:xsd:pain.001.001.02
```

This is a SEPA Credit Transfer Initiation format.

Some banks may require newer `pain.001` versions or bank-specific rules. Always check with your bank before using generated files in production.

---

## Documentation

For more detailed instructions, see:

* [Usage Guide](docs/usage.md)

---

## Tests

Run the test suite with:

```bash
mvn clean test
```

Build the project with:

```bash
mvn clean package
```

---

## Community Edition and Future Professional Features

This repository contains the **Community Edition** of SEPA Generator.

The Community Edition is free and open source. It is intended to remain a useful local tool for generating SEPA XML files from CSV or Excel input files.

Future professional features may be developed separately, such as:

* newer `pain.001` version support
* advanced SEPA validation reports
* bank-specific validation profiles
* multiple debtor/company profiles
* batch workflows
* accountant-oriented features
* paid support

---

## Security and Privacy

SEPA Generator runs locally on your machine.

The Community Edition does not require uploading payment files to an external server.

You remain responsible for:

* checking input data
* reviewing generated XML files
* validating files with your bank
* protecting real banking data
* avoiding commits of private configuration or real payment files

Do not commit real payment data, private configuration files, or sensitive banking information to the repository.

---

## Contributing

Contributions are welcome.

You can help by:

* reporting bugs
* improving documentation
* testing the application with different banks or input files
* suggesting validation improvements
* opening pull requests

Before contributing, please make sure the project builds and tests pass:

```bash
mvn clean test
```

---

## Author

* [Pierre Cariou](https://github.com/pierrecariou)

---

## License

This project is licensed under the Apache License 2.0.

See [LICENSE](LICENSE) for details.

