package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

/**
 * Minimal release-safety suite for the SEPA Generator Community Edition.
 *
 * Covers the core CSV -> SEPA XML pipeline ({@link CsvToBeans} + {@link BeansToXml})
 * plus a validation guard and a large-input smoke test. All inputs/outputs use
 * temporary files; the debtor configuration is redirected to a temp file via the
 * {@code sepa.config.file} system property so no user-local path is touched.
 */
public class ReleaseSafetyTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";
    private static final String VALID_IBAN = "GB29NWBK60161331926819";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;

    @Before
    public void redirectConfigToTempFile() throws Exception {
        previousConfigProperty = System.getProperty("sepa.config.file");
        File config = writeConfig(VALID_IBAN);
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

    // 1. A normal CSV input generates a SEPA XML successfully.
    @Test
    public void normalCsvGeneratesXmlSuccessfully() throws Exception {
        File csv = writeCsv("100.00", "200.00", "50.50");
        File xml = tmp.newFile("output.xml");

        generate(csv, xml, LocalDate.now().plusDays(1));

        assertTrue("Output XML should exist", xml.exists());
        assertTrue("Output XML should not be empty", xml.length() > 0);

        String content = read(xml);
        assertTrue("Output should be a pain.001 SEPA document",
                content.contains("pain.001.001.02"));
    }

    // 2. The generated XML contains the expected number of transactions.
    @Test
    public void generatedXmlContainsExpectedTransactionCount() throws Exception {
        File csv = writeCsv("100.00", "200.00", "50.50");
        File xml = tmp.newFile("count.xml");

        generate(csv, xml, LocalDate.now().plusDays(1));

        String content = read(xml);
        assertTrue("Expected 3 transactions in header",
                content.contains("<NbOfTxs>3</NbOfTxs>"));
        assertEquals("Expected 3 transaction blocks",
                3, countOccurrences(content, "<CdtTrfTxInf>"));
    }

    // 3. The generated XML contains the expected total amount.
    @Test
    public void generatedXmlContainsExpectedTotalAmount() throws Exception {
        File csv = writeCsv("100.00", "200.00", "50.50");
        File xml = tmp.newFile("total.xml");

        generate(csv, xml, LocalDate.now().plusDays(1));

        String content = read(xml);
        assertTrue("Expected control sum of 350.50",
                content.contains("<CtrlSum>350.50</CtrlSum>"));
    }

    // 3b. Realistic payment run: several distinct creditors (different IBANs/BICs,
    //     amounts and remittance information). The generated XML must contain the
    //     expected core SEPA fields for the debtor and every transaction.
    @Test
    public void realisticCsvGeneratesExpectedCoreFields() throws Exception {
        LocalDate executionDate = LocalDate.now().plusDays(7);

        String csvContent = CSV_HEADER + "\n"
                + "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice 2026-001 furniture\n"
                + "Juan Pablo Services,FR1420041010050500013M02606,BNPAFRPPXXX,980.25,INV-2026-002,Consulting January\n"
                + "Acme Supplies Ltd,GB29NWBK60161331926819,BANKNL2A,75.10,INV-2026-003,Office supplies order 4711\n";
        File csv = tmp.newFile("realistic-" + System.nanoTime() + ".csv");
        Files.write(csv.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
        File xml = tmp.newFile("realistic.xml");

        generate(csv, xml, executionDate);

        String content = read(xml);

        // Group header
        assertTrue("Expected 3 transactions", content.contains("<NbOfTxs>3</NbOfTxs>"));
        assertTrue("Expected control sum 2555.35", content.contains("<CtrlSum>2555.35</CtrlSum>"));
        assertTrue("Expected initiating party name", content.contains("<Nm>Test Party</Nm>"));

        // Debtor side (from config)
        assertTrue("Expected debtor name", content.contains("<Nm>Test Debtor</Nm>"));
        assertTrue("Expected debtor IBAN", content.contains("<IBAN>" + VALID_IBAN + "</IBAN>"));
        assertTrue("Expected debtor BIC", content.contains("<BIC>BNPAFRPP</BIC>"));
        assertTrue("Expected requested execution date",
                content.contains("<ReqdExctnDt>" + executionDate + "</ReqdExctnDt>"));

        // Creditor side, per transaction
        assertTrue(content.contains("<Nm>Karlson GmbH</Nm>"));
        assertTrue(content.contains("<IBAN>DE89370400440532013000</IBAN>"));
        assertTrue(content.contains("<BIC>DEUTDEFF</BIC>"));
        assertTrue(content.contains("<Nm>Juan Pablo Services</Nm>"));
        assertTrue(content.contains("<IBAN>FR1420041010050500013M02606</IBAN>"));
        assertTrue(content.contains("<BIC>BNPAFRPPXXX</BIC>"));
        assertTrue(content.contains("<Nm>Acme Supplies Ltd</Nm>"));
        assertTrue(content.contains("<BIC>BANKNL2A</BIC>"));

        // Amounts (normalized to 2 decimals, dot separator)
        assertTrue(content.contains(">1500.00</InstdAmt>"));
        assertTrue(content.contains(">980.25</InstdAmt>"));
        assertTrue(content.contains(">75.10</InstdAmt>"));

        // End-to-end ids and remittance information
        assertTrue(content.contains("<EndToEndId>INV-2026-001</EndToEndId>"));
        assertTrue(content.contains("<EndToEndId>INV-2026-002</EndToEndId>"));
        assertTrue(content.contains("<EndToEndId>INV-2026-003</EndToEndId>"));
        assertTrue(content.contains("<Ustrd>Invoice 2026-001 furniture</Ustrd>"));
        assertTrue(content.contains("<Ustrd>Consulting January</Ustrd>"));
        assertTrue(content.contains("<Ustrd>Office supplies order 4711</Ustrd>"));
    }

    // 4. Invalid debtor/IBAN configuration fails with a clear validation error.
    @Test
    public void invalidDebtorIbanFailsWithClearError() throws Exception {
        File config = writeConfig("NOT-A-VALID-IBAN");
        System.setProperty("sepa.config.file", config.getAbsolutePath());

        File csv = writeCsv("100.00");
        File xml = tmp.newFile("invalid.xml");

        try {
            generate(csv, xml, LocalDate.now().plusDays(1));
            fail("Generation should fail for an invalid debtor IBAN");
        } catch (Exception e) {
            String message = e.getMessage();
            assertNotNull("Validation error should carry a message", message);
            assertTrue("Error should mention the invalid IBAN, but was: " + message,
                    message.toUpperCase().contains("IBAN"));
        }
    }

    // 5. Performance smoke test: a large (~1,000 row) CSV generates without crashing
    //    and within a reasonable time. Single-threaded; only measures completion.
    @Test
    public void largeCsvGeneratesWithinReasonableTime() throws Exception {
        int rows = 1000;
        File csv = writeLargeCsv(rows);
        File xml = tmp.newFile("large.xml");

        long start = System.currentTimeMillis();
        generate(csv, xml, LocalDate.now().plusDays(1));
        long elapsedMs = System.currentTimeMillis() - start;

        assertTrue("Large output XML should exist", xml.exists());
        assertTrue("Large output XML should not be empty", xml.length() > 0);

        String content = read(xml);
        assertTrue("Large output should contain all transactions",
                content.contains("<NbOfTxs>" + rows + "</NbOfTxs>"));
        assertEquals("Large output should contain one block per transaction",
                rows, countOccurrences(content, "<CdtTrfTxInf>"));
        // 1,000 rows x 100.00 = deterministic control sum
        assertTrue("Large output should contain the expected control sum",
                content.contains("<CtrlSum>100000.00</CtrlSum>"));

        assertTrue("Large generation took too long: " + elapsedMs + " ms",
                elapsedMs < 30_000);
    }

    // 6. A blank "information" column omits RmtInf entirely rather than emitting
    //    an empty <Ustrd> (which would be schema-invalid). Non-blank rows still
    //    carry their remittance information.
    @Test
    public void blankRemittanceInformationOmitsRmtInf() throws Exception {
        String csvContent = CSV_HEADER + "\n"
                + "Creditor A," + VALID_IBAN + ",BANKNL2A,100.00,E2E-A,\n"
                + "Creditor B," + VALID_IBAN + ",BANKNL2A,200.00,E2E-B,Invoice B\n";
        File csv = tmp.newFile("remittance-" + System.nanoTime() + ".csv");
        Files.write(csv.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
        File xml = tmp.newFile("remittance.xml");

        generate(csv, xml, LocalDate.now().plusDays(1));

        String content = read(xml);
        assertTrue("Non-blank remittance should still be present",
                content.contains("<Ustrd>Invoice B</Ustrd>"));
        assertEquals("Only one transaction should carry an RmtInf/Ustrd",
                1, countOccurrences(content, "<Ustrd>"));
        assertTrue("Blank remittance must not emit an empty Ustrd",
                !content.contains("<Ustrd></Ustrd>") && !content.contains("<Ustrd/>"));
    }

    // 7. Message ids are unique per run and fit the ISO 20022 Max35Text limit.
    @Test
    public void generatedMessageIdIsUniqueAndWithinMax35() throws Exception {
        File csv = writeCsv("100.00");

        File xml1 = tmp.newFile("msgid1.xml");
        generate(csv, xml1, LocalDate.now().plusDays(1));
        String msgId1 = extractMessageId(read(xml1));

        File xml2 = tmp.newFile("msgid2.xml");
        generate(csv, xml2, LocalDate.now().plusDays(1));
        String msgId2 = extractMessageId(read(xml2));

        assertNotNull("MsgId should be present", msgId1);
        assertNotNull("MsgId should be present", msgId2);
        assertTrue("MsgId should start with the CT- prefix", msgId1.startsWith("CT-"));
        assertTrue("MsgId must fit Max35Text (<= 35 chars), was " + msgId1.length(),
                msgId1.length() <= 35);
        assertTrue("PmtInfId (MsgId-1) must fit Max35Text (<= 35 chars)",
                (msgId1.length() + 2) <= 35);
        assertTrue("Two runs must not reuse the same MsgId", !msgId1.equals(msgId2));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractMessageId(String content) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("<MsgId>(.*?)</MsgId>").matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private void generate(File csv, File xml, LocalDate date) throws Exception {
        CsvToBeans csvToBeans = new CsvToBeans(date);
        Document document = csvToBeans.read(csv.getAbsolutePath());
        new BeansToXml().write(document, xml.getAbsolutePath());
    }

    private File writeConfig(String iban) throws Exception {
        String json = "{\n"
                + "  \"debtor\": {\n"
                + "    \"name\": \"Test Debtor\",\n"
                + "    \"iban\": \"" + iban + "\",\n"
                + "    \"bic\": \"BNPAFRPP\"\n"
                + "  },\n"
                + "  \"initiatingParty\": {\n"
                + "    \"name\": \"Test Party\",\n"
                + "    \"siret\": \"12345678901234\"\n"
                + "  }\n"
                + "}\n";
        File f = tmp.newFile("config-" + System.nanoTime() + ".json");
        Files.write(f.toPath(), json.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private File writeCsv(String... amounts) throws Exception {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
        int i = 1;
        for (String amount : amounts) {
            sb.append("Creditor ").append(i).append(',')
              .append(VALID_IBAN).append(',')
              .append("BANKNL2A").append(',')
              .append(amount).append(',')
              .append("E2E").append(i).append(',')
              .append("invoice ").append(i)
              .append('\n');
            i++;
        }
        File f = tmp.newFile("input-" + System.nanoTime() + ".csv");
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private File writeLargeCsv(int rows) throws Exception {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
        for (int i = 1; i <= rows; i++) {
            sb.append("Creditor ").append(i).append(',')
              .append(VALID_IBAN).append(',')
              .append("BANKNL2A").append(',')
              .append("100.00").append(',')
              .append("E2E").append(i).append(',')
              .append("invoice ").append(i)
              .append('\n');
        }
        File f = tmp.newFile("large-" + System.nanoTime() + ".csv");
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
