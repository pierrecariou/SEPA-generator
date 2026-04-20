package com.pcariou.view.common;

import javax.swing.*;
import java.awt.*;

public class UI {
    public static void applyFieldPadding(JTextField tf) {
        tf.setMargin(new Insets(0, 10, 0, 10)); // left/right padding
    }

    public static Dimension getJButtonDimension() {
        return new JButton().getPreferredSize();
    }

}
