SEPA Generator - input templates
================================

Each template has a header row and one example row. Replace the example row
with your own payments. Keep the header row unchanged.

Columns
-------
Required (all formats):
  name             Creditor name
  IBAN             Creditor IBAN
  BIC              Creditor BIC
  amount           Amount, greater than 0, at most 2 decimals
  end_to_end_id    End-to-end identifier
  information      Remittance information

Optional address columns (pain.001.001.09 only):
  street, building_number, postcode, town, country

The address columns are OPTIONAL. They are only used when generating
pain.001.001.09 output. For pain.001.001.02 output, the address columns are
ignored by the current Community Edition, so you can leave them empty or omit
them entirely (use a "Basic" template).

Files
-----
  sepa-template-basic.csv / .xlsx
      Required columns only.

  sepa-template-with-optional-address.csv / .xlsx
      Required columns plus the optional address columns for pain.001.001.09.
