package com.pcariou.generator;

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
        workbook.save(tempCsv.toAbsolutePath().toString());
        eraseEvaluationFooter(tempCsv);
        return tempCsv;
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
