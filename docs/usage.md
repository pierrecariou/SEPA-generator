# SEPA Generator — Usage Guide

## Prerequisites

- Java 11 or later
- A valid SEPA creditor account (IBAN + BIC)
- A CSV file following the payment template format

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

### 2. Prepare your CSV file

Use the provided template (`Payments-template-example.csv`) as a reference. Each row represents one credit transfer.

Required columns:

| Column | Format | Example |
|---|---|---|
| Creditor name | Text | `ACME Corp` |
| Creditor IBAN | IBAN | `DE89370400440532013000` |
| Creditor BIC | BIC | `COBADEFFXXX` |
| Amount | Decimal | `1500.00` |
| Currency | ISO 4217 | `EUR` |
| Reference | Text (max 35 chars) | `INV-2024-001` |
| End-to-end ID | Text (max 35 chars) | `E2E-20240101-001` |

### 3. Generate the SEPA XML

1. Click **Browse** and select your CSV file.
2. Choose the **execution date** (must be a future business day).
3. Click **Generate XML**.
4. The output XML file will be saved in the same directory as your CSV, or in the configured output directory.

A summary card shows the number of transactions, total amount, and execution date after successful generation.

## Output Format

The generated file is a **SEPA Credit Transfer Initiation** XML (`pain.001.001.02`) compliant with the ISO 20022 standard, accepted by most European banks.

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
