package com.pcariou.generator;

import com.aspose.cells.Encoding;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.TxtSaveOptions;
import com.aspose.cells.TxtValueQuoteType;
import com.aspose.cells.Workbook;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts an Excel workbook (.xls/.xlsx) to a CSV file at an absolute
 * temporary path, independent of the process working directory. This is what
 * makes Excel input work when the app is launched from the installed Windows
 * shortcut (whose working directory is not the input file's folder and may not
 * be writable).
 *
 * <p>Used by the Community generation flow ({@code Generator}). It strips the
 * trailing Aspose evaluation footer appended to saved CSV files. The caller
 * owns the returned file and must delete it once it is done reading from it.
 *
 * <p>The CSV is written with explicit, deterministic options rather than the
 * library defaults, so a spreadsheet cannot be silently corrupted on its way to
 * the parser:
 * <ul>
 *   <li>a fixed comma field separator (never a locale-dependent one), matching
 *       the comma-delimited format the CSV parser expects;</li>
 *   <li>UTF-8 without a byte-order mark, so accented names and other non-ASCII
 *       text survive and decode consistently with {@code CsvSourceReader};</li>
 *   <li>minimum quoting, so a cell that itself contains a comma, quote or line
 *       break is quoted and cannot split into extra columns;</li>
 *   <li>no trimming of leading blank rows/columns, so a value is never shifted
 *       into a neighbouring column and mis-bound.</li>
 * </ul>
 * The cell text itself is Aspose's displayed value; this converter never
 * re-interprets an ambiguous cell (for example, it does not turn a
 * thousands-separated or scientific-notation display into a number). Such a
 * value simply reaches the normal validation, which rejects it with row and
 * field context rather than generating a wrong amount.
 */
public final class ExcelToCsvConverter {

    private ExcelToCsvConverter() {
    }

    /**
     * Converts {@code inputFile} to a temporary CSV whose name starts with
     * {@code tempFilePrefix}. Never terminates the JVM: any failure is thrown
     * so the caller can decide how to handle it.
     */
    public static Path convert(String inputFile, String tempFilePrefix) throws Exception {
        Path tempCsv = Files.createTempFile(tempFilePrefix, ".csv");
        Workbook workbook = new Workbook(inputFile);
        workbook.save(tempCsv.toAbsolutePath().toString(), csvSaveOptions());
        eraseEvaluationFooter(tempCsv);
        return tempCsv;
    }

    /** Deterministic CSV export options (see the class comment). */
    private static TxtSaveOptions csvSaveOptions() {
        TxtSaveOptions options = new TxtSaveOptions(SaveFormat.CSV);
        options.setSeparator(',');
        options.setEncoding(Encoding.getUTF8NoBOM());
        options.setQuoteType(TxtValueQuoteType.MINIMUM);
        options.setTrimLeadingBlankRowAndColumn(false);
        return options;
    }

    /** Removes the trailing Aspose evaluation line appended to saved CSV files. */
    private static void eraseEvaluationFooter(Path csv) throws Exception {
        try (RandomAccessFile f = new RandomAccessFile(csv.toFile(), "rw")) {
            long length = f.length() - 1;
            byte b;
            do {
                length -= 1;
                if (length < 0) {
                    return;
                }
                f.seek(length);
                b = f.readByte();
            } while (b != 10);
            f.setLength(length + 1);
        }
    }
}
