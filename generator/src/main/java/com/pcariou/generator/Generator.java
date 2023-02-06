package com.pcariou.generator;

import com.pcariou.model.*;
import com.pcariou.service.*;
import com.pcariou.view.*;

import org.apache.commons.io.FilenameUtils;

import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Date;

import javax.swing.UIManager;

import com.aspose.cells.*;

public class Generator implements IGenerator
{
    private static final String[] VALID_EXTENSIONS = {"xls", "xlsx", "csv"};
    private static final String[] EXCEL_EXTENSIONS = {"xls", "xlsx"};

    private GUIView view;

    public Generator()
    {
    }

    public void setView(GUIView view)
    {
        this.view = view;
    }

    public void generate(String inputFile, String outputFile, Date date)
    {
        if (!argumentsAreValid(inputFile, outputFile, date))
            return;
        if (isExcelFile(inputFile))
            inputFile = convertToCsv(inputFile);
        transformCsvToXml(inputFile, outputFile, date);
    }

    private void transformCsvToXml(String inputFile, String outputFile, Date date)
    {
        try {
            CsvToBeans csvToBeans = new CsvToBeans(date);
            Document document = csvToBeans.read(inputFile);
            new BeansToXml().write(document, outputFile);
            view.showSuccessMessage(outputFile + " generated successfully.");
            view.showTableResult(csvToBeans.getTableResult());
        } catch (Exception e) {
            view.showErrorMessage(e.getMessage());
        }
    }

    private boolean argumentsAreValid(String inputFile, String outputFile, Date date)
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

    public static void fromCommanLine(String[] args)
    {
    if (args.length != 4) {
            System.out.println("Usage: java -jar generator.jar <input file> <output file>");
            System.exit(1);
        }
        if (args[0].equals(args[1])) {
            System.out.println("Input and output files must be different");
            System.exit(1);
        }
        if (!args[1].endsWith(".xml")) {
            System.out.println("Output file must be an XML file");
            System.exit(1);
        }

        String inputFilename = args[0];
        String outputFilename = args[1];

        if (inputFilename.endsWith(".xls") || inputFilename.endsWith(".xlsx")) {
            try {
                Workbook workbook = new Workbook(args[0]);
                inputFilename = FilenameUtils.getBaseName(inputFilename) + ".csv";
                workbook.save(FilenameUtils.getBaseName(inputFilename) + ".csv");
                eraseNoLicenseMessage(inputFilename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (!inputFilename.endsWith(".csv")) {
            System.out.println("Input file must be a CSV or XLS/XLSX file");
            System.exit(1);
        }

        try {
            Document document = new CsvToBeans(null).read(inputFilename); // TODO: date from args[2] instead of null
            new BeansToXml().write(document, outputFilename);
            //System.out.println("" + outputFilename + " generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setGUINativeLookAndFeel()
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
    public static void main( String[] args )
    {
        if (args.length == 0) {
            Generator generator = new Generator();
            //setGUINativeLookAndFeel();
            GUIView view = new GUIView(generator);
            generator.setView(view);
        } else {
            fromCommanLine(args);
        }
    }
}
