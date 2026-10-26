package com.pcariou.service;

import com.pcariou.model.Document;
import com.pcariou.model.InitiatingPartyIdentifier;
import com.pcariou.model.PainVersion;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The optional Group Header initiating-party identifier ({@code InitgPty/Id})
 * in every Community Credit Transfer version: legacy SIRET output is
 * unchanged, international identifiers carry their scheme and issuer where the
 * schema allows it, a missing identifier omits {@code Id}, nothing else in the
 * file changes, and every generated file is valid against the official ISO
 * 20022 XSD.
 */
public class InitiatingPartyIdentificationXmlTest {

    private static final String CT_CSV =
            "name,IBAN,BIC,amount,end_to_end_id,information\n"
            + "Karlson GmbH,DE89 3704 0044 0532 0130 00,deutdeff,1500,E2E-1,Invoice 42\n";

    private static final String SIRET = "\"siret\": \"12345678901234\"";
    private static final String BANK_CODE =
            "\"siret\": \"IE77889900\", \"schemeCode\": \"bank\", \"issuer\": \"BOFI\"";
    private static final String PROPRIETARY =
            "\"siret\": \"OIN-123456\", \"schemeProprietary\": \"Originator ID\"";
    private static final String NONE = null;

    private static final PainVersion[] STRUCTURED =
            { PainVersion.PAIN_001_001_03, PainVersion.PAIN_001_001_09 };

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousConfig;

    @Before
    public void rememberConfig() {
        previousConfig = System.getProperty("sepa.config.file");
    }

    @After
    public void restoreConfig() {
        if (previousConfig == null) {
            System.clearProperty("sepa.config.file");
        } else {
            System.setProperty("sepa.config.file", previousConfig);
        }
    }

    @Test
    public void legacySiretOutputIsUnchanged() throws Exception {
        assertEquals("<Nm>Acme Ltd</Nm><Id><OrgId><PrtryId><Id>12345678901234</Id></PrtryId></OrgId></Id>",
                initiatingParty(creditTransfer(SIRET, PainVersion.PAIN_001_001_02)));
        for (PainVersion version : STRUCTURED) {
            assertEquals(version.toString(),
                    "<Nm>Acme Ltd</Nm><Id><OrgId><Othr><Id>12345678901234</Id></Othr></OrgId></Id>",
                    initiatingParty(creditTransfer(SIRET, version)));
        }
    }

    @Test
    public void isoSchemeCodeAndIssuerAreWrittenInPain03And09() throws Exception {
        for (PainVersion version : STRUCTURED) {
            assertEquals(version.toString(), "<Nm>Acme Ltd</Nm><Id><OrgId><Othr><Id>IE77889900</Id>"
                    + "<SchmeNm><Cd>BANK</Cd></SchmeNm><Issr>BOFI</Issr></Othr></OrgId></Id>",
                    initiatingParty(creditTransfer(BANK_CODE, version)));
        }
    }

    @Test
    public void proprietarySchemeIsWrittenInPain03And09() throws Exception {
        for (PainVersion version : STRUCTURED) {
            assertEquals(version.toString(), "<Nm>Acme Ltd</Nm><Id><OrgId><Othr><Id>OIN-123456</Id>"
                    + "<SchmeNm><Prtry>Originator ID</Prtry></SchmeNm></Othr></OrgId></Id>",
                    initiatingParty(creditTransfer(PROPRIETARY, version)));
        }
    }

    @Test
    public void pain02CarriesTheIssuerButHasNoSchemeName() throws Exception {
        assertEquals("<Nm>Acme Ltd</Nm><Id><OrgId><PrtryId><Id>IE77889900</Id><Issr>BOFI</Issr>"
                + "</PrtryId></OrgId></Id>",
                initiatingParty(creditTransfer(BANK_CODE, PainVersion.PAIN_001_001_02)));
    }

    @Test
    public void missingIdentifierOmitsId() throws Exception {
        for (PainVersion version : PainVersion.values()) {
            assertEquals(version.toString(), "<Nm>Acme Ltd</Nm>",
                    initiatingParty(creditTransfer(NONE, version)));
        }
    }

    @Test
    public void outputsDifferOnlyInInitiatingParty() throws Exception {
        for (PainVersion version : PainVersion.values()) {
            String reference = withoutInitiatingParty(creditTransfer(SIRET, version));
            for (String identifier : new String[] { NONE, BANK_CODE, PROPRIETARY }) {
                assertEquals(version + " " + identifier, reference,
                        withoutInitiatingParty(creditTransfer(identifier, version)));
            }
        }
    }

    @Test
    public void invalidIdentifierBlocksGeneration() throws Exception {
        try {
            creditTransfer("\"siret\": \"123\", \"schemeCode\": \"SRET\"", PainVersion.PAIN_001_001_09);
            fail("A SRET identifier that is not 14 digits must be rejected");
        } catch (Exception expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("SIRET"));
        }
    }

    @Test
    public void pain02WarnsOnlyWhenAConfiguredSchemeIsLeftOut() throws Exception {
        assertEquals(Collections.singletonList(InitiatingPartyIdentifier.SCHEME_NOT_IN_PAIN_001_02),
                GenerationWarnings.forDocument(document(BANK_CODE), PainVersion.PAIN_001_001_02));
        assertEquals(Collections.singletonList(InitiatingPartyIdentifier.SCHEME_NOT_IN_PAIN_001_02),
                GenerationWarnings.forDocument(document(PROPRIETARY), PainVersion.PAIN_001_001_02));
        assertTrue(GenerationWarnings.forDocument(document(SIRET), PainVersion.PAIN_001_001_02).isEmpty());
        assertTrue(GenerationWarnings.forDocument(document(NONE), PainVersion.PAIN_001_001_02).isEmpty());
        for (PainVersion version : STRUCTURED) {
            assertTrue(GenerationWarnings.forDocument(document(BANK_CODE), version).isEmpty());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Document document(String identifierJson) throws Exception {
        writeConfig(identifierJson);
        return new CsvToBeans(LocalDate.now().plusDays(5)).read(new StringReader(CT_CSV));
    }

    private String creditTransfer(String identifierJson, PainVersion version) throws Exception {
        Document document = document(identifierJson);
        File xml = tmp.newFile();
        PainWriter.forVersion(version).write(document, xml.getAbsolutePath());
        assertSchemaValid(xml, version);
        return compact(xml);
    }

    private void writeConfig(String identifierJson) throws Exception {
        String json = "{\n"
                + "  \"debtor\": { \"name\": \"Acme Ltd\", \"iban\": \"FR1420041010050500013M02606\","
                + " \"bic\": \"BNPAFRPP\" },\n"
                + "  \"initiatingParty\": { \"name\": \"Acme Ltd\""
                + (identifierJson == null ? "" : ", " + identifierJson) + " }\n"
                + "}\n";
        File config = tmp.newFile();
        Files.write(config.toPath(), json.getBytes(StandardCharsets.UTF_8));
        System.setProperty("sepa.config.file", config.getAbsolutePath());
    }

    /** Validates against the authentic ISO 20022 schema in src/test/resources/iso20022. */
    static void assertSchemaValid(File xml, PainVersion version) throws Exception {
        URL xsd = InitiatingPartyIdentificationXmlTest.class.getResource(
                "/iso20022/pain.001.001." + version.getCode() + ".xsd");
        assertNotNull("Missing XSD for " + version, xsd);
        try {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd)
                    .newValidator().validate(new StreamSource(xml));
        } catch (SAXException e) {
            fail(version + " output is not XSD-valid: " + e.getMessage());
        }
    }

    private static String compact(File xml) throws Exception {
        String content = new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8);
        return content.replaceAll(">\\s+<", "><")
                .replaceAll("<MsgId>[^<]*</MsgId>", "<MsgId/>")
                .replaceAll("<PmtInfId>[^<]*</PmtInfId>", "<PmtInfId/>")
                .replaceAll("<CreDtTm>[^<]*</CreDtTm>", "<CreDtTm/>");
    }

    private static String initiatingParty(String xml) {
        return between(xml, "<InitgPty>", "</InitgPty>");
    }

    private static String withoutInitiatingParty(String xml) {
        return xml.replaceFirst("<InitgPty>.*?</InitgPty>", "<InitgPty/>");
    }

    private static String between(String xml, String open, String close) {
        int start = xml.indexOf(open);
        assertTrue("Missing " + open, start >= 0);
        return xml.substring(start + open.length(), xml.indexOf(close, start));
    }
}
