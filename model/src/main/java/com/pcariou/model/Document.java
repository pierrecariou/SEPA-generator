package com.pcariou.model;

import javax.xml.bind.annotation.*;

/**
 * Hello world!
 *
 */
@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class Document
{
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
