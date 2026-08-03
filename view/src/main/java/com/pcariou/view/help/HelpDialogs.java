package com.pcariou.view.help;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Small shared behaviour for the Help dialogs, so each one does not re-implement
 * the same keyboard convention.
 */
final class HelpDialogs {

    private static final String CLOSE_ACTION = "help.close";

    private HelpDialogs() {
    }

    /** Makes {@code Esc} close {@code dialog}, as expected from a read-only dialog. */
    static void closeOnEscape(final JDialog dialog) {
        if (dialog == null) {
            return;
        }
        JComponent root = dialog.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_ACTION);
        root.getActionMap().put(CLOSE_ACTION, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
    }
}
