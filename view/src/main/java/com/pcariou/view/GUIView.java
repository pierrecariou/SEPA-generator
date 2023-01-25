package com.pcariou.view;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.io.File;

/**
 * GUIView class
 *
 */
public class GUIView extends JFrame
{
	private JButton buttonSelectInputFile;
	private JButton buttonSelectOutputFile;
	private JButton buttonGenerate;
	private String filenameInput;
	private String filenameOutput;
	private JLabel labelMessageSuccess;

	private JPanel GUIPanel;
	private JPanel leftPanel;
	private JPanel rightPanel;

	private IGenerator generator;
	
	public GUIView(IGenerator generator)
	{
		super("Sepa Generator - Version 1.0");
		this.generator = generator;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(dim.width/2, dim.height/2);
        setLocation(dim.width/2 - getWidth()/2, dim.height/2 - getHeight()/2);
        setVisible(true);
		initComponents();
		//pack();
		revalidate();
		repaint();
	}

	public void showErrorMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	public void showSuccessMessage(String message)
	{
		labelMessageSuccess.setText(message);
		labelMessageSuccess.setVisible(true);
	}

	private void initComponents()
	{
		GUIPanel = new JPanel();
		add(GUIPanel, BorderLayout.CENTER);

		GUIPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		GUIPanel.setBackground(new Color(248, 248, 248));

		leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		leftPanel.setBackground(new Color(248, 248, 248));
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		JTextField textField = new JTextField();
		designTextField(textField, "Credit Transfer File");
		panel.add(textField);

		buttonSelectInputFile = new JButton("choose");

		designButton(buttonSelectInputFile);
		buttonSelectInputFile.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select file with transactions");
			fileChooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV, XLS and XLSX files", "csv", "xls", "xlsx");
			fileChooser.addChoosableFileFilter(filter);
			int fileOption = fileChooser.showOpenDialog(this);

			if (fileOption == JFileChooser.APPROVE_OPTION) {
				filenameInput = fileChooser.getSelectedFile().getAbsolutePath();
				textField.setText(filenameInput);
			}
		});
		buttonSelectInputFile.setPreferredSize(new Dimension(100, 30));
		panel.add(buttonSelectInputFile, gbc);
		
		leftPanel.add(panel, gbc);


		JPanel panel1 = new JPanel();
		panel1.setLayout(new GridBagLayout());

		JTextField textField1 = new JTextField();
		designTextField(textField1, "SEPA Output File");
		panel1.add(textField1);

		buttonSelectOutputFile = new JButton("choose");
		designButton(buttonSelectOutputFile);
		buttonSelectOutputFile.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select the SEPA output file");
			fileChooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.addChoosableFileFilter(filter);
			int fileOption = fileChooser.showOpenDialog(this);

			if (fileOption == JFileChooser.APPROVE_OPTION) {
				filenameOutput = fileChooser.getSelectedFile().getAbsolutePath();
				textField1.setText(filenameOutput);
			}
		});
		buttonSelectOutputFile.setPreferredSize(new Dimension(100, 30));
		panel1.add(buttonSelectOutputFile, gbc);

		leftPanel.add(panel1, gbc);
		panel1.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		gbc.insets = new Insets(40, 0, 0, 0);

		buttonGenerate = new JButton("Generate");
		designButton(buttonGenerate);
		buttonGenerate.addActionListener(e -> {
			labelMessageSuccess.setVisible(false);
			generator.generate(filenameInput, filenameOutput);
		});

		leftPanel.add(buttonGenerate, gbc);

		gbc.insets = new Insets(10, 0, 0, 0);

		rightPanel = new JPanel();
		rightPanel.setBackground(new Color(248, 248, 248));

		try {
			ImageIcon icon = new ImageIcon(ImageIO.read(new File("view/resources/logo_DigistratConsulting.png")));
			JLabel label = new JLabel(icon);
			rightPanel.add(label, gbc);
		} catch (Exception e) {
			e.printStackTrace();
		}

		GUIPanel.add(leftPanel, gbc);
		add(rightPanel, BorderLayout.PAGE_END);

		labelMessageSuccess = new JLabel();
		labelMessageSuccess.setForeground(new Color(81, 81, 81));
		labelMessageSuccess.setVisible(false);
		leftPanel.add(labelMessageSuccess, gbc);
	}

	private void designButton(JButton button)
	{
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setForeground(new Color(81, 81, 81));
		button.setBackground(new Color(95, 178, 244));
	}

	private void designTextField(JTextField textField, String text)
	{
		textField.setPreferredSize(new Dimension(500, 30));
		textField.setEditable(false);
		textField.setBackground(Color.WHITE);
		textField.setForeground(new Color(81, 81, 81));
		textField.setText(text);
	}

}
