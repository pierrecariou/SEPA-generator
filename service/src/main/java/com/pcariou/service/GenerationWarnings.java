package com.pcariou.service;

import com.pcariou.model.Document;
import com.pcariou.model.InitiatingPartyIdentifier;
import com.pcariou.model.PainVersion;
import com.pcariou.model.ProprietaryIdentification;

import java.util.ArrayList;
import java.util.List;

/**
 * Nonblocking warnings about configured data that the chosen pain.001 version
 * cannot carry. Generation still succeeds; the warnings only explain what was
 * left out of the file.
 */
public final class GenerationWarnings
{
	private GenerationWarnings()
	{
	}

	/** Warnings for writing {@code document} as {@code version}; empty when none apply. */
	public static List<String> forDocument(Document document, PainVersion version)
	{
		List<String> warnings = new ArrayList<String>();
		String identifierWarning = InitiatingPartyIdentifier.formatWarning(initiatingPartyIdentifier(document), version);
		if (identifierWarning != null) {
			warnings.add(identifierWarning);
		}
		return warnings;
	}

	private static InitiatingPartyIdentifier initiatingPartyIdentifier(Document document)
	{
		if (document == null || document.getPain() == null || document.getPain().getGroupHeader() == null
				|| document.getPain().getGroupHeader().getInitiatingParty() == null
				|| document.getPain().getGroupHeader().getInitiatingParty().getPartyIdentification() == null
				|| document.getPain().getGroupHeader().getInitiatingParty().getPartyIdentification()
						.getOrganisationIdentification() == null) {
			return null;
		}
		ProprietaryIdentification id = document.getPain().getGroupHeader().getInitiatingParty()
				.getPartyIdentification().getOrganisationIdentification().getProprietaryIdentification();
		return id == null ? null
				: InitiatingPartyIdentifier.of(id.getId(), id.getSchemeCode(), id.getSchemeProprietary(), id.getIssuer());
	}
}
