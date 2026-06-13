# SEPA Generator — Usage Guide

## Prerequisites

- Java 8 or later
- A valid SEPA creditor account (IBAN + BIC)
- A CSV or Excel file following the payment template format

## Getting Started

### 1. Configure your debtor settings

Open **Settings** (gear icon in the header) and fill in:

| Field | Description |
|---|---|
| Debtor name | Your company or legal name |
| Debtor IBAN | Your bank account IBAN (e.g. `FR7630006000011234567890189`) |
| Debtor BIC | Your bank's BIC/SWIFT code (e.g. `BNPAFRPP`) |
| Initiating party name | Legal entity initiating the transfer batch |
| SIRET | 14-digit French company identifier |

Click **Save**. The status bar will confirm configuration is complete.

### 2. Prepare your input file

Use the provided sample (`samples/valid/sepa-valid-sample.csv`) as a reference. The input may be a `.csv`, `.xls`, or `.xlsx` file. Each row represents one credit transfer.

Required columns:

| Column | Format | Example |
|---|---|---|
| `name` | Text | `ACME Corp` |
| `IBAN` | IBAN | `DE89370400440532013000` |
| `BIC` | BIC | `COBADEFFXXX` |
| `amount` | Decimal, > 0, max 2 decimals | `1500.00` |
| `end_to_end_id` | Text (max 35 chars) | `E2E-20240101-001` |
| `information` | Text (remittance information) | `INV-2024-001` |

The column order does not matter.

For `pain.001.001.09`, you may optionally add structured creditor postal address columns (`street`, `building_number`, `postcode`, `town`, `country`). When any address field is provided, `town` and a 2-letter ISO `country` code are required. Files without address columns remain fully supported.

### 3. Generate the SEPA XML

1. Click **Browse** and select your input file.
2. Choose the **execution date** (must be a future date).
3. Choose the **SEPA format** (`pain.001.001.02` or `pain.001.001.09`).
4. Click **Generate**.
5. The output XML file will be saved in the same directory as your input file, or in the configured output directory.

A summary card shows the number of transactions, total amount, and execution date after successful generation.

## Output Format

SEPA Generator produces SEPA Credit Transfer Initiation XML in two ISO 20022 formats:

- `pain.001.001.02` (classic)
- `pain.001.001.09` (modern ISO 20022, with optional structured postal addresses)

The files are designed to follow the ISO 20022 standard. Final bank acceptance can depend on your bank, upload channel, account configuration, the required `pain.001` version, and bank-specific rules.

## Command Line

SEPA Generator can also be run from the command line:

```bash
java -jar generator.jar <input.csv|.xls|.xlsx> <output.xml> <YYYY-MM-DD> [--format=02|09]
```

- `<input>` — payment input file (`.csv`, `.xls`, or `.xlsx`)
- `<output>` — destination file, must end with `.xml`
- `<YYYY-MM-DD>` — execution date (must be a future date)
- `--format=02|09` — optional SEPA format; defaults to `02`

Debtor and initiating party information is read from the local configuration file.

## Settings File

Settings are stored locally at:

- **Windows**: `%USERPROFILE%\.sepa-generator-config.json`
- **macOS / Linux**: `~/.sepa-generator-config.json`

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Generate button disabled | No CSV selected, or execution date missing |
| Settings banner in header | Required fields not yet configured |
| XML rejected by bank | Check BIC/IBAN format and execution date |
| Empty creditor rows in CSV | Rows with missing mandatory fields are skipped |

## Support

- [GitHub repository](https://github.com/pierrecariou/SEPA-generator)
- [Report an issue](https://github.com/pierrecariou/SEPA-generator/issues/new)
