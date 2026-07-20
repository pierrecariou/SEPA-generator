package com.pcariou.view.main;

import com.pcariou.view.SvgIcons;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the footer status design rule:
 *
 * <ul>
 *   <li>Terminal semantic outcomes (generated / failed) carry a semantic icon +
 *       colour key and keep neutral text.</li>
 *   <li>Every other state (neutral / instructional / in-progress) is icon-free
 *       and uses a neutral text colour.</li>
 *   <li>No status uses the legacy generic status dot.</li>
 * </ul>
 */
public class AppStatusTest {

    private static final Set<AppStatus> TERMINAL_SUCCESS =
            EnumSet.of(AppStatus.GENERATED);
    private static final Set<AppStatus> TERMINAL_FAILURE =
            EnumSet.of(AppStatus.GENERATION_FAILED);

    @Test
    public void terminalSuccessCarriesCheckIconAndSuccessColour() {
        for (AppStatus s : TERMINAL_SUCCESS) {
            assertEquals(s + " uses the circle-check icon", SvgIcons.CIRCLE_CHECK, s.iconName);
            assertEquals(s + " tints with the success colour", "App.successColor", s.iconColorKey);
            assertNeutralText(s);
        }
    }

    @Test
    public void terminalFailureCarriesXIconAndErrorColour() {
        for (AppStatus s : TERMINAL_FAILURE) {
            assertEquals(s + " uses the circle-x icon", SvgIcons.CIRCLE_X, s.iconName);
            assertEquals(s + " tints with the error colour", "App.errorColor", s.iconColorKey);
            assertNeutralText(s);
        }
    }

    @Test
    public void nonTerminalStatesAreIconFree() {
        for (AppStatus s : AppStatus.values()) {
            if (TERMINAL_SUCCESS.contains(s) || TERMINAL_FAILURE.contains(s)) {
                assertNotNull(s + " is terminal and must have an icon", s.iconName);
                continue;
            }
            assertNull(s + " must be icon-free", s.iconName);
            assertNull(s + " must have no icon colour", s.iconColorKey);
        }
    }

    @Test
    public void noStatusUsesTheLegacyDot() {
        for (AppStatus s : AppStatus.values()) {
            assertFalse(s + " must not embed the legacy status dot",
                    s.label.contains("\u25CF"));
        }
    }

    @Test
    public void highestPrefersLowerOrdinal() {
        assertEquals(AppStatus.GENERATION_FAILED,
                AppStatus.highest(AppStatus.READY, AppStatus.GENERATION_FAILED));
        assertEquals(AppStatus.READY, AppStatus.highest((AppStatus) null));
    }

    /** Terminal outcomes keep neutral text so the icon (not the sentence) carries the colour. */
    private static void assertNeutralText(AppStatus s) {
        assertTrue(s + " keeps neutral text (a UIManager foreground key)",
                s.styleColor.startsWith("$"));
    }
}
