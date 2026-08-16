package com.pcariou.view.help;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.custom.Links;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Small shared behaviour for the Help dialogs, so each one does not re-implement
 * the same keyboard and link conventions.
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

    /**
     * A text action that presents itself as a hyperlink and opens {@code url} in
     * the browser.
     *
     * <p>Styling and hover behaviour are the ones already used by the application
     * footer - accent foreground, one step smaller, hand cursor and underline on
     * hover, all through the shared {@link Links} helper - so links look and feel
     * the same wherever they appear. Unlike the footer the focus ring is kept,
     * because these dialogs are small keyboard-reachable surfaces where the user
     * must be able to see which link is about to be activated.</p>
     */
    static JButton linkButton(String text, final String url, final Window owner) {
        JButton link = new JButton(text);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setOpaque(false);
        link.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        link.addActionListener(e -> ExternalLinks.open(url, owner));
        Links.asLink(link);
        return link;
    }
}
