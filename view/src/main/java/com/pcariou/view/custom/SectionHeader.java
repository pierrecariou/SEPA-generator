package com.pcariou.view.custom;

import com.formdev.flatlaf.FlatClientProperties;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * A restrained, JetBrains-style section header used to group related rows on a
 * form: a compact bold title followed by a thin 1px separator rule that fills the
 * remaining width. It carries no border, fill, or shadow so it reads as
 * hierarchy, not as another card.
 *
 * <p>The title font is applied through a FlatLaf {@code STYLE} client property so
 * it survives live light/dark theme switches, and the rule resolves
 * {@code App.borderColor} at paint time so it re-tints automatically.
 */
public final class SectionHeader extends JPanel {

    private final String title;

    public SectionHeader(String title) {
        super(new MigLayout("insets 0, fillx", "[]10[grow,fill]", "[]"));
        this.title = title;
        setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold 13");

        add(titleLabel, "aligny center");
        add(new Rule(), "growx, h 1!, aligny center");
    }

    /** The section title text, e.g. {@code "Input"}. */
    public String getTitle() {
        return title;
    }

    /** A 1px horizontal rule tinted with the current theme's border colour. */
    private static final class Rule extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Color c = UIManager.getColor("App.borderColor");
            if (c == null) c = UIManager.getColor("Separator.foreground");
            if (c == null) return;
            g.setColor(c);
            int y = getHeight() / 2;
            g.drawLine(0, y, getWidth() - 1, y);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(10, 1);
        }
    }
}
