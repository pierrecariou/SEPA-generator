package com.pcariou.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

/**
 * Shared layout and error-handling behaviour for the settings form panels.
 *
 * Holds the GridBag constraint factories, the error-label styling and the
 * apply/clear-error helpers so concrete panels only declare their own fields
 * and rows. The layout (insets, label column width, anchors) is intentionally
 * identical across panels so the two cards line up.
 */
public abstract class AbstractSettingsPanel extends JPanel {

    /** Fixed label column width so all settings panels align. */
    protected static final int LABEL_WIDTH = 70;

    protected GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 0;
        c.weightx = 0;
        c.fill    = GridBagConstraints.NONE;
        c.anchor  = GridBagConstraints.LINE_END;
        c.insets  = new Insets(6, 8, 0, 6);
        c.ipadx   = LABEL_WIDTH;
        return c;
    }

    protected GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 1;
        c.weightx = 1.0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.LINE_START;
        c.insets  = new Insets(6, 0, 0, 8);
        return c;
    }

    protected GridBagConstraints errorConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 1;
        c.weightx = 1.0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.LINE_START;
        c.insets  = new Insets(0, 0, 4, 8);
        return c;
    }

    protected void configureErrorLabel(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(UIManager.getColor("Actions.Red") != null
                ? UIManager.getColor("Actions.Red")
                : new Color(0xCC0000));
        label.setVisible(false);
    }

    protected void applyError(JTextField field, JLabel errorLabel, String message) {
        field.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        field.repaint();
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    protected void clearError(JTextField field, JLabel errorLabel) {
        field.putClientProperty(FlatClientProperties.OUTLINE, null);
        field.repaint();
        errorLabel.setVisible(false);
    }
}
