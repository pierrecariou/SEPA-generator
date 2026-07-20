package com.pcariou.view.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Hardening — {@link ConfigStore#write(AppConfig)} is atomic: a normal write
 * reloads exactly, an existing file is replaced cleanly, temporary files are
 * removed, and a failing write never truncates or destroys a previously valid
 * config file.
 */
public class ConfigStoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousOverride;
    private File configFile;
    private ConfigStore store;

    @Before
    public void redirectConfigToTempFile() {
        previousOverride = System.getProperty("sepa.config.file");
        configFile = new File(tmp.getRoot(), "config-" + System.nanoTime() + ".json");
        System.setProperty("sepa.config.file", configFile.getAbsolutePath());
        store = new ConfigStore();
    }

    @After
    public void restoreOverride() {
        if (previousOverride == null) {
            System.clearProperty("sepa.config.file");
        } else {
            System.setProperty("sepa.config.file", previousOverride);
        }
    }

    private static AppConfig debtorConfig(String debtorName) {
        AppConfig cfg = new AppConfig();
        cfg.debtor = new AppConfig.Debtor();
        cfg.debtor.name = debtorName;
        cfg.debtor.iban = "FR7630006000011234567890189";
        cfg.debtor.bic = "AGRIFRPP";
        cfg.initiatingParty = new AppConfig.InitiatingParty();
        cfg.initiatingParty.name = "Niryosys Initiating";
        cfg.initiatingParty.siret = "12345678901234";
        return cfg;
    }

    @Test
    public void overridePropertyControlsLocation() {
        assertEquals(configFile.getAbsolutePath(), store.file().getAbsolutePath());
    }

    @Test
    public void normalWriteSucceedsAndReloadsExactly() {
        assertTrue(store.write(debtorConfig("Niryosys SAS")));

        AppConfig reloaded = store.read();
        assertEquals("Niryosys SAS", reloaded.debtor.name);
        assertEquals("FR7630006000011234567890189", reloaded.debtor.iban);
        assertEquals("AGRIFRPP", reloaded.debtor.bic);
        assertEquals("Niryosys Initiating", reloaded.initiatingParty.name);
        assertEquals("12345678901234", reloaded.initiatingParty.siret);
    }

    @Test
    public void replacementOfExistingConfigSucceeds() {
        store.write(debtorConfig("First"));
        assertTrue(store.write(debtorConfig("Second")));
        assertEquals("Second", store.read().debtor.name);
    }

    @Test
    public void temporaryFilesAreCleanedUpAfterWrite() {
        store.write(debtorConfig("Niryosys SAS"));

        File[] temps = tmp.getRoot().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".tmp");
            }
        });
        assertEquals(0, temps.length);
    }

    @Test
    public void utf8ValuesRoundTrip() {
        store.write(debtorConfig("Société Générale — €uro façade Iñtërnâtiônàl"));
        assertEquals("Société Générale — €uro façade Iñtërnâtiônàl",
                store.read().debtor.name);
    }

    @Test
    public void unrelatedSectionsArePreserved() {
        AppConfig cfg = debtorConfig("Niryosys SAS");
        cfg.debtor.address = new AppConfig.Address();
        cfg.debtor.address.street = "Rue de la Paix";
        cfg.debtor.address.town = "Paris";
        cfg.debtor.address.country = "FR";
        cfg.fileSettings = new AppConfig.FileSettings();
        cfg.fileSettings.painFormat = "09";
        cfg.appearance = new AppConfig.Appearance();
        cfg.appearance.theme = "DARK";

        store.write(cfg);

        AppConfig reloaded = store.read();
        assertEquals("Rue de la Paix", reloaded.debtor.address.street);
        assertEquals("Paris", reloaded.debtor.address.town);
        assertEquals("FR", reloaded.debtor.address.country);
        assertEquals("09", reloaded.fileSettings.painFormat);
        assertEquals("DARK", reloaded.appearance.theme);
    }

    @Test
    public void writeFailsWhenDirectoryMissingAndDoesNotCreateDestination() {
        File missing = new File(tmp.getRoot(), "no-such-dir/config.json");
        System.setProperty("sepa.config.file", missing.getAbsolutePath());

        assertFalse(new ConfigStore().write(debtorConfig("Niryosys SAS")));
        assertFalse("A failed write must not create the destination", missing.exists());
    }

    @Test
    public void existingValidConfigIsPreservedWhenWriteFails() throws Exception {
        // A valid config already on disk.
        store.write(debtorConfig("Niryosys SAS"));
        byte[] original = Files.readAllBytes(configFile.toPath());

        // Force the next write to fail: point the store at a path whose "parent"
        // is the existing config file (not a directory), so the temp file cannot
        // be created and the move never runs.
        System.setProperty("sepa.config.file",
                new File(configFile, "nested.json").getAbsolutePath());

        assertFalse(new ConfigStore().write(debtorConfig("Replacement that must not land")));

        // The previously valid file is byte-for-byte intact — never truncated.
        assertArrayEquals(original, Files.readAllBytes(configFile.toPath()));
    }

    @Test
    public void missingFileReadsAsNull() {
        assertFalse(configFile.exists());
        assertNull(store.read());
    }
}
