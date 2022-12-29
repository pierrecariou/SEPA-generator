package com.pcariou.service;

import com.pcariou.model.Document;
import javax.xml.bind.*;
import java.io.*;

public class BeansToXml
{
	public void write(Document document, String outputFile) {
		try {
			JAXBContext context = JAXBContext.newInstance(Document.class);
			Marshaller marshaller = context.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        	marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			marshaller.marshal(document, new File(outputFile));
		} catch (JAXBException e) {
			System.out.println(e.getMessage());
		}
	}
}
