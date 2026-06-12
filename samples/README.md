# Sample input files

Manual test inputs for the SEPA Generator Community Edition. All names, IBANs,
BICs and references are **fake demo data** — syntactically valid where needed,
but not tied to any real person, company or bank account. Do not use them for
real payments.

All files use the column layout expected by the application:

```
name,IBAN,BIC,amount,end_to_end_id,information
```

## valid/

| File | Purpose |
|------|---------|
| `sepa-valid-sample.csv`  | 5 realistic transactions; generates a SEPA XML successfully (total 5305.34 EUR). |
| `sepa-valid-sample.xlsx` | Same 5 transactions in Excel (xlsx) format. |
| `sepa-valid-sample.xls`  | Same 5 transactions in legacy Excel (xls) format. |

The Excel files were produced with the project's Aspose Cells dependency in
evaluation mode, so they contain an extra "Evaluation Warning" sheet/line.
The application strips this automatically during Excel-to-CSV conversion;
generation output is unaffected.

## invalid/

Each file demonstrates one clear validation scenario and should produce a
clear error message instead of an XML file.

| File | Scenario |
|------|----------|
| `sepa-invalid-bic.csv`           | Creditor BIC `ABCD123` — wrong length and digits in the bank code. |
| `sepa-invalid-amount.csv`        | One row per amount error: zero, negative, more than 2 decimals, not a number. |
| `sepa-invalid-missing-field.csv` | Mandatory creditor IBAN left empty. |
| `sepa-invalid-mixed-errors.csv`  | One valid row plus rows combining BIC, amount and missing-field errors. |

## How to use

1. Start the SEPA Generator and configure any debtor in Settings
   (demo values work, e.g. IBAN `FR1420041010050500013M02606`, BIC `BNPAFRPP`).
2. Select a sample file as the credit transfer input and pick a future
   execution date.
3. Click **Generate**:
   - files under `valid/` produce a `pain.001.001.02` XML and a generation summary;
   - files under `invalid/` show a validation error describing the problem.
