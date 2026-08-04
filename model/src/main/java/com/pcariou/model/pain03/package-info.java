/**
 * Output-only JAXB DTOs for ISO 20022 pain.001.001.03.
 *
 * <p>JAXB binds the XML namespace at package level, so the .03 message needs
 * its own package, separate from the pain.001.001.02 model in
 * {@code com.pcariou.model} and the pain.001.001.09 model in
 * {@code com.pcariou.model.pain09}. These classes carry no CSV binding and no
 * validation: parsing and validation stay on the shared .02 model, and
 * {@code Pain03Writer} maps the validated data into these DTOs just before
 * marshalling.
 */
@javax.xml.bind.annotation.XmlSchema(namespace = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
package com.pcariou.model.pain03;
