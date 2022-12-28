package com.pcariou.generator;

import com.pcariou.model.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.*;

import com.aspose.cells.*;
import java.util.*;

public class Generator
{
    public static Document createDocument(HashMap<String, String> accountHolderInfo)
    {
        InitiatingParty initiatingParty = new InitiatingParty();

        GroupHeader groupHeader = new GroupHeader("messageIdentification", "creationDateTime", "numberOfTransactions", initiatingParty);
        groupHeader.setControlSum("2000");

        CreditTransferTransactionInformation creditTransferTransactionInformation = new CreditTransferTransactionInformation("pmtId", "amt", "cdtrAgt", "cdtr", "cdtrAcct", "rmtInf");

        ServiceLevel serviceLevel = new ServiceLevel("Cd");
        PaymentTypeInformation paymentTypeInformation = new PaymentTypeInformation("instructionPriority", serviceLevel, "categoryPurpose");

        Debtor debtor = new Debtor("name");

        AccountIdentification accountIdentification = new AccountIdentification("iban");
        DebtorAccount debtorAccount = new DebtorAccount(accountIdentification, "EUR");

        FinancialInstitutionIdentification financialInstitutionIdentification = new FinancialInstitutionIdentification("bic");
        DebtorAgent debtorAgent = new DebtorAgent(financialInstitutionIdentification);

        OrganisationIdentification organisationIdentification = new OrganisationIdentification();

        PartyIdentification partyIdentification = new PartyIdentification(organisationIdentification);

        UltimateDebtor ultimateDebtor = new UltimateDebtor("name", partyIdentification);

        ArrayList<PaymentInformation> paymentInformations = new ArrayList<>();
        PaymentInformation paymentInformation = new PaymentInformation("paymentInformationIdentification", "paymentMethod", paymentTypeInformation, "requestedExecutionDate", debtor, debtorAccount, debtorAgent, ultimateDebtor, "chargeBearer");
        paymentInformations.add(paymentInformation);
        paymentInformations.add(paymentInformation);
        Pain pain = new Pain(groupHeader, paymentInformations);
        Document document = new Document(pain);
        return document;
    }

    public static void convertJavaToXml(Object object, String filename) throws JAXBException
    {
        JAXBContext context = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = context.createMarshaller();

        //marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        //modify the standalone attribute to false
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
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

        HashMap<String, String> accountHolderInfo = new HashMap<String, String>();
        accountHolderInfo.put("name", "John Doe");
        accountHolderInfo.put("IBAN", "DE89370400440532013000");
        accountHolderInfo.put("BIC", "COBADEFFXXX");
        accountHolderInfo.put("id", "DE98ZZZ09999999999");
        accountHolderInfo.put("currency", "EUR");
        
        Document document = createDocument(accountHolderInfo);
       // Document document = new Document(new Pain());
        try {
            convertJavaToXml(document, outputFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
