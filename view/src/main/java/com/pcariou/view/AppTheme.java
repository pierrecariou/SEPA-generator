package com.pcariou.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.pcariou.view.config.ConfigStore;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AppTheme {
    private static final ConfigStore CONFIG = new ConfigStore();
    private static Mode currentMode = Mode.LIGHT;

    /**
     * Components that live outside any window's visible hierarchy while hidden
     * (chiefly reused {@link javax.swing.JPopupMenu}s). {@link FlatLaf#updateUI()}
     * only reaches components attached to showing windows, so these must be
     * refreshed explicitly on theme change or they keep stale colors. Weakly
     * held so registering a component never keeps it alive.
     */
    private static final List<WeakReference<Component>> THEMED_COMPONENTS = new ArrayList<>();

    public enum Mode { LIGHT, DARK }

    public static Mode getCurrentMode() { return currentMode; }

    private AppTheme() {}

    /** Resolves the persisted theme, falling back to {@link Mode#LIGHT}. */
    public static Mode loadPersistedMode() {
        String stored = CONFIG.readTheme();
        if (stored != null) {
            try {
                return Mode.valueOf(stored.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unknown value — fall through to default
            }
        }
        return Mode.LIGHT;
    }

    public static void apply(Mode mode) {
        try {
            currentMode = mode;

            if (mode == Mode.DARK) FlatDarkLaf.setup();
            else FlatLightLaf.setup();

            applyDefaults(mode);

            UIManager.put("Component.arc",     10);
            UIManager.put("Button.arc",        10);
            UIManager.put("TextComponent.arc", 10);

            FlatLaf.updateUI();
            refreshThemedComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers a component (typically a reused {@link javax.swing.JPopupMenu})
     * to be refreshed on every theme change, so it adopts the active theme even
     * while it is not currently displayed. Safe to call once at construction.
     */
    public static void registerThemedComponent(Component component) {
        if (component == null) {
            return;
        }
        THEMED_COMPONENTS.add(new WeakReference<>(component));
    }

    /** Re-applies the current UI to registered off-screen components. */
    private static void refreshThemedComponents() {
        Iterator<WeakReference<Component>> it = THEMED_COMPONENTS.iterator();
        while (it.hasNext()) {
            Component component = it.next().get();
            if (component == null) {
                it.remove();
            } else {
                SwingUtilities.updateComponentTreeUI(component);
            }
        }
    }

    public static void switchMode() {
        apply(currentMode == Mode.LIGHT ? Mode.DARK : Mode.LIGHT);
        CONFIG.saveTheme(currentMode.name());
    }

    private static void applyDefaults(Mode mode) {
        boolean dark = (mode == Mode.DARK);

        // ── Light palette ──────────────────────────────────────────────────
        ColorUIResource lightAppBg   = c(245, 247, 250);
        ColorUIResource lightCard    = c(255, 255, 255);
        ColorUIResource lightInputBg = c(248, 250, 252);
        ColorUIResource lightBorder  = c(224, 229, 236);
        ColorUIResource lightText    = c(31,  41,  55);
        ColorUIResource lightText2   = c(107, 114, 128);
        ColorUIResource lightSelBg   = c(209, 236, 232);
        ColorUIResource lightAccent  = c(15,  118, 110);
        ColorUIResource lightAccent2 = c(13,  94,  86);
        ColorUIResource lightDisBg   = c(237, 239, 243);
        ColorUIResource lightDisFg   = c(156, 163, 175);

        // ── Dark palette ───────────────────────────────────────────────────
        ColorUIResource darkAppBg    = c(31,  35,  40);
        ColorUIResource darkCard     = c(44,  49,  57);
        ColorUIResource darkInputBg  = c(34,  39,  46);
        ColorUIResource darkBorder   = c(58,  65,  75);
        ColorUIResource darkText     = c(229, 231, 235);
        ColorUIResource darkText2    = c(167, 174, 186);
        ColorUIResource darkSelBg    = c(36,  68,  62);
        ColorUIResource darkAccent   = c(20,  184, 166);
        ColorUIResource darkAccent2  = c(45,  212, 191);
        ColorUIResource darkDisBg    = c(46,  51,  58);
        ColorUIResource darkDisFg    = c(100, 107, 118);

        ColorUIResource appBg   = dark ? darkAppBg   : lightAppBg;
        ColorUIResource panelBg = dark ? darkCard    : lightCard;
        ColorUIResource inputBg = dark ? darkInputBg : lightInputBg;
        ColorUIResource border  = dark ? darkBorder  : lightBorder;
        ColorUIResource text    = dark ? darkText    : lightText;
        ColorUIResource text2   = dark ? darkText2   : lightText2;
        ColorUIResource selBg   = dark ? darkSelBg   : lightSelBg;
        ColorUIResource accent  = dark ? darkAccent  : lightAccent;
        ColorUIResource accent2 = dark ? darkAccent2 : lightAccent2;
        ColorUIResource disBg   = dark ? darkDisBg   : lightDisBg;
        ColorUIResource disFg   = dark ? darkDisFg   : lightDisFg;
        // Accent button text: near-black on bright teal (dark), white on deep teal (light)
        ColorUIResource accentFg = dark ? c(10, 28, 26) : new ColorUIResource(Color.WHITE);
        // Card border: slightly lighter than the card itself to hint separation without harsh lines
        ColorUIResource cardBorder = dark ? c(52, 58, 67) : border;

        // ── App surfaces ───────────────────────────────────────────────────
        UIManager.put("Panel.background",    appBg);
        UIManager.put("Viewport.background", appBg);
        // JOptionPane dialogs must sit on the same application background as
        // every other window; FlatLaf's own default is a different gray and
        // makes message dialogs visibly lighter, especially in dark mode.
        UIManager.put("OptionPane.background", appBg);
        UIManager.put("TitledBorder.border", BorderFactory.createLineBorder(cardBorder));

        // ── Text ───────────────────────────────────────────────────────────
        UIManager.put("Label.foreground",         text);
        UIManager.put("TextField.foreground",     text);
        UIManager.put("TextArea.foreground",      text);
        UIManager.put("PasswordField.foreground", text);
        UIManager.put("Label.disabledForeground", text2);

        // ── Inputs ─────────────────────────────────────────────────────────
        UIManager.put("TextField.background",         inputBg);
        UIManager.put("TextArea.background",          inputBg);
        UIManager.put("PasswordField.background",     inputBg);
        UIManager.put("TextField.inactiveBackground", panelBg);
        // padding key preserves FlatBorder — keeps outline/error ring working
        UIManager.put("TextField.padding",            new Insets(6, 10, 6, 10));
        UIManager.put("Component.borderColor",        border);
        UIManager.put("Component.disabledBorderColor", dark ? c(50, 56, 65) : c(210, 214, 220));

        // ── Tables / Lists ─────────────────────────────────────────────────
        UIManager.put("Table.background",          panelBg);
        UIManager.put("Table.foreground",          text);
        UIManager.put("Table.selectionBackground", selBg);
        UIManager.put("Table.selectionForeground", text);
        UIManager.put("Table.gridColor",           cardBorder);
        UIManager.put("List.selectionBackground",  selBg);
        UIManager.put("List.selectionForeground",  text);

        // ── Focus / Accent ─────────────────────────────────────────────────
        UIManager.put("Component.accentColor",                  accent);
        UIManager.put("Component.focusColor",                   accent);
        UIManager.put("Component.focusedBorderColor",           accent);
        UIManager.put("ProgressBar.foreground",                 accent);
        UIManager.put("CheckBox.icon.selectedBackground",       accent);
        UIManager.put("CheckBox.icon.focusedSelectedBackground", accent2);
        UIManager.put("Hyperlink.linkColor",                    accent);

        // ── Popup / menu selection — subtle turquoise-tinted selection instead
        // of FlatLaf's derived blue, kept readable in both themes. ─────────────
        UIManager.put("PopupMenu.background",          panelBg);
        UIManager.put("MenuItem.selectionBackground",  selBg);
        UIManager.put("MenuItem.selectionForeground",  text);
        UIManager.put("Menu.selectionBackground",      selBg);
        UIManager.put("Menu.selectionForeground",      text);
        UIManager.put("MenuItem.underlineSelectionColor", accent);
        UIManager.put("MenuItem.acceleratorForeground",   text2);

        // ── Combo boxes — FlatLaf's dropdown list uses its own ComboBox.* keys
        // (not MenuItem.*), so without these the combo popups keep the derived
        // blue selection and a different surface than the other menus/inputs. ──
        UIManager.put("ComboBox.background",               inputBg);
        UIManager.put("ComboBox.buttonBackground",         inputBg);
        UIManager.put("ComboBox.buttonEditableBackground", inputBg);
        UIManager.put("ComboBox.popupBackground",          panelBg);
        UIManager.put("ComboBox.selectionBackground",      selBg);
        UIManager.put("ComboBox.selectionForeground",      text);

        // ── Semantic status colors — calm, professional, harmonized with the
        // turquoise/charcoal palette. Tuned per theme for contrast; the accent
        // (turquoise) is deliberately NOT reused as the success color. ─────────
        UIManager.put("App.successColor", dark ? c(110, 178, 130) : c(46, 122, 66));
        UIManager.put("App.errorColor",   dark ? c(224, 113, 110) : c(198, 55,  55));
        UIManager.put("App.warningColor", dark ? c(214, 168, 75)  : c(154, 103, 0));
        UIManager.put("App.infoColor",    dark ? c(96,  164, 214) : c(33,  102, 168));

        // ── Buttons — neutral defaults, accent only for primary ────────────
        UIManager.put("Button.background",          panelBg);
        UIManager.put("Button.foreground",          text);
        UIManager.put("Button.disabledBackground",  disBg);
        UIManager.put("Button.disabledForeground",  disFg);
        UIManager.put("Button.margin",              new Insets(7, 14, 7, 14));

        UIManager.put("Button.default.margin",            new Insets(7, 16, 7, 16));
        UIManager.put("Button.default.background",        accent);
        UIManager.put("Button.default.foreground",        accentFg);
        UIManager.put("Button.default.hoverBackground",   accent2);
        UIManager.put("Button.default.pressedBackground", accent2);
        UIManager.put("Button.default.focusedBackground", accent2);

        // ── Button borders & focus — eliminate FlatLaf's derived blue ──────
        // FlatLaf derives these border/focus/hover keys from its built-in blue
        // accent at LAF-install time. Setting Component.accentColor afterwards
        // does NOT recompute them, so we override each one explicitly here.
        // Secondary buttons: neutral border in normal/hover; subtle accent only
        // for real keyboard focus.
        UIManager.put("Button.borderColor",                border);
        UIManager.put("Button.hoverBorderColor",           border);
        UIManager.put("Button.focusedBorderColor",         accent);
        // Default (primary) button is filled with accent: blend its border and
        // hover border into the fill so there is NO permanent colored outline,
        // and reserve a subtle accent2 border for real keyboard focus only.
        UIManager.put("Button.default.borderColor",        accent);
        UIManager.put("Button.default.hoverBorderColor",   accent2);
        UIManager.put("Button.default.focusColor",         accent2);
        UIManager.put("Button.default.focusedBorderColor", accent2);
        // Help "?" button keys (also derived from blue)
        UIManager.put("HelpButton.focusedBorderColor",     accent);
        UIManager.put("HelpButton.hoverBorderColor",       border);

        // Toolbar (icon) buttons— make the hover/pressed feedback clearly
        // visible against the header surface in both themes.
        UIManager.put("Button.toolbar.hoverBackground",
                dark ? c(63, 70, 82)  : c(232, 235, 240));
        UIManager.put("Button.toolbar.pressedBackground",
                dark ? c(78, 86, 100) : c(221, 225, 232));

        // ── Custom app keys ────────────────────────────────────────────────
        UIManager.put("App.cardBackground",  panelBg);
        UIManager.put("App.borderColor",     cardBorder);   // header / footer rule
        UIManager.put("App.cardBorderColor", cardBorder);   // card / tile outlines
        UIManager.put("Separator.foreground", cardBorder);
        // Stat tile background: soft in light, slightly recessed in dark
        UIManager.put("App.tileBackground",
                dark ? c(34, 38, 45) : c(248, 250, 252));
    }

    private static ColorUIResource c(int r, int g, int b) {
        return new ColorUIResource(new Color(r, g, b));
    }
}