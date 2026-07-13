package com.pcariou.view;

import org.junit.After;
import org.junit.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;

/**
 * Verifies the centralized semantic status palette, the shared menu/combo/
 * OptionPane surfaces, and the live theme refresh of off-screen components
 * (reused popup menus).
 */
public class AppThemeTest {

    @After
    public void restoreLightTheme() {
        AppTheme.apply(AppTheme.Mode.LIGHT);
    }

    @Test
    public void semanticColorsAreDefinedDistinctAndNotTheAccent_light() {
        assertSemanticPalette(AppTheme.Mode.LIGHT);
    }

    @Test
    public void semanticColorsAreDefinedDistinctAndNotTheAccent_dark() {
        assertSemanticPalette(AppTheme.Mode.DARK);
    }

    private void assertSemanticPalette(AppTheme.Mode mode) {
        AppTheme.apply(mode);

        Color success = UIManager.getColor("App.successColor");
        Color error   = UIManager.getColor("App.errorColor");
        Color warning = UIManager.getColor("App.warningColor");
        Color info    = UIManager.getColor("App.infoColor");
        Color accent  = UIManager.getColor("Component.accentColor");

        assertNotNull("success defined", success);
        assertNotNull("error defined", error);
        assertNotNull("warning defined", warning);
        assertNotNull("info defined", info);

        // All four semantic families are visually distinct.
        assertNotEquals(success, error);
        assertNotEquals(success, warning);
        assertNotEquals(success, info);
        assertNotEquals(error, warning);
        assertNotEquals(error, info);
        assertNotEquals(warning, info);

        // Success must not be the turquoise accent (accent is for actions/links).
        assertNotEquals("success must differ from the accent", accent, success);
    }

    @Test
    public void menuSelectionUsesAccentTintedSelectionNotDefaultBlue() {
        AppTheme.apply(AppTheme.Mode.DARK);
        Color menuSel = UIManager.getColor("MenuItem.selectionBackground");
        assertNotNull(menuSel);
        // Menu selection reuses the shared subtle turquoise selection background,
        // the same family as tables/lists - not FlatLaf's derived blue.
        assertEquals(UIManager.getColor("List.selectionBackground"), menuSel);
    }

    @Test
    public void registeredPopupAdoptsActiveThemeAfterLiveSwitch() {
        AppTheme.apply(AppTheme.Mode.LIGHT);

        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem("Item"));
        AppTheme.registerThemedComponent(popup);

        AppTheme.apply(AppTheme.Mode.DARK);
        assertEquals("Popup adopts the dark PopupMenu.background after a live switch",
                UIManager.getColor("PopupMenu.background"), popup.getBackground());

        AppTheme.apply(AppTheme.Mode.LIGHT);
        assertEquals("Popup adopts the light PopupMenu.background after switching back",
                UIManager.getColor("PopupMenu.background"), popup.getBackground());
    }

    @Test
    public void warningColorMatchesTheDeeperOchrePalettePerTheme() {
        AppTheme.apply(AppTheme.Mode.LIGHT);
        assertEquals("light warning is the deep ochre #9A6700",
                new Color(154, 103, 0), toPlainColor(UIManager.getColor("App.warningColor")));

        AppTheme.apply(AppTheme.Mode.DARK);
        assertEquals("dark warning is the softer gold #D6A84B",
                new Color(214, 168, 75), toPlainColor(UIManager.getColor("App.warningColor")));
    }

    @Test
    public void optionPaneDialogsSitOnTheApplicationBackground() {
        for (AppTheme.Mode mode : AppTheme.Mode.values()) {
            AppTheme.apply(mode);
            assertEquals(mode + ": JOptionPane dialogs must use the app background",
                    UIManager.getColor("Panel.background"),
                    UIManager.getColor("OptionPane.background"));
        }
    }

    @Test
    public void comboBoxDropdownsUseTheSharedSelectionAndPopupSurfaces() {
        for (AppTheme.Mode mode : AppTheme.Mode.values()) {
            AppTheme.apply(mode);
            assertEquals(mode + ": combo selection matches the shared selection family",
                    UIManager.getColor("List.selectionBackground"),
                    UIManager.getColor("ComboBox.selectionBackground"));
            assertEquals(mode + ": combo selection text stays the normal foreground",
                    UIManager.getColor("Label.foreground"),
                    UIManager.getColor("ComboBox.selectionForeground"));
            assertEquals(mode + ": combo popup matches the other popup menus",
                    UIManager.getColor("PopupMenu.background"),
                    UIManager.getColor("ComboBox.popupBackground"));
            assertEquals(mode + ": combo field matches the other input fields",
                    UIManager.getColor("TextField.background"),
                    UIManager.getColor("ComboBox.background"));
        }
    }

    /** Normalizes a (possibly ColorUIResource) UIManager color to a plain Color for RGB comparison. */
    private static Color toPlainColor(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }
}
