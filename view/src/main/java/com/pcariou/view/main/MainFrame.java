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
		super("SEPA Generator" + " v" + version);
		this.generator = generator;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 540));
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setResizable(true);
//		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
//		setSize(dim.width / 2, (int) (dim.height / 1.7));
//		setLocation(dim.width / 2 - getWidth() / 2, dim.height / 2 - getHeight() / 2);

		setLayout(new BorderLayout());
		add(new HeaderPanel(this), BorderLayout.NORTH);

		formPanel = new FormPanel(this);
		add(formPanel, BorderLayout.CENTER);

		add(new FooterPanel(this, formPanel), BorderLayout.SOUTH);

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
