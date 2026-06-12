package com.pcariou.view.custom;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * Shared card styling used across the application (main form, summary,
 * settings). Cards are rounded panels using the {@code App.cardBackground}
 * and {@code App.cardBorderColor} UIManager keys and re-style themselves on
 * every look-and-feel change so light/dark theme switches stay consistent.
 */
public final class Cards {

    private Cards() {
    }

    /** Returns a new card panel that refreshes its background and border on every L&F change. */
    public static JPanel createCard(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override public void updateUI() {
                super.updateUI();
                refreshCardAppearance(this);
            }
        };
        card.putClientProperty(FlatClientProperties.STYLE, "arc:16");
        refreshCardAppearance(card);
        return card;
    }

    public static void refreshCardAppearance(JPanel card) {
        card.setOpaque(true);
        Color bg = UIManager.getColor("App.cardBackground");
        if (bg != null) card.setBackground(bg);
        Color border = UIManager.getColor("App.cardBorderColor");
        if (border == null) border = UIManager.getColor("Separator.foreground");
        if (border != null) card.setBorder(roundedBorder(border, 16));
    }

    /** Anti-aliased rounded-rect border — matches FlatLaf arc radius. */
    public static AbstractBorder roundedBorder(Color color, int arc) {
        return new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
        };
    }
}
