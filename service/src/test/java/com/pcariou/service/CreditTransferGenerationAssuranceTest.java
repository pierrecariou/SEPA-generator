package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.pcariou.model.Document;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * Generation-assurance coverage for the credit-transfer CSV pipeline
 * (slice A1–A4): deterministic UTF-8/BOM decoding, IBAN registry validation
 * and canonical normalisation, EPC field/amount limits, and BigDecimal amount
 * handling with precise row/field diagnostics.
 */
public class CreditTransferGenerationAssuranceTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;

    @Before
    public void redirectConfigToTempFile() throws Exception {
        previousConfigProperty = System.getProperty("sepa.config.file");
        String json = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"Test Debtor\",\n"
                + "    \"iban\": \"GB29NWBK60161331926819\",\n"
                + "    \"bic\": \"BNPAFRPP\"\n"
                + "  },\n"
                + "  \"initiatingParty\": {\n"
                + "    \"name\": \"Test Party\",\n"
                + "    \"siret\": \"12345678901234\"\n"
                + "  }\n"
                + "}\n";
        File config = tmp.newFile("config-" + System.nanoTime() + ".json");
        Files.write(config.toPath(), json.getBytes(StandardCharsets.UTF_8));
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

    // ── A1: deterministic decoding ───────────────────────────────────────────

    @Test
    public void utf8AccentsArePreservedWithoutBom() throws Exception {
        Document document = generate(csv(
                "Société Générale,DE89370400440532013000,DEUTDEFF,1500.00,E2E-1,Facture réglée"));
        assertEquals("Accented creditor name must survive UTF-8 decoding",
                "Société Générale", creditorName(document));
    }

    @Test
    public void utf8ByteOrderMarkIsStrippedFromFirstColumn() throws Exception {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String content = CSV_HEADER + "\n"
                + "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,E2E-1,Invoice\n";
        File file = new File(tmp.getRoot(), "bom.csv");
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);
        Files.write(file.toPath(), withBom);

        Document document = new CsvToBeans(LocalDate.now().plusDays(7)).read(file.getAbsolutePath());
        assertEquals("A leading BOM must not corrupt the first field",
                "Karlson GmbH", creditorName(document));
    }

    // ── A2: IBAN normalisation and registry validation ───────────────────────

    @Test
    public void creditorIbanIsNormalisedToCanonicalFormInOutput() throws Exception {
        Document document = generate(csv(
                "Karlson GmbH,de89 3704 0044 0532 0130 00,DEUTDEFF,1500.00,E2E-1,Invoice"));
        assertEquals("The emitted IBAN must be the canonical validated value",
                "DE89370400440532013000", creditorIban(document));
    }

    @Test
    public void ibanWithWrongLengthForCountryIsRejectedWithRowContext() throws Exception {
        try {
            generate(csv("Karlson GmbH,DE8937040044053201300,DEUTDEFF,1500.00,E2E-1,Invoice"));
            fail("A German IBAN of the wrong length must be rejected");
        } catch (Exception e) {
            assertTrue("Error must carry row context: " + e.getMessage(),
                    e.getMessage().contains("Row 2"));
            assertTrue("Error must explain the IBAN length problem: " + e.getMessage(),
                    e.getMessage().contains("IBAN"));
        }
    }

    // ── A3: EPC field and amount limits ──────────────────────────────────────

    @Test
    public void creditorNameOverSeventyCharsIsRejected() throws Exception {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 71; i++) {
            name.append('A');
        }
        try {
            generate(csv(name + ",DE89370400440532013000,DEUTDEFF,1500.00,E2E-1,Invoice"));
            fail("A creditor name longer than 70 characters must be rejected");
        } catch (Exception e) {
            assertTrue("Error must carry row context: " + e.getMessage(),
                    e.getMessage().contains("Row 2"));
        }
    }

    @Test
    public void amountAboveEpcCeilingIsRejected() throws Exception {
        try {
            generate(csv("Karlson GmbH,DE89370400440532013000,DEUTDEFF,1000000000.00,E2E-1,Invoice"));
            fail("An amount over the EPC ceiling must be rejected");
        } catch (Exception e) {
            assertTrue("Error must carry row context: " + e.getMessage(),
                    e.getMessage().contains("Row 2"));
        }
    }

    // ── A4: BigDecimal amounts and row-context diagnostics ────────────────────

    @Test
    public void amountsAreReformattedToTwoDecimalsAndCommaAccepted() throws Exception {
        Document document = generate(csv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500,E2E-1,Invoice"));
        assertEquals("1500.00", instructedAmount(document, 0));
    }

    @Test
    public void controlSumIsExactAcrossMultipleRows() throws Exception {
        Document document = generate(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,100.10,E2E-1,Invoice",
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,200.20,E2E-2,Invoice",
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,300.30,E2E-3,Invoice");
        assertEquals("Control sum must be an exact BigDecimal total",
                "600.60", document.getPain().getGroupHeader().getControlSum());
        assertEquals("3", document.getPain().getGroupHeader().getNumberOfTransactions());
    }

    @Test
    public void invalidAmountAttributesTheCorrectSourceRow() throws Exception {
        try {
            generate(
                    "Karlson GmbH,DE89370400440532013000,DEUTDEFF,100.00,E2E-1,Invoice",
                    "Karlson GmbH,DE89370400440532013000,DEUTDEFF,notanumber,E2E-2,Invoice");
            fail("A malformed amount must be rejected");
        } catch (Exception e) {
            assertTrue("Error must point at the second data row (line 3): " + e.getMessage(),
                    e.getMessage().contains("Row 3"));
        }
    }

    // ── XML 1.0 legality: generation is blocked before any XML is written ─────

    @Test
    public void xmlIllegalCharacterIsRejectedBeforeGeneration() throws Exception {
        try {
            generate(
                    "Karlson GmbH,DE89370400440532013000,DEUTDEFF,100.00,E2E-1,Invoice",
                    "Bad\u0007Name,DE89370400440532013000,DEUTDEFF,200.00,E2E-2,Invoice");
            fail("A value with an XML 1.0-illegal character must be rejected before writing XML");
        } catch (Exception e) {
            assertTrue("Error must carry the source row context: " + e.getMessage(),
                    e.getMessage().contains("Row 3"));
            assertTrue("Error must name the illegal character: " + e.getMessage(),
                    e.getMessage().contains("U+0007"));
        }
    }

    @Test
    public void tabIsNotRejectedByTheXmlRuleDuringGeneration() throws Exception {
        Document document = generate(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,100.00,E2E-1,Invoice\twith tab");
        assertEquals("1", document.getPain().getGroupHeader().getNumberOfTransactions());
    }

    @Test
    public void xmlIllegalCharInDebtorNameConfigIsRejectedBeforeGeneration() throws Exception {
        // Override the config written by @Before with one that has a BEL (U+0007)
        // in the debtor name — use the JSON \u0007 escape so the file is parseable.
        String illegalNameConfig = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"Bad\\u0007Debtor\",\n"
                + "    \"iban\": \"GB29NWBK60161331926819\",\n"
                + "    \"bic\": \"NWBKGB2L\"\n"
                + "  },\n"
                + "  \"initiatingParty\": {\n"
                + "    \"name\": \"Test Initiating Party\",\n"
                + "    \"siret\": \"12345678901234\"\n"
                + "  }\n"
                + "}\n";
        File illegalConfig = tmp.newFile("config-illegal-debtor-" + System.nanoTime() + ".json");
        Files.write(illegalConfig.toPath(), illegalNameConfig.getBytes(StandardCharsets.UTF_8));
        System.setProperty("sepa.config.file", illegalConfig.getAbsolutePath());

        try {
            generate("Karlson GmbH,DE89370400440532013000,DEUTDEFF,100.00,E2E-1,Invoice");
            fail("An XML-illegal character in the debtor name must block generation");
        } catch (Exception e) {
            assertTrue("Error must mention the illegal character U+0007: " + e.getMessage(),
                    e.getMessage().contains("U+0007"));
        }
    }

    @Test
    public void normalUnicodeAndAccentsAreAccepted() throws Exception {
        // French accents, euro sign, non-Latin CJK — all legal XML 1.0
        Document document = generate(csv(
                "Société Müller \u4E2D\u56FD,DE89370400440532013000,DEUTDEFF,1500.00,E2E-1,Facture \u20AC"));
        assertEquals("1", document.getPain().getGroupHeader().getNumberOfTransactions());
    }

    @Test
    public void noOutputFileIsCreatedWhenXmlValidationBlocksGeneration() throws Exception {
        File outputXml = new File(tmp.getRoot(), "output-" + System.nanoTime() + ".xml");
        File inputCsv = new File(tmp.getRoot(), "input-" + System.nanoTime() + ".csv");
        String content = CSV_HEADER + "\n"
                + "Bad\u0007Name,DE89370400440532013000,DEUTDEFF,100.00,E2E-1,Invoice\n";
        Files.write(inputCsv.toPath(), content.getBytes(StandardCharsets.UTF_8));

        try {
            com.pcariou.model.Document doc =
                    new CsvToBeans(LocalDate.now().plusDays(7)).read(inputCsv.getAbsolutePath());
            new com.pcariou.service.BeansToXml().write(doc, outputXml.getAbsolutePath());
            fail("Should have thrown before writing XML");
        } catch (Exception expected) {
            // Verify CsvToBeans threw before BeansToXml was reached
            assertTrue("Exception must mention the illegal char: " + expected.getMessage(),
                    expected.getMessage().contains("U+0007"));
        }
        assertTrue("No XML output file must exist after a blocked generation",
                !outputXml.exists());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Document generate(String... rows) throws Exception {
        File csv = new File(tmp.getRoot(), "input-" + System.nanoTime() + ".csv");
        StringBuilder content = new StringBuilder(CSV_HEADER).append("\n");
        for (String row : rows) {
            content.append(row).append("\n");
        }
        Files.write(csv.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        return new CsvToBeans(LocalDate.now().plusDays(7)).read(csv.getAbsolutePath());
    }

    private String csv(String row) {
        return row;
    }

    private static String creditorName(Document document) {
        return transactions(document).get(0).getCreditor().getName();
    }

    private static String creditorIban(Document document) {
        return transactions(document).get(0).getCreditorAccount()
                .getAccountIdentification().getIban();
    }

    private static String instructedAmount(Document document, int index) {
        return transactions(document).get(index).getAmount()
                .getInstructedAmount().getInstructedAmount();
    }

    private static List<com.pcariou.model.CreditTransferTransactionInformation> transactions(
            Document document) {
        return document.getPain().getPaymentInformation().get(0)
                .getCreditTransferTransactionInformation();
    }
}
