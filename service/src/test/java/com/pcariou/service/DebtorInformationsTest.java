package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pcariou.model.InitiatingPartyIdentifier;

import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

/**
 * Covers the deterministic, UTF-8, try-with-resources config read in
 * {@link DebtorInformations}: all fields still parse, accented values round-trip,
 * and the config file can be replaced immediately after reading (no lingering
 * handle) without any GC/finalization/retry workaround.
 */
public class DebtorInformationsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;
    private File config;

    @Before
    public void redirectConfigToTempFile() throws Exception {
        previousConfigProperty = System.getProperty("sepa.config.file");
        config = tmp.newFile("config-" + System.nanoTime() + ".json");
        writeConfig("Sociète Générale — Iñtërnâtiônàl", "Nïryosys éditeur");
        System.setProperty("sepa.config.file", config.getAbsolutePath());
    }

    @After
    public void restoreConfigProperty() {
        if (previousConfigProperty == null) {
            System.clearProperty("sepa.config.file");
        } else {
            System.setProperty("sepa.config.file", previousConfigProperty);
        }
    }

    private void writeConfig(String debtorName, String partyName) throws Exception {
        String json = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"" + debtorName + "\",\n"
                + "    \"iban\": \"GB29NWBK60161331926819\",\n"
                + "    \"bic\": \"BNPAFRPP\"\n"
                + "  },\n"
                + "  \"initiatingParty\": {\n"
                + "    \"name\": \"" + partyName + "\",\n"
                + "    \"siret\": \"12345678901234\"\n"
                + "  }\n"
                + "}\n";
        Files.write(config.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readsAllFieldsWithAccentedUtf8RoundTrip() throws Exception {
        DebtorInformations info = new DebtorInformations(LocalDate.now().plusDays(7));

        assertEquals("Sociète Générale — Iñtërnâtiônàl", info.name);
        assertEquals("GB29NWBK60161331926819", info.iban);
        assertEquals("BNPAFRPP", info.bic);
        assertEquals("Nïryosys éditeur", info.initiatingPartyName);
        assertEquals("12345678901234", info.initiatingPartyIdentifier.getId());
        assertNull("A legacy SIRET-only config has no scheme", info.initiatingPartyIdentifier.getSchemeCode());
        assertNull(info.initiatingPartyIdentifier.getSchemeProprietary());
        assertNull(info.initiatingPartyIdentifier.getIssuer());
        assertTrue(validator().validate(info).isEmpty());
    }

    @Test
    public void readsInternationalIdentifierWithSchemeAndIssuer() throws Exception {
        writeJson("{ \"debtor\": { \"name\": \"Acme\", \"iban\": \"GB29NWBK60161331926819\", \"bic\": \"BNPAFRPP\" },"
                + " \"initiatingParty\": { \"name\": \"Acme\", \"siret\": \"IE77889900\","
                + " \"schemeCode\": \"BANK\", \"issuer\": \"BOFI\" } }");
        DebtorInformations info = new DebtorInformations(LocalDate.now().plusDays(7));

        assertEquals(InitiatingPartyIdentifier.of("IE77889900", "BANK", null, "BOFI"),
                info.initiatingPartyIdentifier);
        assertTrue(validator().validate(info).isEmpty());
    }

    @Test
    public void missingOrBlankIdentifierIsAllowed() throws Exception {
        for (String party : new String[] { "{ \"name\": \"Acme\" }", "{ \"name\": \"Acme\", \"siret\": \"  \" }" }) {
            writeJson("{ \"debtor\": { \"name\": \"Acme\", \"iban\": \"GB29NWBK60161331926819\","
                    + " \"bic\": \"BNPAFRPP\" }, \"initiatingParty\": " + party + " }");
            DebtorInformations info = new DebtorInformations(LocalDate.now().plusDays(7));

            assertNull(party, info.initiatingPartyIdentifier);
            assertTrue(party, validator().validate(info).isEmpty());
        }
    }

    @Test
    public void invalidSchemeCombinationIsReported() throws Exception {
        writeJson("{ \"debtor\": { \"name\": \"Acme\", \"iban\": \"GB29NWBK60161331926819\", \"bic\": \"BNPAFRPP\" },"
                + " \"initiatingParty\": { \"name\": \"Acme\", \"schemeCode\": \"BANK\" } }");
        DebtorInformations info = new DebtorInformations(LocalDate.now().plusDays(7));

        assertFalse(validator().validate(info).isEmpty());
    }

    private void writeJson(String json) throws Exception {
        Files.write(config.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void configFileCanBeReplacedImmediatelyAfterReadingWithoutGcWorkaround() throws Exception {
        // Read the configuration (this used to leak an open FileReader handle).
        new DebtorInformations(LocalDate.now().plusDays(7));

        // Immediately perform the same atomic replace ConfigStore uses. On
        // Windows a leaked read handle would make this fail with an IOException;
        // no System.gc()/runFinalization()/sleep/retry is used here on purpose.
        Path replacement = tmp.newFile("replacement-" + System.nanoTime() + ".json").toPath();
        Files.write(replacement,
                Files.readAllBytes(config.toPath()));
        Files.move(replacement, config.toPath(), StandardCopyOption.REPLACE_EXISTING);

        assertTrue("Config file must still exist after atomic replacement", config.exists());

        // And it must still read back cleanly afterwards.
        DebtorInformations reread = new DebtorInformations(LocalDate.now().plusDays(7));
        assertEquals("Sociète Générale — Iñtërnâtiônàl", reread.name);
    }
}
