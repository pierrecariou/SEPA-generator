package com.pcariou.service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Single, centralized JAXB marshalling/pretty-printing entry point for every
 * SEPA document family (pain.001.001.02 and pain.001.001.09).
 *
 * <p>Marshalling is directed at a character {@link Writer}
 * ({@link StreamResult}) rather than a {@link java.io.File}. The JAXB reference
 * implementation uses a different, correct indenter for character output; the
 * byte/stream indenter it selects for a {@code File}/{@code OutputStream} has a
 * defect that drops the indentation of the first child of a nested element when
 * that element follows a text-bearing sibling. Routing all writers through this
 * helper keeps formatting identical across message families without any
 * per-element string manipulation.
 *
 * <p>The character path does not emit the XML declaration (fragment mode) or a
 * trailing newline, so both are written explicitly to preserve the exact output
 * the {@code File} path previously produced for pain.001.
 */
public final class SepaXmlMarshaller
{
	private static final String XML_HEADER =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>";

	private SepaXmlMarshaller()
	{
	}

	public static void marshal(Object document, String outputFile) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(document.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		try (Writer writer = new OutputStreamWriter(
				new FileOutputStream(outputFile), StandardCharsets.UTF_8))
		{
			writer.write(XML_HEADER);
			writer.write("\n");
			marshaller.marshal(document, new StreamResult(writer));
			writer.write("\n");
		}
	}
}
