package com.pcariou.generator;

import com.pcariou.model.*;
import com.pcariou.service.*;

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

        String inputFile = args[0];
        String outputFile = args[1];

        if (inputFile.endsWith(".xls") || inputFile.endsWith(".xlsx")) {
            try {
                Workbook workbook = new Workbook(args[0]);
                workbook.save("input_file.csv");
                inputFile = "input_file.csv";
                eraseNoLicenseMessage(inputFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (!inputFile.endsWith(".csv")) {
            System.out.println("Input file must be a CSV or XLS/XLSX file");
            System.exit(1);
        }

        Document document = new CsvToBeans().read(inputFile);
        new BeansToXml().write(document, outputFile);
    }
}
