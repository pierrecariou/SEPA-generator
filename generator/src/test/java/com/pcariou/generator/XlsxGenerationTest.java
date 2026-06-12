package com.pcariou.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.pcariou.model.Document;
import com.pcariou.service.BeansToXml;
import com.pcariou.service.CsvToBeans;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Release-safety test for the XLSX -> CSV -> SEPA XML pipeline.
 *
 * The application converts Excel input to CSV with Aspose Cells before running
 * the shared CSV pipeline ({@link CsvToBeans} + {@link BeansToXml}). This test
 * builds a realistic XLSX workbook programmatically (deterministic, no binary
 * fixture), converts it the same way and verifies the generated SEPA XML.
 *
 * Aspose Cells runs in evaluation mode in tests and may append a watermark
 * line/sheet; the conversion helper strips any such line, mirroring the
 * application's own cleanup of the converted CSV.
 */
public class XlsxGenerationTest {

    private static final String[] CSV_HEADER =
            {"name", "IBAN", "BIC", "amount", "end_to_end_id", "information"};

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

    @Test
    public void realisticXlsxGeneratesExpectedSepaXml() throws Exception {
        String[][] rows = {
                {"Karlson GmbH", "DE89370400440532013000", "DEUTDEFF", "1500.00", "INV-2026-001", "Invoice 2026-001 furniture"},
                {"Juan Pablo Services", "FR1420041010050500013M02606", "BNPAFRPPXXX", "980.25", "INV-2026-002", "Consulting January"},
                {"Acme Supplies Ltd", "GB29NWBK60161331926819", "BANKNL2A", "75.10", "INV-2026-003", "Office supplies order 4711"},
        };
        File xlsx = writeXlsx("payments.xlsx", rows);
        File xml = tmp.newFile("from-xlsx.xml");

        File csv = convertToCsv(xlsx);
        generate(csv, xml, LocalDate.now().plusDays(7));

        String content = read(xml);
        assertTrue("Output should be a pain.001 SEPA document",
                content.contains("pain.001.001.02"));
        assertTrue("Expected 3 transactions", content.contains("<NbOfTxs>3</NbOfTxs>"));
        assertEquals("Expected 3 transaction blocks",
                3, countOccurrences(content, "<CdtTrfTxInf>"));
        assertTrue("Expected control sum 2555.35", content.contains("<CtrlSum>2555.35</CtrlSum>"));

        assertTrue(content.contains("<Nm>Karlson GmbH</Nm>"));
        assertTrue(content.contains("<IBAN>DE89370400440532013000</IBAN>"));
        assertTrue(content.contains("<BIC>DEUTDEFF</BIC>"));
        assertTrue(content.contains(">1500.00</InstdAmt>"));
        assertTrue(content.contains("<EndToEndId>INV-2026-002</EndToEndId>"));
        assertTrue(content.contains("<Ustrd>Office supplies order 4711</Ustrd>"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private File writeXlsx(String name, String[][] rows) throws Exception {
        Workbook workbook = new Workbook();
        Worksheet sheet = workbook.getWorksheets().get(0);
        for (int col = 0; col < CSV_HEADER.length; col++) {
            sheet.getCells().get(0, col).putValue(CSV_HEADER[col]);
        }
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length; col++) {
                sheet.getCells().get(row + 1, col).putValue(rows[row][col]);
            }
        }
        File f = new File(tmp.getRoot(), name);
        workbook.save(f.getAbsolutePath());
        return f;
    }

    /**
     * Converts an XLSX file to CSV the same way the application does (Aspose
     * Workbook save), then strips any evaluation-watermark line appended by
     * the unlicensed Aspose build, mirroring the app's own cleanup step.
     */
    private File convertToCsv(File xlsx) throws Exception {
        File rawCsv = new File(tmp.getRoot(), "converted-" + System.nanoTime() + ".csv");
        Workbook workbook = new Workbook(xlsx.getAbsolutePath());
        workbook.save(rawCsv.getAbsolutePath());

        List<String> kept = new ArrayList<String>();
        for (String line : Files.readAllLines(rawCsv.toPath(), StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()
                    || line.contains("Evaluation")
                    || line.contains("Aspose")) {
                continue;
            }
            kept.add(line);
        }
        File cleanCsv = new File(tmp.getRoot(), "clean-" + System.nanoTime() + ".csv");
        StringBuilder sb = new StringBuilder();
        for (String line : kept) {
            sb.append(line).append('\n');
        }
        Files.write(cleanCsv.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return cleanCsv;
    }

    private void generate(File csv, File xml, LocalDate date) throws Exception {
        CsvToBeans csvToBeans = new CsvToBeans(date);
        Document document = csvToBeans.read(csv.getAbsolutePath());
        new BeansToXml().write(document, xml.getAbsolutePath());
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
