package com.pcariou.view.config;

/**
 * In-memory representation of {@code ~/.sepa-generator-config.json}.
 *
 * Field names map 1:1 to the persisted JSON keys; do not rename them without
 * a matching migration, as that would change the on-disk config format.
 */
public class AppConfig {

    public Debtor debtor;
    public InitiatingParty initiatingParty;
    public FileSettings fileSettings;
    public Appearance appearance;

    public static class Debtor {
        public String name;
        public String iban;
        public String bic;
        /** Optional postal address, used in pain.001.001.09 output only. */
        public Address address;
    }

    /** Optional postal address; all fields optional, see model validation rules. */
    public static class Address {
        public String street;
        public String buildingNumber;
        public String postcode;
        public String town;
        public String country;
    }

    public static class InitiatingParty {
        public String name;
        /**
         * Optional initiating party identifier value. The historical key name
         * is kept for compatibility; the value is any organisation identifier
         * (e.g. a SIRET or a bank-assigned identifier).
         */
        public String siret;
        /** Optional ISO scheme code ({@code SchmeNm/Cd}), e.g. BANK or SRET. */
        public String schemeCode;
        /** Optional proprietary scheme name ({@code SchmeNm/Prtry}). */
        public String schemeProprietary;
        /** Optional identifier issuer ({@code Issr}). */
        public String issuer;
    }

    public static class FileSettings {
        public String defaultInputPath;
        public String defaultOutputPath;
        /** Persisted pain.001 version code ("02" / "09"); null means default (02). */
        public String painFormat;
    }

    public static class Appearance {
        public String theme;
    }
}
