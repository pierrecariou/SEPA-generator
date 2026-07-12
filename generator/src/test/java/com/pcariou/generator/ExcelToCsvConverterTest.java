package com.pcariou.generator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
}
