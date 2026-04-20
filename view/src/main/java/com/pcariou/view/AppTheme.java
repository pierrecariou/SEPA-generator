package com.pcariou.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lombok.Getter;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

public final class AppTheme {
    @Getter private static Mode currentMode = Mode.LIGHT;

    public enum Mode { LIGHT, DARK }

    private AppTheme() {}

    public static void apply(Mode mode) {
        try {
            currentMode = mode;

            // 1) Setup base LAF
            if (mode == Mode.DARK) FlatDarkLaf.setup();
            else FlatLightLaf.setup();

            // 2) Apply our overrides (grey-first + accent)
            applyDefaults(mode);

            // 3) Round corners a bit (modern but classy)
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 10);

            // Refresh existing windows
            FlatLaf.updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void switchMode() {
        apply(currentMode == Mode.LIGHT ? Mode.DARK : Mode.LIGHT);
    }

    private static void applyDefaults(Mode mode) {
        boolean dark = (mode == Mode.DARK);


        // ---- Grey-first palette v2 ----
        // Light
        ColorUIResource lightAppBg   = c(232, 235, 239); // #E8EBEF
        ColorUIResource lightPanel   = c(243, 244, 246); // #F3F4F6
        ColorUIResource lightInputBg = c(250, 250, 251); // #FAFAFB
        ColorUIResource lightBorder  = c(201, 206, 214); // #C9CED6
        ColorUIResource lightText    = c(21,  22,  26);  // #15161A
        ColorUIResource lightText2   = c(84,  90,  102); // #545A66
        ColorUIResource lightSelBg   = c(217, 222, 230); // #D9DEE6

        // Dark (lighter charcoal)
        ColorUIResource darkAppBg    = c(36,  39,  44);  // #24272C
        ColorUIResource darkPanel    = c(44,  48,  54);  // #2C3036
        ColorUIResource darkInputBg  = c(50,  55,  65);  // #323741
        ColorUIResource darkBorder   = c(69,  75,  86);  // #454B56
        ColorUIResource darkText     = c(236, 239, 244); // #ECEFF4
        ColorUIResource darkText2    = c(184, 190, 201); // #B8BEC9
        ColorUIResource darkSelBg    = c(58,  64,  75);  // #3A404B

        ColorUIResource appBg   = dark ? darkAppBg   : lightAppBg;
        ColorUIResource panelBg = dark ? darkPanel   : lightPanel;
        ColorUIResource inputBg = dark ? darkInputBg : lightInputBg;
        ColorUIResource border  = dark ? darkBorder  : lightBorder;
        ColorUIResource text    = dark ? darkText    : lightText;
        ColorUIResource text2   = dark ? darkText2   : lightText2;
        ColorUIResource selBg   = dark ? darkSelBg   : lightSelBg;

        // ---- Warm Amber accent ----
        // Light accent: #C98A12 (201, 138, 18)
        // Dark accent:  #F2B84B (242, 184, 75)  <-- your value
        ColorUIResource accent  = dark ? c(242, 184, 75) : c(201, 138, 18);
        ColorUIResource accent2 = dark ? c(220, 165, 65) : c(169, 114, 14); // hover/pressed

        // ---- App surfaces ----
        UIManager.put("Panel.background", appBg);
        UIManager.put("Viewport.background", appBg);

        // Card-like containers
        UIManager.put("TitledBorder.border", BorderFactory.createLineBorder(border));

        // ---- Text ----
        UIManager.put("Label.foreground", text);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextArea.foreground", text);
        UIManager.put("PasswordField.foreground", text);

        // Secondary text (we'll use explicitly for smaller labels later)
        UIManager.put("Label.disabledForeground", text2);

        // ---- Inputs ----
        UIManager.put("TextField.background", inputBg);
        UIManager.put("TextArea.background", inputBg);
        UIManager.put("PasswordField.background", inputBg);
        UIManager.put("TextField.inactiveBackground", panelBg);

        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // ---- Tables / Lists ----
        UIManager.put("Table.background", panelBg);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", selBg);
        UIManager.put("Table.selectionForeground", text);
        UIManager.put("Table.gridColor", border);

        UIManager.put("List.selectionBackground", selBg);
        UIManager.put("List.selectionForeground", text);

        // ---- Focus / Accent ----
        UIManager.put("Component.focusColor", accent);
        UIManager.put("Component.focusedBorderColor", accent);
        UIManager.put("ProgressBar.foreground", accent);

        // ---- Buttons ----
        UIManager.put("Button.background", panelBg);
        UIManager.put("Button.foreground", text);
//        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(border),
//                BorderFactory.createEmptyBorder(7, 14, 7, 14)
//        ));
        UIManager.put("Button.margin", new Insets(7, 14, 7, 14)); // FlatLaf uses margin instead of border for padding

        // Default (primary) button: use accent
        UIManager.put("Button.default.margin", new Insets(7, 16, 7, 16)); // slightly wider padding for primary buttons
        UIManager.put("Button.default.background", accent);
        UIManager.put("Button.default.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("Button.default.hoverBackground", accent2);
        UIManager.put("Button.default.pressedBackground", accent2);

        // Optional: slightly tint panels used as “cards”
        UIManager.put("App.cardBackground", panelBg); // custom key you can use in your code
        UIManager.put("App.borderColor", border); // custom key for borders

        UIManager.put("Separator.foreground", border);
    }

    private static ColorUIResource c(int r, int g, int b) {
        return new ColorUIResource(new Color(r, g, b));
    }
}

