package com.pcariou.view;

import org.junit.Test;

import javax.swing.Icon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the standardized semantic mapping used by application message
 * dialogs: each {@link AppDialogs.Kind} yields a dialog icon and a matching
 * {@link javax.swing.JOptionPane} message type, so dialogs look consistent and
 * avoid the default Swing/FlatLaf icons.
 */
public class AppDialogsTest {

    @Test
    public void everyKindProducesADialogIcon() {
        for (AppDialogs.Kind kind : AppDialogs.Kind.values()) {
            Icon icon = kind.icon();
            assertNotNull("Kind " + kind + " must have an icon", icon);
            assertEquals("Dialog icons are square, " + SvgIcons.DIALOG_ICON_SIZE + "px",
                    SvgIcons.DIALOG_ICON_SIZE, icon.getIconWidth());
            assertEquals(SvgIcons.DIALOG_ICON_SIZE, icon.getIconHeight());
        }
    }

    @Test
    public void kindsMapToDistinctSemanticIcons() {
        String success = AppDialogs.Kind.SUCCESS.iconName();
        String error = AppDialogs.Kind.ERROR.iconName();
        String info = AppDialogs.Kind.INFO.iconName();
        String warning = AppDialogs.Kind.WARNING.iconName();

        assertEquals(SvgIcons.CIRCLE_CHECK, success);
        assertEquals(SvgIcons.CIRCLE_X, error);
        assertEquals(SvgIcons.CIRCLE_INFO, info);
        assertEquals(SvgIcons.TRIANGLE_ALERT, warning);

        assertTrue("success and error icons differ", !success.equals(error));
        assertTrue("info and warning icons differ", !info.equals(warning));
    }
}
