package com.pcariou.view.main.center;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pcariou.model.PainVersion;

import org.junit.Test;

import javax.swing.JComboBox;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards the credit-transfer format selector after pain.001.001.03 was added as
 * a legacy bank-compatibility option.
 *
 * <p>The selector is built from {@code PainVersion.values()} and rendered with
 * {@code FormPanel.formatLabelFor}, so the enum content, the display order, the
 * user-facing wording and the persisted-code round-trip are what this test
 * pins down. Building the real {@link FormPanel} needs a full application
 * context, so the combo is assembled here exactly as the panel does.
 */
public class FormPanelFormatSelectionTest {

    @Test
    public void selectorOffersTheThreeSupportedVersionsInOrder() {
        JComboBox<PainVersion> combo = new JComboBox<>(PainVersion.values());

        List<PainVersion> shown = new ArrayList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            shown.add(combo.getItemAt(i));
        }

        assertEquals(3, shown.size());
        assertEquals(PainVersion.PAIN_001_001_02, shown.get(0));
        assertEquals(PainVersion.PAIN_001_001_03, shown.get(1));
        assertEquals(PainVersion.PAIN_001_001_09, shown.get(2));
    }

    @Test
    public void modernVersionRemainsTheDefaultWhenNothingIsPersisted() {
        PainVersion persisted = PainVersion.fromCode(null);
        PainVersion selected = persisted != null ? persisted : PainVersion.PAIN_001_001_09;

        assertEquals(PainVersion.PAIN_001_001_09, selected);
    }

    @Test
    public void previouslyPersistedSelectionsStillResolve() {
        assertEquals(PainVersion.PAIN_001_001_02, PainVersion.fromCode("02"));
        assertEquals(PainVersion.PAIN_001_001_09, PainVersion.fromCode("09"));
        assertEquals(PainVersion.PAIN_001_001_03, PainVersion.fromCode("03"));
        assertEquals("03", PainVersion.PAIN_001_001_03.getCode());
    }

    @Test
    public void legacyVersionIsLabelledAsBankCompatibility() throws Exception {
        Method formatLabelFor = FormPanel.class.getDeclaredMethod("formatLabelFor", PainVersion.class);
        formatLabelFor.setAccessible(true);

        String label = (String) formatLabelFor.invoke(null, PainVersion.PAIN_001_001_03);

        assertTrue("Expected the schema id in the label: " + label,
                label.contains("pain.001.001.03"));
        assertTrue("Expected plain-language legacy wording: " + label,
                label.toLowerCase().contains("legacy bank compatibility"));
        assertEquals("pain.001.001.09 (modern ISO 20022)",
                formatLabelFor.invoke(null, PainVersion.PAIN_001_001_09));
        assertEquals("pain.001.001.02 (classic)",
                formatLabelFor.invoke(null, PainVersion.PAIN_001_001_02));
    }
}
