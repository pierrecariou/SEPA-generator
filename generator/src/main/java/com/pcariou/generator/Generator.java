package com.pcariou.generator;

import com.pcariou.model.Document;
import com.pcariou.model.Pain;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.*;

import com.aspose.cells.*;


public class Generator
{
    public static void convertJavaToXml(Object object, String filename) throws JAXBException
    {
        JAXBContext jc = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = jc.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(object, new File(filename));
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

        if (inputFile.endsWith(".xls")) {
            try {
                Workbook workbook = new Workbook(args[0]);
                workbook.save("input_file.csv");
                inputFile = "input_file.csv";
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (!inputFile.endsWith(".csv")) {
            System.out.println("Input file must be a CSV or XLS file");
            System.exit(1);
        }
        
        Document document = new Document(new Pain());
        try {
            convertJavaToXml(document, outputFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
