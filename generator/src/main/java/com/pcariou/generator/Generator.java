package com.pcariou.generator;

import com.pcariou.model.*;
import com.pcariou.service.*;

import org.apache.commons.io.FilenameUtils;

import java.io.RandomAccessFile;

import com.aspose.cells.*;


public class Generator
{
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

    public static void main( String[] args )
    {
        if (args.length != 2) {
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

        Document document = new CsvToBeans().read(inputFilename);
        new BeansToXml().write(document, outputFilename);
    }
}
