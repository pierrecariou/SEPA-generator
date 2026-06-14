package com.pcariou.view;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the non-UI logic of {@link ExternalLinks}: target classification and
 * {@code mailto:} stripping. The actual Desktop interaction is not exercised here
 * because it requires a windowing environment.
 */
public class ExternalLinksTest {

    @Test
    public void classifiesMailtoAsMail() {
        assertEquals(ExternalLinks.Kind.MAIL, ExternalLinks.classify("mailto:contact@sepa-xml-generator.com"));
    }

    @Test
    public void mailtoSchemeIsCaseInsensitive() {
        assertEquals(ExternalLinks.Kind.MAIL, ExternalLinks.classify("MAILTO:contact@sepa-xml-generator.com"));
    }

    @Test
    public void classifiesHttpsAsBrowse() {
        assertEquals(ExternalLinks.Kind.BROWSE, ExternalLinks.classify("https://sepa-xml-generator.com"));
    }

    @Test
    public void classifiesNullAsBrowse() {
        assertEquals(ExternalLinks.Kind.BROWSE, ExternalLinks.classify(null));
    }

    @Test
    public void stripsMailtoPrefix() {
        assertEquals("contact@sepa-xml-generator.com",
                ExternalLinks.stripMailto("mailto:contact@sepa-xml-generator.com"));
    }

    @Test
    public void leavesNonMailtoUnchanged() {
        assertEquals("https://sepa-xml-generator.com",
                ExternalLinks.stripMailto("https://sepa-xml-generator.com"));
    }
}
