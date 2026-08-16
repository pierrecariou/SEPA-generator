package com.pcariou.view.help;

import javax.swing.AbstractButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;

/**
 * Test helper: flattens a Swing component tree into the text a user would see,
 * following the convention already used by the other view tests.
 */
final class HelpDialogText {

    private HelpDialogText() {
    }

    /** All label, button and text-component text under {@code root}. */
    static String of(Component root) {
        StringBuilder text = new StringBuilder();
        collect(root, text);
        return text.toString();
    }

    private static void collect(Component component, StringBuilder out) {
        if (component instanceof JTextComponent) {
            out.append(((JTextComponent) component).getText()).append('\n');
        } else if (component instanceof AbstractButton) {
            append(((AbstractButton) component).getText(), out);
        } else if (component instanceof JLabel) {
            append(((JLabel) component).getText(), out);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collect(child, out);
            }
        }
    }

    private static void append(String text, StringBuilder out) {
        if (text != null) {
            out.append(text).append('\n');
        }
    }

    /** Whether a scroll pane exists anywhere under {@code root}. */
    static boolean containsScrollPane(Component root) {
        return firstScrollPane(root) != null;
    }

    /** The first button whose text equals {@code text}, or {@code null}. */
    static AbstractButton button(Component component, String text) {
        if (component instanceof AbstractButton
                && text.equals(((AbstractButton) component).getText())) {
            return (AbstractButton) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                AbstractButton found = button(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** The first {@link JEditorPane} under {@code root}, or {@code null}. */
    static JEditorPane editorPane(Component component) {
        if (component instanceof JEditorPane) {
            return (JEditorPane) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JEditorPane found = editorPane(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Every {@link JLabel} under {@code root}, in tree order. */
    static java.util.List<JLabel> labels(Component root) {
        java.util.List<JLabel> found = new java.util.ArrayList<>();
        collectLabels(root, found);
        return found;
    }

    private static void collectLabels(Component component, java.util.List<JLabel> out) {
        if (component instanceof JLabel) {
            out.add((JLabel) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectLabels(child, out);
            }
        }
    }

    /** The first scroll pane under {@code root}, or {@code null}. */
    static Component firstScrollPane(Component component) {
        if (component instanceof JScrollPane) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component found = firstScrollPane(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
