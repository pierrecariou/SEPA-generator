package com.pcariou.view;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.toedter.calendar.JDateChooser;

import java.util.List;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;

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
	private JDateChooser dateChooser;

	private JPanel GUIPanel;
	private JPanel leftPanel;
	private JPanel rightPanel;

	private JTable tableResult;
	//private JScrollPane scrollPane;
	private static final String[] columnNames =
		{"File name", "Number of transactions", "Total amount", "Execution date"};

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

	public void showTableResult(List<String> resultList)
	{
		resultList.add(0, Path.of(filenameOutput).getFileName().toString());

		Object[][] data = new Object[1][columnNames.length];
		for (int i = 0; i < columnNames.length; i++)
			data[0][i] = resultList.get(i);
		//tableResult = new JTable(data, columnNames);
		tableResult.setModel(new DefaultTableModel(data, columnNames));
		// center the text in the table
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		//tableResult.setDefaultRenderer(String.class, centerRenderer);
		for (int i = 0; i < columnNames.length; i++)
			tableResult.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		tableResult.setVisible(true);
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

		buttonSelectInputFile = new JButton("Browse");

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

		buttonSelectOutputFile = new JButton("Browse");
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


		//select date for execution date of payments with JDatePicker

		JPanel panel2 = new JPanel();
		panel2.setLayout(new GridBagLayout());	

		JLabel labelExecutionDate = new JLabel("Execution Date:   ");
		//labelExecutionDate.setFont(new Font("Arial", Font.BOLD, 14));
		labelExecutionDate.setForeground(new Color(81, 81, 81));
		panel2.add(labelExecutionDate);

		dateChooser = new JDateChooser();
		dateChooser.setDateFormatString("yyyy/MM/dd");
		dateChooser.setPreferredSize(new Dimension(200, 30));
		dateChooser.setBackground(new Color(95, 178, 254));
		//dateChooser.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		//dateChooser.setOpaque(true);
		dateChooser.setForeground(new Color(81, 81, 81));
		panel2.add(dateChooser);

		leftPanel.add(panel2, gbc);
		panel2.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));


		gbc.insets = new Insets(40, 0, 0, 0);

		buttonGenerate = new JButton("Generate");
		designButton(buttonGenerate);
		buttonGenerate.addActionListener(e -> {
			labelMessageSuccess.setVisible(false);
			tableResult.setVisible(false);
			generator.generate(filenameInput, filenameOutput, dateChooser.getDate());
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
		
		tableResult = new JTable();
		tableResult.setEnabled(false);

		leftPanel.add(tableResult.getTableHeader(), gbc);
		gbc.insets = new Insets(0, 0, 0, 0);
		leftPanel.add(tableResult, gbc);

		tableResult.setVisible(false);
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
