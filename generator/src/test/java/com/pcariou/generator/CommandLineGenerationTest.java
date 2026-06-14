package com.pcariou.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Regression tests for the CLI entry point ({@link Generator#runCommandLine}).
 *
 * Historically the CLI passed a {@code null} execution date into the shared
 * generation pipeline, causing a NullPointerException. The CLI now takes the
 * execution date as the third positional argument:
 *
 * <pre>input.csv output.xml 2026-07-01 [--format=02|09]</pre>
 *
 * A legacy unused 4th positional argument is still tolerated for backward
 * compatibility with old invocations.
 */
public class CommandLineGenerationTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";
    private static final String CSV_ROW =
            "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-2026-001,Invoice 2026-001 furniture";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfigProperty;
    private PrintStream previousOut;
    private ByteArrayOutputStream capturedOut;

    @Before
    public void setUp() throws Exception {
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

        previousOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, "UTF-8"));
    }

    @After
    public void tearDown() {
        System.setOut(previousOut);
        if (previousConfigProperty == null) {
            System.clearProperty("sepa.config.file");
        } else {
            System.setProperty("sepa.config.file", previousConfigProperty);
        }
    }

    // ── Valid generation ─────────────────────────────────────────────────────

    @Test
    public void validCliGenerationDefaultsToPain02() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-02.xml");
        String date = futureDate();

        int exit = Generator.runCommandLine(new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), date});

        assertEquals("Expected success, output was:\n" + output(), 0, exit);
        String content = read(xml);
        assertTrue(content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.02"));
        assertTrue("Execution date must be used",
                content.contains("<ReqdExctnDt>" + date + "</ReqdExctnDt>"));
        assertTrue(content.contains("<NbOfTxs>1</NbOfTxs>"));
    }

    @Test
    public void validCliGenerationWithFormat09() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-09.xml");
        String date = futureDate();

        int exit = Generator.runCommandLine(
                new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), date, "--format=09"});

        assertEquals("Expected success, output was:\n" + output(), 0, exit);
        String content = read(xml);
        assertTrue(content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.09"));
        assertTrue(content.contains("<CstmrCdtTrfInitn>"));
        assertTrue("Execution date must be used with the .09 Dt wrapper",
                content.replaceAll("\\s+", "").contains("<ReqdExctnDt><Dt>" + date + "</Dt></ReqdExctnDt>"));
    }

    @Test
    public void legacyFourthArgumentIsStillTolerated() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-legacy.xml");

        int exit = Generator.runCommandLine(
                new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), futureDate(), "legacy-unused"});

        assertEquals("Expected success, output was:\n" + output(), 0, exit);
        assertTrue(xml.exists());
    }

    // ── Execution date errors ────────────────────────────────────────────────

    @Test
    public void missingExecutionDateFailsWithUsage() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-missing-date.xml");

        int exit = Generator.runCommandLine(new String[]{csv.getAbsolutePath(), xml.getAbsolutePath()});

        assertEquals(1, exit);
        assertTrue("Expected usage message, got:\n" + output(),
                output().contains("execution date YYYY-MM-DD"));
        assertFalse("No XML must be produced", xml.exists());
    }

    @Test
    public void blankExecutionDateFailsClearly() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-blank-date.xml");

        int exit = Generator.runCommandLine(new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), "  "});

        assertEquals(1, exit);
        assertTrue("Expected mandatory-date message, got:\n" + output(),
                output().contains("execution date is mandatory"));
        assertFalse(xml.exists());
    }

    @Test
    public void invalidExecutionDateFormatFailsClearly() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-bad-date.xml");

        int exit = Generator.runCommandLine(
                new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), "01/07/2026"});

        assertEquals(1, exit);
        assertTrue("Expected format error mentioning the value, got:\n" + output(),
                output().contains("01/07/2026") && output().contains("YYYY-MM-DD"));
        assertFalse(xml.exists());
    }

    @Test
    public void pastExecutionDateFailsClearly() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-past-date.xml");

        int exit = Generator.runCommandLine(
                new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(),
                        LocalDate.now().minusDays(1).toString()});

        assertEquals(1, exit);
        assertTrue("Expected future-date error, got:\n" + output(),
                output().contains("future"));
    }

    // ── Format errors ────────────────────────────────────────────────────────

    @Test
    public void unknownFormatFailsClearly() throws Exception {
        File csv = writeCsv();
        File xml = outputFile("cli-bad-format.xml");

        int exit = Generator.runCommandLine(
                new String[]{csv.getAbsolutePath(), xml.getAbsolutePath(), futureDate(), "--format=99"});

        assertEquals(1, exit);
        assertTrue("Expected unknown-format message, got:\n" + output(),
                output().contains("Unknown format"));
        assertFalse(xml.exists());
    }

    // ── XLS/XLSX input (installed-app path handling) ─────────────────────────

    /**
     * Regression test for the installed-app XLSX bug: Excel input was converted
     * to a CSV named after the input base name and written into the process
     * working directory. Launched from the Windows MSI shortcut, the working
     * directory differs from the input folder (and is often not writable), so
     * the converted CSV could not be found ("... .csv (The system cannot find
     * the file specified)").
     *
     * The conversion now uses an absolute temporary CSV path. This test runs the
     * real CLI conversion with an XLSX whose folder is NOT the working directory
     * and asserts that:
     *   - generation succeeds and produces XML, and
     *   - no CSV named after the input is left in the current working directory.
     */
    @Test
    public void xlsxInputGeneratesXmlWithoutWritingCsvToWorkingDirectory() throws Exception {
        String baseName = "installed-app-input-" + System.nanoTime();
        File xlsx = writeXlsx(baseName + ".xlsx");
        File xml = outputFile("from-xlsx.xml");

        File strayInWorkingDir = new File(System.getProperty("user.dir"), baseName + ".csv");
        assertFalse("Precondition: no stray CSV should exist yet", strayInWorkingDir.exists());

        int exit = Generator.runCommandLine(
                new String[]{xlsx.getAbsolutePath(), xml.getAbsolutePath(), futureDate()});

        assertEquals("Expected success, output was:\n" + output(), 0, exit);
        assertTrue("XML output must be produced from XLSX input", xml.exists());
        String content = read(xml);
        assertTrue(content.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.02"));
        assertTrue("Expected one transaction from the single XLSX row",
                content.contains("<NbOfTxs>1</NbOfTxs>"));
        assertTrue(content.contains("<IBAN>DE89370400440532013000</IBAN>"));

        assertFalse("Conversion must not write a CSV into the working directory: "
                        + strayInWorkingDir.getAbsolutePath(),
                strayInWorkingDir.exists());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a minimal one-row XLSX workbook matching the CSV header layout. */
    private File writeXlsx(String name) throws Exception {
        String[] header = {"name", "IBAN", "BIC", "amount", "end_to_end_id", "information"};
        String[] row = {"Karlson GmbH", "DE89370400440532013000", "DEUTDEFF",
                "1500.00", "INV-2026-001", "Invoice 2026-001 furniture"};
        Workbook workbook = new Workbook();
        Worksheet sheet = workbook.getWorksheets().get(0);
        for (int col = 0; col < header.length; col++) {
            sheet.getCells().get(0, col).putValue(header[col]);
        }
        for (int col = 0; col < row.length; col++) {
            sheet.getCells().get(1, col).putValue(row[col]);
        }
        File f = new File(tmp.getRoot(), name);
        workbook.save(f.getAbsolutePath());
        return f;
    }

    private File writeCsv() throws Exception {
        File f = tmp.newFile("input-" + System.nanoTime() + ".csv");
        Files.write(f.toPath(), (CSV_HEADER + "\n" + CSV_ROW + "\n").getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /** Output path that does not exist yet, so failures can assert absence. */
    private File outputFile(String name) {
        return new File(tmp.getRoot(), System.nanoTime() + "-" + name);
    }

    private String futureDate() {
        return LocalDate.now().plusDays(7).toString();
    }

    private String output() throws Exception {
        return capturedOut.toString("UTF-8");
    }

    private String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
