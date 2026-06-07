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

    public static class Debtor {
        public String name;
        public String iban;
        public String bic;
    }

    public static class InitiatingParty {
        public String name;
        public String siret;
    }

    public static class FileSettings {
        public String defaultInputPath;
        public String defaultOutputPath;
    }
}
