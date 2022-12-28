package com.pcariou.model;

import javax.xml.bind.annotation.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.02")
@XmlAccessorType(XmlAccessType.FIELD)
public class Document
{
	@XmlElement(name = "pain.001.001.02")
	private Pain pain;

	public Document() {
	}

	public Document(Pain pain) {
		this.pain = pain;
	}

	public Pain getPain() {
		return pain;
	}

	public void setPain(Pain pain) {
		this.pain = pain;
	}
}
