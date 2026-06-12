package com.pcariou.generator;

import com.pcariou.model.*;
import com.pcariou.service.*;
import com.pcariou.view.*;

import com.pcariou.view.main.MainFrame;
import org.apache.commons.io.FilenameUtils;

import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.Arrays;

import com.aspose.cells.*;

public class Generator implements IGenerator
{
    private static final String[] VALID_EXTENSIONS = {"xls", "xlsx", "csv"};
    private static final String[] EXCEL_EXTENSIONS = {"xls", "xlsx"};

    private MainFrame view;

    public Generator()
    {
    }

    public void setView(MainFrame view)
    {
        this.view = view;
    }

    public void generate(String inputFile, String outputFile, LocalDate date, PainVersion version)
    {
        if (!argumentsAreValid(inputFile, outputFile, date))
            return;
        if (isExcelFile(inputFile))
            inputFile = convertToCsv(inputFile);
        transformCsvToXml(inputFile, outputFile, date, version);
    }

    private void transformCsvToXml(String inputFile, String outputFile, LocalDate date, PainVersion version)
    {
        try {
            CsvToBeans csvToBeans = new CsvToBeans(date);
            Document document = csvToBeans.read(inputFile);
            PainWriter.forVersion(version).write(document, outputFile);
            view.showSuccessMessage(outputFile, " generated successfully.");
            view.showTableResult(csvToBeans.getTableResult());
        } catch (Exception e) {
            view.showErrorMessage(e.getMessage());
        }
    }

    private boolean argumentsAreValid(String inputFile, String outputFile, LocalDate date)
    {
        if (inputFile == null || inputFile.isEmpty())
        {
            view.showErrorMessage("Please select an input file.");
            return false;
        }
        if (outputFile == null || outputFile.isEmpty())
        {
            view.showErrorMessage("Please select an output file.");
            return false;
        }
        if (!(Arrays.asList(VALID_EXTENSIONS).contains(FilenameUtils.getExtension(inputFile))))
        {
            view.showErrorMessage("Please select a valid input file.");
            return false;
        }
        if (!FilenameUtils.getExtension(outputFile).equals("xml"))
        {
            view.showErrorMessage("Please select a valid output file.");
            return false;
        }
        if (date == null)
        {
            view.showErrorMessage("Please select a valid date.");
            return false;
        }
        return true;
    }

    private Boolean isExcelFile(String inputFile)
    {
        return Arrays.asList(EXCEL_EXTENSIONS).contains(FilenameUtils.getExtension(inputFile));
    }

    private String convertToCsv(String inputFile)
    {
        try {
            Workbook workbook = new Workbook(inputFile);
            inputFile = FilenameUtils.getBaseName(inputFile) + ".csv";
            workbook.save(FilenameUtils.getBaseName(inputFile) + ".csv");
            eraseNoLicenseMessage(inputFile);
        } catch (Exception e) {
            view.showErrorMessage(e.getMessage());
        }
        return inputFile;
    }

    private static void eraseNoLicenseMessage(String inputFile) 
    {
        Byte b;

        try {
            RandomAccessFile f = new RandomAccessFile(inputFile, "rw");
            long length = f.length() - 1;
            do {                     
                length -= 1;
                f.seek(length);
                b = f.readByte();
            } while(b != 10);
            f.setLength(length+1);
            f.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static final String CLI_USAGE =
            "Usage: java -jar generator.jar <input file> <output file> <execution date YYYY-MM-DD> [--format=02|09]";

    public static void fromCommandLine(String[] args)
    {
        int exitCode = runCommandLine(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Runs the CLI generation flow and returns an exit code (0 on success).
     * Separated from {@link #fromCommandLine} so it can be tested without
     * killing the JVM.
     */
    static int runCommandLine(String[] args)
    {
        // Optional --format=02|09 argument (anywhere); defaults to pain.001.001.02.
        PainVersion version = PainVersion.PAIN_001_001_02;
        java.util.List<String> positional = new java.util.ArrayList<>();
        for (String arg : args) {
            if (arg != null && arg.startsWith("--format=")) {
                PainVersion parsed = PainVersion.fromCode(arg.substring("--format=".length()));
                if (parsed == null) {
                    System.out.println("Unknown format. Supported values: --format=02 (pain.001.001.02), --format=09 (pain.001.001.09)");
                    return 1;
                }
                version = parsed;
            } else {
                positional.add(arg);
            }
        }

        // 3 positional arguments expected; a legacy unused 4th argument is
        // still tolerated for backward compatibility with old invocations.
        if (positional.size() < 3 || positional.size() > 4) {
            System.out.println(CLI_USAGE);
            return 1;
        }

        String inputFilename = positional.get(0);
        String outputFilename = positional.get(1);
        String rawDate = positional.get(2);

        if (inputFilename.equals(outputFilename)) {
            System.out.println("Input and output files must be different");
            return 1;
        }
        if (!outputFilename.endsWith(".xml")) {
            System.out.println("Output file must be an XML file");
            return 1;
        }

        LocalDate executionDate = parseExecutionDate(rawDate);
        if (executionDate == null) {
            return 1;
        }

        if (inputFilename.endsWith(".xls") || inputFilename.endsWith(".xlsx")) {
            try {
                Workbook workbook = new Workbook(inputFilename);
                inputFilename = FilenameUtils.getBaseName(inputFilename) + ".csv";
                workbook.save(FilenameUtils.getBaseName(inputFilename) + ".csv");
                eraseNoLicenseMessage(inputFilename);
            } catch (Exception e) {
                System.out.println("Could not convert the Excel input file: " + e.getMessage());
                return 1;
            }
        } else if (!inputFilename.endsWith(".csv")) {
            System.out.println("Input file must be a CSV or XLS/XLSX file");
            return 1;
        }

        try {
            Document document = new CsvToBeans(executionDate).read(inputFilename);
            PainWriter.forVersion(version).write(document, outputFilename);
            System.out.println(outputFilename + " generated successfully.");
            return 0;
        } catch (Exception e) {
            System.out.println("Generation failed: " + e.getMessage());
            return 1;
        }
    }

    /** Parses and validates the CLI execution date; prints an error and returns null when invalid. */
    private static LocalDate parseExecutionDate(String rawDate)
    {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            System.out.println("The execution date is mandatory (expected format: YYYY-MM-DD, e.g. 2026-07-01)");
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("The execution date \"" + rawDate
                    + "\" is not valid. Please use the YYYY-MM-DD format (e.g. 2026-07-01).");
            return null;
        }
    }

    public static void main( String[] args )
    {
        if (args.length == 0) {
            AppTheme.apply(AppTheme.loadPersistedMode());
            Generator generator = new Generator();
            MainFrame view = new MainFrame(generator, AppInfo.getVersion());
            generator.setView(view);
        } else {
            fromCommandLine(args);
        }
    }
}
