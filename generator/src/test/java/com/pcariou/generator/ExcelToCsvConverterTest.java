package com.pcariou.generator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Verifies that a failing Excel conversion is surfaced as a thrown exception
 * rather than terminating the JVM (the previous helper called
 * {@code System.exit(1)} on I/O failure).
 *
 * <p>The fact that this test process survives and JUnit reports the assertions
 * is itself proof that no {@code System.exit} was triggered.
 */
public class ExcelToCsvConverterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void invalidWorkbookThrowsInsteadOfExitingJvm() throws Exception {
        File bogus = tmp.newFile("garbage.xlsx");
        Files.write(bogus.toPath(), "this is not a real workbook".getBytes(StandardCharsets.UTF_8));

        try {
            ExcelToCsvConverter.convert(bogus.getAbsolutePath(), "sepa-generator-test-");
            fail("Expected conversion of an invalid workbook to throw");
        } catch (Exception expected) {
            // Surfaced as an error the caller can handle; JVM stays alive.
            assertTrue(true);
        }
    }

    @Test
    public void missingFileThrowsInsteadOfExitingJvm() {
        try {
            ExcelToCsvConverter.convert(
                    tmp.getRoot().getAbsolutePath() + File.separator + "does-not-exist.xlsx",
                    "sepa-generator-test-");
            fail("Expected conversion of a missing file to throw");
        } catch (Exception expected) {
            assertNotNull(expected);
        }
    }

    /**
     * A cell that itself contains a comma must be quoted (MINIMUM quoting) so it
     * cannot split into extra columns when the CSV is parsed. This exercises the
     * hardened deterministic CSV export options.
     */
    @Test
    public void cellContainingCommaIsQuotedAndNotSplit() throws Exception {
        Workbook workbook = new Workbook();
        Worksheet sheet = workbook.getWorksheets().get(0);
        sheet.getCells().get(0, 0).putValue("name");
        sheet.getCells().get(0, 1).putValue("information");
        sheet.getCells().get(1, 0).putValue("Karlson GmbH");
        sheet.getCells().get(1, 1).putValue("Invoice, urgent, paid");
        File xlsx = tmp.newFile("commas.xlsx");
        workbook.save(xlsx.getAbsolutePath());

        java.nio.file.Path csv = ExcelToCsvConverter.convert(xlsx.getAbsolutePath(), "sepa-generator-test-");
        try {
            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            String dataRow = null;
            for (String line : lines) {
                if (line.contains("Karlson GmbH")) {
                    dataRow = line;
                    break;
                }
            }
            assertNotNull("Data row should be present", dataRow);
            assertTrue("A comma-bearing cell must be quoted so it stays one column: " + dataRow,
                    dataRow.contains("\"Invoice, urgent, paid\""));
        } finally {
            Files.deleteIfExists(csv);
        }
    }
}
