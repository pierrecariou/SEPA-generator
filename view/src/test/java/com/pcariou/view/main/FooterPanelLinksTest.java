package com.pcariou.view.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.pcariou.view.AppLinks;

import org.junit.Test;

import javax.swing.AbstractButton;
import java.awt.Component;
import java.awt.Container;

/**
 * The footer link group states the publisher once, quietly, next to the
 * existing product and support links.
 */
public class FooterPanelLinksTest {

    @Test
    public void footerOffersTheCompanyLink() {
        assertEquals("https://niryosys.com", AppLinks.COMPANY);
        assertNotNull("The footer must offer the publisher's site",
                button(new FooterPanel(null, null), "Niryosys"));
    }

    @Test
    public void footerDoesNotRepeatThePublisherByline() {
        String text = allText(new FooterPanel(null, null));

        assertFalse("The footer names the company once; the byline lives in About",
                text.contains("by Niryosys"));
        assertFalse("The product is not renamed after its publisher",
                text.contains("Niryosys SEPA Generator"));
    }

    private static AbstractButton button(Component component, String text) {
        if (component instanceof AbstractButton
                && text.equals(((AbstractButton) component).getText())) {
            return (AbstractButton) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                AbstractButton found = button(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String allText(Component component) {
        StringBuilder out = new StringBuilder();
        collect(component, out);
        return out.toString();
    }

    private static void collect(Component component, StringBuilder out) {
        if (component instanceof AbstractButton) {
            out.append(((AbstractButton) component).getText()).append('\n');
        } else if (component instanceof javax.swing.JLabel) {
            out.append(((javax.swing.JLabel) component).getText()).append('\n');
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collect(child, out);
            }
        }
    }
}
