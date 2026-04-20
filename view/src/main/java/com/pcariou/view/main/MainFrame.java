package com.pcariou.view.main;

import com.pcariou.view.IGenerator;
import com.pcariou.view.main.center.FormPanel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Main application window.
 *
 * Header: settings
 * Center: form panel (file/date inputs + results)
 * Footer: generate button
 */
public class MainFrame extends JFrame {
	@Getter private final IGenerator generator;
	private final FormPanel formPanel;

    public MainFrame(IGenerator generator, String version) {
        super("SEPA Generator v" + version);
        this.generator = generator;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(new HeaderPanel(this), BorderLayout.NORTH);

        formPanel = new FormPanel(this);
        add(formPanel, BorderLayout.CENTER);

        add(new FooterPanel(this, formPanel), BorderLayout.SOUTH);

        // 1) Compute natural layout size first
        pack();

        // Keep the smallest allowed size = what the layout truly needs
        Dimension packed = getSize();
        setMinimumSize(packed);

        // 2) Compute usable screen size (accounts for taskbar/dock)
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }

        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int usableW = bounds.width  - insets.left - insets.right;
        int usableH = bounds.height - insets.top  - insets.bottom;

        // 3) Choose a "nice default" but clamp to screen & never smaller than packed
        int baseW = 900;
        int baseH = 540;

        int maxW = (int) (usableW * 0.92);
        int maxH = (int) (usableH * 0.88);

        int targetW = Math.max(packed.width,  Math.min(baseW, maxW));
        int targetH = Math.max(packed.height, Math.min(baseH, maxH));

        setSize(targetW, targetH);

        setResizable(true);
        setLocationRelativeTo(null);
        setVisible(true);
    }

	public void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	public void showSuccessMessage(String outputFilePath, String message) {
//		formPanel.showSuccessMessage(outputFilePath, message);
	}

	public void showTableResult(List<String> resultList) {
//		formPanel.showTableResult(resultList);
	}
}
