package com.pcariou.view.custom;

import javax.swing.AbstractButton;
import javax.swing.JLabel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Consistent behaviour for clickable text links across the application.
 *
 * <p>Links show a hand cursor and underline on hover. Hover only toggles the
 * underline (drawn within the existing text bounds); it never changes the font
 * size, weight, padding or preferred width, so the surrounding layout never
 * shifts when the pointer moves over a link. The base colour set by the caller
 * (via a FlatLaf {@code STYLE} client property or {@code setForeground}) is left
 * untouched so theme switches keep working.
 */
public final class Links {

    private Links() {
    }

    /** Applies the hand cursor to any clickable component (buttons, icon buttons, links). */
    public static <C extends Component> C handCursor(C component) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return component;
    }

    /** Hand cursor + underline-on-hover for a label styled as a link. */
    public static JLabel asLink(JLabel label) {
        handCursor(label);
        addHoverUnderline(label);
        return label;
    }

    /** Hand cursor + underline-on-hover for a button styled as a link. */
    public static AbstractButton asLink(AbstractButton button) {
        handCursor(button);
        addHoverUnderline(button);
        return button;
    }

    private static void addHoverUnderline(final Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setUnderlined(component, true); }
            @Override public void mouseExited(MouseEvent e)  { setUnderlined(component, false); }
        });
    }

    private static void setUnderlined(Component component, boolean underlined) {
        Font font = component.getFont();
        if (font == null) {
            return;
        }
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE,
                underlined ? TextAttribute.UNDERLINE_ON : Integer.valueOf(-1));
        component.setFont(font.deriveFont(attributes));
    }
}
