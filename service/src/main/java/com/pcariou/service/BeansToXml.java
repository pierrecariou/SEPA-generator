package com.pcariou.service;

import com.pcariou.model.Document;

public class BeansToXml
{
	public void write(Document document, String outputFile) throws Exception
	{
			SepaXmlMarshaller.marshal(document, outputFile);
	}
}
