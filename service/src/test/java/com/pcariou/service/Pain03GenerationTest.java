package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.pcariou.model.Document;
import com.pcariou.model.PainVersion;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Structural tests for the pain.001.001.03 output path ({@link Pain03Writer}),
 * the legacy format offered for banks and upload channels that still require
 * it, plus regression guards proving the .02 and .09 paths are unchanged.
 *
 * <p>Mirrors {@link Pain09GenerationTest} (same temp-file/config redirection).
 */
public class Pain03GenerationTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";
    private static final String ADDRESS_CSV_HEADER =
            CSV_HEADER + ",street,building_number,postcode,town,country";
    private static final String DEBTOR_IBAN = "GB29NWBK60161331926819";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;

    @Before
    public void redirectConfigToTempFile() throws Exception {
        previousConfigProperty = System.getProperty("sepa.config.file");
        writeConfig(false);
    }

    @After
    public void restoreConfigProperty() {
        if (previousConfigProperty == null) {
            System.clearProperty("sepa.config.file");
        } else {
            System.setProperty("sepa.config.file", previousConfigProperty);
        }
    }

    // 1. The .03 output has the expected structure and version-specific markers.
    @Test
    public void pain03OutputHasExpectedStructure() throws Exception {
        LocalDate executionDate = LocalDate.now().plusDays(7);
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice 2026-001 furniture",
                "Acme Supplies Ltd," + DEBTOR_IBAN + ",BANKNL2A,75.10,INV-2026-003,Office supplies order 4711");
        File xml = tmp.newFile("pain03.xml");

        generate(csv, xml, executionDate, PainVersion.PAIN_001_001_03);
        String content = read(xml);
        String compact = content.replaceAll("\\s+", "");

        assertTrue("Expected .03 namespace",
                content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"));
        assertTrue("Expected CstmrCdtTrfInitn root child", content.contains("<CstmrCdtTrfInitn>"));
        assertFalse("The .02 root child must not appear", content.contains("<pain.001.001.02>"));
        assertFalse("The .02-only Grpg element must not appear", content.contains("<Grpg>"));
        assertFalse("No PstlAdr expected when no address fields are provided",
                content.contains("<PstlAdr>"));

        // .03 uses BIC, not the .09 BICFI
        assertTrue("Expected debtor BIC", content.contains("<BIC>BNPAFRPP</BIC>"));
        assertTrue("Expected creditor BIC", content.contains("<BIC>DEUTDEFF</BIC>"));
        assertTrue("Expected creditor BIC", content.contains("<BIC>BANKNL2A</BIC>"));
        assertFalse("BICFI must not appear in .03 output", content.contains("<BICFI>"));

        // .03 carries the execution date directly (no Dt wrapper)
        assertTrue("Expected plain ReqdExctnDt",
                content.contains("<ReqdExctnDt>" + executionDate + "</ReqdExctnDt>"));
        assertFalse("The .09 Dt wrapper must not appear", compact.contains("<ReqdExctnDt><Dt>"));

        // Totals: group header and payment block
        assertEquals("Expected group-header and payment-block NbOfTxs",
                2, countOccurrences(content, "<NbOfTxs>2</NbOfTxs>"));
        assertEquals("Expected group-header and payment-block CtrlSum",
                2, countOccurrences(content, "<CtrlSum>1575.10</CtrlSum>"));
        assertEquals("Expected one block per transaction",
                2, countOccurrences(content, "<CdtTrfTxInf>"));

        // SIRET as OrgId/Othr/Id (not the .02 PrtryId)
        assertTrue("Expected SIRET as Othr/Id",
                compact.contains("<OrgId><Othr><Id>12345678901234</Id></Othr></OrgId>"));
        assertFalse("The .02-only PrtryId element must not appear", content.contains("<PrtryId>"));

        // Core SEPA content
        assertTrue(content.contains("<Nm>Test Party</Nm>"));
        assertTrue(content.contains("<Nm>Test Debtor</Nm>"));
        assertTrue(content.contains("<IBAN>" + DEBTOR_IBAN + "</IBAN>"));
        assertTrue(content.contains("<Nm>Karlson GmbH</Nm>"));
        assertTrue(content.contains("<IBAN>DE89370400440532013000</IBAN>"));
        assertTrue(content.contains("Ccy=\"EUR\">1500.00</InstdAmt>"));
        assertTrue(content.contains("Ccy=\"EUR\">75.10</InstdAmt>"));
        assertTrue(content.contains("<EndToEndId>INV-2026-001</EndToEndId>"));
        assertTrue(content.contains("<EndToEndId>INV-2026-003</EndToEndId>"));
        assertTrue(content.contains("<Ustrd>Invoice 2026-001 furniture</Ustrd>"));
        assertTrue(content.contains("<Ustrd>Office supplies order 4711</Ustrd>"));
        assertTrue(content.contains("<PmtMtd>TRF</PmtMtd>"));
        assertTrue(content.contains("<Cd>SEPA</Cd>"));
        assertTrue("Expected ChrgBr SLEV on the payment block",
                content.contains("<ChrgBr>SLEV</ChrgBr>"));
    }

    // 2. Element order inside the payment block follows the .03 schema sequence.
    @Test
    public void pain03PaymentBlockFollowsSchemaSequence() throws Exception {
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice");
        File xml = tmp.newFile("pain03-order.xml");

        generate(csv, xml, LocalDate.now().plusDays(7), PainVersion.PAIN_001_001_03);
        String compact = read(xml).replaceAll("\\s+", "");

        assertTrue("PmtInfId/PmtMtd/NbOfTxs/CtrlSum/PmtTpInf order expected",
                compact.contains("<PmtMtd>TRF</PmtMtd><NbOfTxs>1</NbOfTxs>"
                        + "<CtrlSum>1500.00</CtrlSum><PmtTpInf>"));
        assertTrue("ChrgBr must sit between DbtrAgt and CdtTrfTxInf",
                compact.contains("</DbtrAgt><ChrgBr>SLEV</ChrgBr><CdtTrfTxInf>"));
        assertTrue("RmtInf must be the last transaction element",
                compact.contains("</CdtrAcct><RmtInf><Ustrd>Invoice</Ustrd></RmtInf></CdtTrfTxInf>"));
    }

    // 3. Structured addresses are supported by .03 and mapped in schema order.
    @Test
    public void pain03EmitsStructuredAddresses() throws Exception {
        writeConfig(true);
        File csv = writeCsvWithHeader(ADDRESS_CSV_HEADER,
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice,Hauptstrasse,5,10115,Berlin,de",
                "Acme Supplies Ltd," + DEBTOR_IBAN + ",BANKNL2A,75.10,INV-2026-003,Supplies,,,,,");
        File xml = tmp.newFile("pain03-addr.xml");

        generate(csv, xml, LocalDate.now().plusDays(7), PainVersion.PAIN_001_001_03);
        String compact = read(xml).replaceAll("\\s+", "");

        assertTrue("Expected debtor PstlAdr",
                compact.contains("<Dbtr><Nm>TestDebtor</Nm><PstlAdr>"
                        + "<StrtNm>RuedelaPaix</StrtNm><BldgNb>10</BldgNb>"
                        + "<PstCd>75002</PstCd><TwnNm>Paris</TwnNm><Ctry>FR</Ctry>"
                        + "</PstlAdr></Dbtr>"));
        assertTrue("Expected creditor PstlAdr with country upper-cased",
                compact.contains("<Cdtr><Nm>KarlsonGmbH</Nm><PstlAdr>"
                        + "<StrtNm>Hauptstrasse</StrtNm><BldgNb>5</BldgNb>"
                        + "<PstCd>10115</PstCd><TwnNm>Berlin</TwnNm><Ctry>DE</Ctry>"
                        + "</PstlAdr></Cdtr>"));
        assertFalse("Creditor without address columns must have no PstlAdr",
                compact.contains("<Cdtr><Nm>AcmeSuppliesLtd</Nm><PstlAdr>"));
    }

    // 4. Accents survive and no BOM is written.
    @Test
    public void pain03PreservesUtf8AndWritesNoBom() throws Exception {
        File csv = writeCsv(
                "Café Münster & Fils,DE89370400440532013000,DEUTDEFF,1500.00,INV-É-01,Réf. <facture> 2026");
        File xml = tmp.newFile("pain03-utf8.xml");

        generate(csv, xml, LocalDate.now().plusDays(7), PainVersion.PAIN_001_001_03);

        byte[] bytes = Files.readAllBytes(xml.toPath());
        assertFalse("Output must not start with a UTF-8 BOM",
                bytes.length > 2 && (bytes[0] & 0xFF) == 0xEF
                        && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("<Nm>Café Münster &amp; Fils</Nm>"));
        assertTrue(content.contains("<Ustrd>Réf. &lt;facture&gt; 2026</Ustrd>"));
    }

    // 5. Invalid rows keep the existing actionable diagnostics for .03 too.
    @Test
    public void invalidRowIsRejectedWithRowContext() throws Exception {
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013001,DEUTDEFF,1500.00,INV-2026-001,Invoice");
        try {
            generate(csv, tmp.newFile("pain03-invalid.xml"),
                    LocalDate.now().plusDays(7), PainVersion.PAIN_001_001_03);
            fail("Expected validation failure for an invalid creditor IBAN");
        } catch (Exception e) {
            assertTrue("Expected row context: " + e.getMessage(), e.getMessage().contains("Row 2"));
            assertTrue("Expected IBAN diagnostic: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("iban"));
        }
    }

    // 6. Version plumbing: writer selection, codes and persisted-value compatibility.
    @Test
    public void writerSelectionAndVersionCodes() {
        assertTrue(PainWriter.forVersion(PainVersion.PAIN_001_001_03) instanceof Pain03Writer);
        assertTrue(PainWriter.forVersion(PainVersion.PAIN_001_001_02) instanceof Pain02Writer);
        assertTrue(PainWriter.forVersion(PainVersion.PAIN_001_001_09) instanceof Pain09Writer);

        assertEquals(PainVersion.PAIN_001_001_03, PainVersion.fromCode("03"));
        assertEquals(PainVersion.PAIN_001_001_03, PainVersion.fromCode("pain.001.001.03"));
        assertEquals("pain.001.001.03", PainVersion.PAIN_001_001_03.getSchemaId());

        // Previously persisted selections must keep resolving to the same versions.
        assertEquals(PainVersion.PAIN_001_001_02, PainVersion.fromCode("02"));
        assertEquals(PainVersion.PAIN_001_001_09, PainVersion.fromCode("09"));
        assertEquals(null, PainVersion.fromCode("99"));
    }

    // 7. Regression: .02 and .09 output is unaffected by the new version.
    @Test
    public void otherVersionsRemainUnchanged() throws Exception {
        LocalDate executionDate = LocalDate.now().plusDays(7);
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice");

        File xml02 = tmp.newFile("regression02.xml");
        generate(csv, xml02, executionDate, PainVersion.PAIN_001_001_02);
        String content02 = read(xml02);
        assertTrue(content02.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.02"));
        assertTrue(content02.contains("<pain.001.001.02>"));
        assertTrue(content02.contains("<Grpg>MIXD</Grpg>"));

        File xml09 = tmp.newFile("regression09.xml");
        generate(csv, xml09, executionDate, PainVersion.PAIN_001_001_09);
        String content09 = read(xml09);
        assertTrue(content09.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.09"));
        assertTrue(content09.contains("<BICFI>DEUTDEFF</BICFI>"));
        assertTrue(content09.replaceAll("\\s+", "").contains("<ReqdExctnDt><Dt>"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void generate(File csv, File xml, LocalDate date, PainVersion version) throws Exception {
        CsvToBeans csvToBeans = new CsvToBeans(date);
        Document document = csvToBeans.read(csv.getAbsolutePath());
        PainWriter.forVersion(version).write(document, xml.getAbsolutePath());
    }

    private File writeCsv(String... rows) throws Exception {
        return writeCsvWithHeader(CSV_HEADER, rows);
    }

    private File writeCsvWithHeader(String header, String... rows) throws Exception {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (String row : rows) {
            sb.append(row).append('\n');
        }
        File f = tmp.newFile("input-" + System.nanoTime() + ".csv");
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private void writeConfig(boolean withDebtorAddress) throws Exception {
        String address = withDebtorAddress
                ? "    ,\"address\": {\n"
                        + "      \"street\": \"Rue de la Paix\",\n"
                        + "      \"buildingNumber\": \"10\",\n"
                        + "      \"postcode\": \"75002\",\n"
                        + "      \"town\": \"Paris\",\n"
                        + "      \"country\": \"fr\"\n"
                        + "    }\n"
                : "";
        String json = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"Test Debtor\",\n"
                + "    \"iban\": \"" + DEBTOR_IBAN + "\",\n"
                + "    \"bic\": \"BNPAFRPP\"\n"
                + address
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

    private String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
