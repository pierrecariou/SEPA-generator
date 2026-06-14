package com.pcariou.view;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the bundled input templates are packaged on the classpath and
 * can be copied to a local file. UI interaction is not exercised here.
 */
public class InputTemplatesTest {

    private static final String EXPECTED_BASIC_HEADER =
            "name,IBAN,BIC,amount,end_to_end_id,information";

    private static final String EXPECTED_ADDRESS_HEADER =
            "name,IBAN,BIC,amount,end_to_end_id,information,"
                    + "street,building_number,postcode,town,country";

    @Test
    public void allTemplatesAreAvailable() {
        for (InputTemplates.Kind kind : InputTemplates.Kind.values()) {
            assertTrue(kind.label() + " should be packaged", InputTemplates.isAvailable(kind));
        }
    }

    @Test
    public void basicCsvTemplateCopiesWithExpectedHeader() throws Exception {
        Path target = Files.createTempFile("sepa-template-test", ".csv");
        try {
            InputTemplates.copyTo(InputTemplates.Kind.CSV_BASIC, target);
            assertTrue(Files.size(target) > 0);

            List<String> lines = Files.readAllLines(target);
            assertTrue("template should contain a header and an example row", lines.size() >= 2);
            assertEquals(EXPECTED_BASIC_HEADER, lines.get(0).trim());
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    public void addressCsvTemplateCopiesWithExpectedHeader() throws Exception {
        Path target = Files.createTempFile("sepa-template-test", ".csv");
        try {
            InputTemplates.copyTo(InputTemplates.Kind.CSV_ADDRESS, target);
            assertTrue(Files.size(target) > 0);

            List<String> lines = Files.readAllLines(target);
            assertTrue("template should contain a header and an example row", lines.size() >= 2);
            assertEquals(EXPECTED_ADDRESS_HEADER, lines.get(0).trim());
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    public void excelTemplatesCopyToNonEmptyZipContainers() throws Exception {
        for (InputTemplates.Kind kind : new InputTemplates.Kind[]{
                InputTemplates.Kind.EXCEL_BASIC, InputTemplates.Kind.EXCEL_ADDRESS}) {
            Path target = Files.createTempFile("sepa-template-test", ".xlsx");
            try {
                InputTemplates.copyTo(kind, target);
                assertTrue(Files.size(target) > 0);

                byte[] head = new byte[2];
                try (java.io.InputStream in = Files.newInputStream(target)) {
                    assertEquals(2, in.read(head));
                }
                // XLSX is a ZIP container; it must start with the "PK" signature.
                assertEquals('P', head[0]);
                assertEquals('K', head[1]);
            } finally {
                Files.deleteIfExists(target);
            }
        }
    }
}
