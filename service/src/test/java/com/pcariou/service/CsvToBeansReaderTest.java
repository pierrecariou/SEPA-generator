package com.pcariou.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.pcariou.model.Document;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Covers the edition-neutral {@link CsvToBeans#read(java.io.Reader)} overload:
 * it must behave identically to the file-based entry point, and the existing
 * {@link CsvToBeans#read(String)} path must keep working unchanged.
 */
public class CsvToBeansReaderTest {

    private static final String CSV_HEADER = "name,IBAN,BIC,amount,end_to_end_id,information";
    private static final String CSV_ROWS =
            "Karlson GmbH,DE89370400440532013000,DEUTDEFF,1500.00,INV-1,Invoice 1\n"
                    + "Acme Ltd,GB29NWBK60161331926819,BANKNL2A,75.10,INV-2,Supplies\n";

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
    public void readerOverloadParsesTheSameTransactionsAsTheFilePath() throws Exception {
        LocalDate date = LocalDate.now().plusDays(7);

        CsvToBeans fromReader = new CsvToBeans(date);
        Document fromReaderDoc = fromReader.read(new StringReader(CSV_HEADER + "\n" + CSV_ROWS));
        assertNotNull(fromReaderDoc);
        assertEquals("2", fromReader.getTableResult().get(0));
        assertEquals("1575.10", fromReader.getTableResult().get(1));

        File csv = tmp.newFile("input-" + System.nanoTime() + ".csv");
        Files.write(csv.toPath(), (CSV_HEADER + "\n" + CSV_ROWS).getBytes(StandardCharsets.UTF_8));

        CsvToBeans fromFile = new CsvToBeans(date);
        Document fromFileDoc = fromFile.read(csv.getAbsolutePath());
        assertNotNull(fromFileDoc);

        // Both entry points must yield the same summary (transactions + control sum + date).
        assertEquals(fromFile.getTableResult(), fromReader.getTableResult());
    }
}
