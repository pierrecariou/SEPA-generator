package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
 * Structural tests for the pain.001.001.09 output path ({@link Pain09Writer})
 * plus a regression guard proving the pain.001.001.02 path is unchanged.
 *
 * Same temp-file/config-redirection approach as {@link ReleaseSafetyTest}.
 */
public class Pain09GenerationTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";
    private static final String DEBTOR_IBAN = "GB29NWBK60161331926819";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;

    @Before
    public void redirectConfigToTempFile() throws Exception {
        previousConfigProperty = System.getProperty("sepa.config.file");
        String json = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"Test Debtor\",\n"
                + "    \"iban\": \"" + DEBTOR_IBAN + "\",\n"
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

    // 1. The .09 output has the expected structure and version-specific markers.
    @Test
    public void pain09OutputHasExpectedStructure() throws Exception {
        LocalDate executionDate = LocalDate.now().plusDays(7);
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice 2026-001 furniture",
                "Acme Supplies Ltd," + DEBTOR_IBAN + ",BANKNL2A,75.10,INV-2026-003,Office supplies order 4711");
        File xml = tmp.newFile("pain09.xml");

        generate(csv, xml, executionDate, PainVersion.PAIN_001_001_09);
        String content = read(xml);

        // Version markers
        assertTrue("Expected .09 namespace",
                content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.09"));
        assertTrue("Expected CstmrCdtTrfInitn root child",
                content.contains("<CstmrCdtTrfInitn>"));
        assertFalse("The .02 root child must not appear",
                content.contains("<pain.001.001.02>"));
        assertFalse("The .02-only Grpg element must not appear",
                content.contains("<Grpg>"));

        // BICFI replaces BIC
        assertTrue("Expected debtor BICFI", content.contains("<BICFI>BNPAFRPP</BICFI>"));
        assertTrue("Expected creditor BICFI", content.contains("<BICFI>DEUTDEFF</BICFI>"));
        assertTrue("Expected creditor BICFI", content.contains("<BICFI>BANKNL2A</BICFI>"));
        assertFalse("The old BIC element must not appear in .09 output",
                content.contains("<BIC>"));

        // ReqdExctnDt with Dt wrapper
        assertTrue("Expected ReqdExctnDt/Dt wrapper",
                content.replaceAll("\\s+", "").contains(
                        "<ReqdExctnDt><Dt>" + executionDate + "</Dt></ReqdExctnDt>"));

        // Group header totals
        assertTrue("Expected 2 transactions", content.contains("<NbOfTxs>2</NbOfTxs>"));
        assertTrue("Expected control sum 1575.10", content.contains("<CtrlSum>1575.10</CtrlSum>"));
        assertEquals("Expected one block per transaction",
                2, countOccurrences(content, "<CdtTrfTxInf>"));

        // SIRET as OrgId/Othr/Id (not the .02 PrtryId)
        assertTrue("Expected SIRET as Othr/Id",
                content.replaceAll("\\s+", "").contains(
                        "<OrgId><Othr><Id>12345678901234</Id></Othr></OrgId>"));
        assertFalse("The .02-only PrtryId element must not appear",
                content.contains("<PrtryId>"));

        // Core fields preserved
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
    }

    // 2. Regression: the same input through the .02 writer keeps the historical markers.
    @Test
    public void pain02OutputRemainsUnchanged() throws Exception {
        LocalDate executionDate = LocalDate.now().plusDays(7);
        File csv = writeCsv(
                "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice 2026-001 furniture");
        File xml = tmp.newFile("pain02.xml");

        generate(csv, xml, executionDate, PainVersion.PAIN_001_001_02);
        String content = read(xml);

        assertTrue("Expected .02 namespace",
                content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.02"));
        assertTrue("Expected .02 root child", content.contains("<pain.001.001.02>"));
        assertTrue("Expected .02 Grpg element", content.contains("<Grpg>MIXD</Grpg>"));
        assertTrue("Expected .02 BIC element", content.contains("<BIC>BNPAFRPP</BIC>"));
        assertFalse("BICFI must not appear in .02 output", content.contains("<BICFI>"));
        assertTrue("Expected plain ReqdExctnDt",
                content.contains("<ReqdExctnDt>" + executionDate + "</ReqdExctnDt>"));
        assertTrue("Expected .02 PrtryId for SIRET", content.contains("<PrtryId>"));
        assertTrue(content.contains("<NbOfTxs>1</NbOfTxs>"));
        assertTrue(content.contains("<CtrlSum>1500.00</CtrlSum>"));
    }

    // 3. Plumbing: version selection picks the right writer and defaults safely.
    @Test
    public void writerSelectionAndVersionCodes() {
        assertTrue(PainWriter.forVersion(PainVersion.PAIN_001_001_02) instanceof Pain02Writer);
        assertTrue(PainWriter.forVersion(PainVersion.PAIN_001_001_09) instanceof Pain09Writer);
        assertTrue("null version must fall back to .02 for compatibility",
                PainWriter.forVersion(null) instanceof Pain02Writer);

        assertEquals(PainVersion.PAIN_001_001_02, PainVersion.fromCode("02"));
        assertEquals(PainVersion.PAIN_001_001_09, PainVersion.fromCode("09"));
        assertEquals(PainVersion.PAIN_001_001_09, PainVersion.fromCode("pain.001.001.09"));
        assertEquals(null, PainVersion.fromCode("99"));
        assertEquals(null, PainVersion.fromCode(null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void generate(File csv, File xml, LocalDate date, PainVersion version) throws Exception {
        CsvToBeans csvToBeans = new CsvToBeans(date);
        Document document = csvToBeans.read(csv.getAbsolutePath());
        PainWriter.forVersion(version).write(document, xml.getAbsolutePath());
    }

    private File writeCsv(String... rows) throws Exception {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
        for (String row : rows) {
            sb.append(row).append('\n');
        }
        File f = tmp.newFile("input-" + System.nanoTime() + ".csv");
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return f;
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
