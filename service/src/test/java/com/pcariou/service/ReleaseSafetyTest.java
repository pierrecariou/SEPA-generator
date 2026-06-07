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

        assertTrue("Large generation took too long: " + elapsedMs + " ms",
                elapsedMs < 30_000);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
