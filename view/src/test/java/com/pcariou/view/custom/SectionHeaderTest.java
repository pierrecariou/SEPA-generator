package com.pcariou.view.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;

import javax.swing.JLabel;

import org.junit.Test;

/**
 * G1 visual coherence pass — the shared {@link SectionHeader} primitive.
 *
 * <p>A section header is a compact title plus a thin separator rule: no fill, no
 * border, no shadow. It reads as hierarchy, not as another card.
 */
public class SectionHeaderTest {

    @Test
    public void exposesItsTitle() {
        assertEquals("Input", new SectionHeader("Input").getTitle());
    }

    @Test
    public void isTransparentSoItBlendsIntoTheCard() {
        assertFalse(new SectionHeader("Input").isOpaque());
    }

    @Test
    public void rendersATitleLabelFollowedByASeparatorRule() {
        SectionHeader header = new SectionHeader("Output");

        assertEquals("title label + separator rule", 2, header.getComponentCount());

        Component first = header.getComponent(0);
        assertTrue("first child is the title label", first instanceof JLabel);
        assertEquals("Output", ((JLabel) first).getText());

        Component second = header.getComponent(1);
        assertFalse("the rule is not another label", second instanceof JLabel);
        assertEquals("the separator is a 1px-tall rule", 1, second.getPreferredSize().height);
    }
}
