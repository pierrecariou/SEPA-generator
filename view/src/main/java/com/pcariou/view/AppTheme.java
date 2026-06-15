package com.pcariou.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.pcariou.view.config.ConfigStore;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

public final class AppTheme {
    private static final ConfigStore CONFIG = new ConfigStore();
    private static Mode currentMode = Mode.LIGHT;

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
        } catch (Exception e) {
            e.printStackTrace();
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

        // ── Status colors (footer) — tuned per theme for contrast ──────────
        UIManager.put("App.successColor", dark ? c(102, 187, 106) : c(46, 125, 50));
        UIManager.put("App.errorColor",   dark ? c(239, 83,  80)  : c(211, 47, 47));

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

        // Toolbar (icon) buttons — make the hover/pressed feedback clearly
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