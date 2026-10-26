package com.pcariou.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.pcariou.model.InitiatingPartyIdentifier;
import com.pcariou.view.config.AppConfig;

import org.junit.Test;

/** Settings-file mapping of the optional initiating party identifier. */
public class InitiatingPartySettingsTest
{
	@Test
	public void legacySiretMapsToAnIdentifierWithoutScheme()
	{
		AppConfig.InitiatingParty party = party("12345678901234", null, null, null);
		assertEquals(InitiatingPartyIdentifier.ofId("12345678901234"), SettingsFrame.identifierOf(party));
	}

	@Test
	public void blankOrMissingIdentifierMapsToNone()
	{
		assertNull(SettingsFrame.identifierOf(null));
		assertNull(SettingsFrame.identifierOf(party(null, null, null, null)));
		assertNull(SettingsFrame.identifierOf(party(" ", "", null, " ")));
	}

	@Test
	public void schemeAndIssuerAreKept()
	{
		assertEquals(InitiatingPartyIdentifier.of("IE77889900", "BANK", null, "BOFI"),
				SettingsFrame.identifierOf(party("IE77889900", "BANK", null, "BOFI")));
		assertEquals(InitiatingPartyIdentifier.of("OIN-1", null, "Originator ID", null),
				SettingsFrame.identifierOf(party("OIN-1", null, "Originator ID", null)));
	}

	@Test
	public void panelRoundTripsTheIdentifier()
	{
		InitiatingPartyPanel panel = new InitiatingPartyPanel();
		assertNull("A blank panel means no identifier", panel.getIdentifier());

		for (InitiatingPartyIdentifier identifier : new InitiatingPartyIdentifier[] {
				InitiatingPartyIdentifier.ofId("12345678901234"),
				InitiatingPartyIdentifier.of("IE77889900", "BANK", null, "BOFI"),
				InitiatingPartyIdentifier.of("OIN-1", null, "Originator ID", null) }) {
			panel.setIdentifier(identifier);
			assertEquals(identifier, panel.getIdentifier());
		}
		panel.setIdentifier(null);
		assertNull(panel.getIdentifier());
	}

	private static AppConfig.InitiatingParty party(String id, String code, String proprietary, String issuer)
	{
		AppConfig.InitiatingParty party = new AppConfig.InitiatingParty();
		party.name = "Acme";
		party.siret = id;
		party.schemeCode = code;
		party.schemeProprietary = proprietary;
		party.issuer = issuer;
		return party;
	}
}
