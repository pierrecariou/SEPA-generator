package com.pcariou.view.main.center;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import com.pcariou.view.custom.FlatDatePickerField;
import com.pcariou.view.main.MainFrame;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class FormPanel extends JPanel {

    private final MainFrame owner;

    private JTextField inputField;
    private FlatDatePickerField flatDatePickerField;
    private JButton generateButton;

    private JLabel successLabel;
    private JTable tableResult;
    private JScrollPane tableScroll;

    @Getter
    private String filenameInput;
    @Getter
    private String filenameOutput;

    private static final String[] COLUMN_NAMES = {
            "File name", "Number of transactions", "Total amount", "Execution date"
    };

    public FormPanel(MainFrame owner) {
        super(new MigLayout("fill, insets 24", "[grow,center]", "[grow,top]"));
        this.owner = owner;

        add(createCard(), "w 760::, growx, top"); // max-width-ish card centered
    }

    private JComponent createCard() {
        JPanel card = new JPanel(new MigLayout(
                "fillx, insets 18, hidemode 3",
                "[grow,fill]",
                "[]12[]12[]12[grow]"
        ));

        // Background + border
        card.setOpaque(true);
        Color bg = UIManager.getColor("App.cardBackground");
        if (bg != null) card.setBackground(bg);

        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = UIManager.getColor("Separator.foreground");
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        // Optional: FlatLaf per-component styling
        card.putClientProperty(FlatClientProperties.STYLE, "arc:16");

        card.add(createCardHeader(), "growx, wrap");
        card.add(createFormGrid(), "growx, wrap");
        card.add(createSuccessArea(), "growx, wrap");
        card.add(createResultArea(), "grow, pushy");
        card.add(createActionsRow(), "growx, wrap");

        return card;
    }

    private JComponent createActionsRow() {
        JPanel p = new JPanel(new MigLayout("insets 0", "[grow][][]", "[]"));
        p.setOpaque(false);

        // Optional secondary actions (can remove)
        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> resetAll());

        JButton generate = new JButton("Generate");
        // FlatLaf: make it look like a primary modern button
        generate.putClientProperty("JButton.buttonType", "roundRect");
        generate.putClientProperty("JComponent.sizeVariant", "large");
        generate.putClientProperty("FlatLaf.style", "minimumWidth:140; minimumHeight:36;");

        generate.addActionListener(e -> onGenerate());

        // Enable/disable based on validation
        generate.setEnabled(false);
        this.generateButton = generate; // store it as a field

        p.add(new JLabel(), "growx, pushx");
        p.add(reset, "gapright 8");
        p.add(generate, "right");

        owner.getRootPane().setDefaultButton(generate);

        // Make Enter trigger Generate
        SwingUtilities.invokeLater(() -> {
            JRootPane rp = SwingUtilities.getRootPane(this);
            if (rp != null) rp.setDefaultButton(generate);
        });

        return p;
    }

    private void onGenerate() {
        Path input = Paths.get(filenameInput);
        String suggestedOutput = FilenameUtils.getBaseName(filenameInput) + ".xml";

        JFileChooser fc = new JFileChooser(input.getParent().toFile());
        fc.setDialogTitle("Save SEPA XML");
        fc.setSelectedFile(new File(input.getParent().toFile(), suggestedOutput));
        fc.setFileFilter(new FileNameExtensionFilter("XML", "xml"));

        if (fc.showSaveDialog(parentWindow()) != JFileChooser.APPROVE_OPTION)
            return;

        File f = fc.getSelectedFile();
        filenameOutput = f.getAbsolutePath();

        owner.getGenerator().generate(
                filenameInput,
                filenameOutput,
                flatDatePickerField.getDate()
        );
        showSuccessMessage(filenameOutput, "Done");
    }

    private void resetAll() {
    }

    private JComponent createCardHeader() {
        JPanel p = new JPanel(new MigLayout("insets 0", "[grow,fill]", "[]2[]"));
        p.setOpaque(false);

        JLabel title = new JLabel("Generate SEPA XML");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subtitle = new JLabel("Select input and execution date.");
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));

        p.add(title, "wrap");
        p.add(subtitle);

        return p;
    }

    private JComponent createFormGrid() {
        JPanel grid = new JPanel(new MigLayout(
                "insets 0, fillx, gapx 12, gapy 10",
                "[right]12[grow,fill]",
                "[]10[]"
        ));
        grid.setOpaque(false);

        // Input
        inputField = createFileField("No input selected");
        JButton browseInput = new JButton("...");
        browseInput.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, browseInput);
        browseInput.addActionListener(e -> chooseInputFile());
        JLabel inputLabel = new JLabel("Credit transfer");
        inputLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        grid.add(inputLabel, "alignx right");
        grid.add(inputField, "growx, wrap");

        // Output
//        outputField = createFileField("No output selected");
//        JButton browseOutput = new JButton("Browse…");
//        browseOutput.addActionListener(e -> chooseOutputFile());
//        grid.add(new JLabel("SEPA output"), "alignx right");
//        grid.add(outputField, "growx");
//        grid.add(browseOutput, "wrap");

        // Date
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        flatDatePickerField = new FlatDatePickerField(tomorrow, LocalDate.now().plusYears(5), browseInput.getPreferredSize());
        flatDatePickerField.setDate(tomorrow);
        JLabel dateLabel = new JLabel("Execution date");
        dateLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        grid.add(dateLabel, "alignx right");
        grid.add(flatDatePickerField, "growx, wrap");

        return grid;
    }

    private JTextField createFileField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setFocusable(false);
//        tf.setColumns(24);
        tf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // FlatLaf placeholder
        tf.putClientProperty("JTextField.placeholderText", placeholder);

        return tf;
    }

    private void configureDatePicker(DatePicker dp) {
//        JTextField dtf = dp.getComponentDateTextField();
//        JButton originalBtn = dp.getComponentToggleCalendarButton();
//
// Make it look like a normal FlatLaf TextField
//        dtf.setBorder(UIManager.getBorder("TextField.border"));
//        dtf.setBackground(UIManager.getColor("TextField.background"));
//        dtf.setForeground(UIManager.getColor("TextField.foreground"));
//        dtf.setOpaque(true);
//
//        dtf.setEditable(false);
//        dp.getSettings().setAllowKeyboardEditing(false);
//
//        originalBtn.setVisible(false);

// Make the calendar button look like an embedded trailing button
//        JButton calendarBtn = new JButton("...");
//        calendarBtn.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
//                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
//        calendarBtn.setFocusable(false);
//        calendarBtn.addActionListener(e -> originalBtn.doClick());
//        dtf.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, calendarBtn);

            JTextField dtf = dp.getComponentDateTextField();
            JButton originalBtn = dp.getComponentToggleCalendarButton();

            // Make the date text field look like FlatLaf TextField
            dtf.setBorder(UIManager.getBorder("TextField.border"));
            dtf.setBackground(UIManager.getColor("TextField.inactiveBackground")); // use inactive background for non-editable feel
            dtf.setForeground(UIManager.getColor("TextField.inactiveForeground"));
            dtf.setOpaque(true);

            // Same behavior as your file fields
            dtf.setEditable(false);
            dp.getSettings().setAllowKeyboardEditing(false);

            // --- IMPORTANT PART ---
            // Detach the original button from the DatePicker UI (but keep the SAME instance)
            Container parent = originalBtn.getParent();
            if (parent != null) {
                parent.remove(originalBtn);
                parent.revalidate();
                parent.repaint();
            }

            // Style it like an embedded trailing button
            originalBtn.setText("…");          // optional (or keep icon)
            originalBtn.setIcon(null);         // optional
            originalBtn.setFocusable(false);
            originalBtn.putClientProperty(
                    FlatClientProperties.BUTTON_TYPE,
                    FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON
            );

            // Put the *real* toggle button inside the textfield
            dtf.putClientProperty(
                    FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT,
                    originalBtn
            );

            dp.setOpaque(false);




// Optional: force same height feeling
//        int h = dtf.getPreferredSize().height;
//        calendarBtn.setPreferredSize(new Dimension(h, h));

//        dp.getComponentDateTextField().setEditable(false);
//        dp.getSettings().setAllowKeyboardEditing(false);

        // sensible defaults (adapt as you wish)
        LocalDate min = LocalDate.now().plusDays(1);
        LocalDate max = LocalDate.now().plusYears(5);
        dp.getSettings().setDateRangeLimits(min, max);
        dp.setDate(min);

        // Make it visually align with fields
//        dp.getComponentDateTextField().setColumns(10);
    }

    private JComponent createSuccessArea() {
        successLabel = new JLabel();
        successLabel.setVisible(false);
        successLabel.setForeground(UIManager.getColor("Label.foreground"));
        return successLabel;
    }

    private JComponent createResultArea() {
        tableResult = new JTable(new DefaultTableModel(COLUMN_NAMES, 0));
        tableResult.setFillsViewportHeight(true);

        tableScroll = new JScrollPane(tableResult);
        tableScroll.setVisible(false);

        return tableScroll;
    }

    // ------------------------
    // Actions
    // ------------------------

    private void chooseInputFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select credit transfer file");
        fc.setFileFilter(new FileNameExtensionFilter("CSV / Excel", "csv", "xls", "xlsx"));

        if (fc.showOpenDialog(parentWindow()) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            filenameInput = f.getAbsolutePath();

            inputField.setText(f.getName());          // keep UI clean
            inputField.setToolTipText(filenameInput); // full path on hover

            resetOutputs();
            updateGenerateButtonState();
        }
    }

    private void updateGenerateButtonState() {
        boolean valid = (filenameInput != null && !filenameInput.isEmpty())
                && flatDatePickerField.getDate() != null;
        generateButton.setEnabled(valid);
    }

//    private void chooseOutputFile() {
//        JFileChooser fc = new JFileChooser();
//        fc.setDialogTitle("Select SEPA output file");
//        fc.setFileFilter(new FileNameExtensionFilter("XML", "xml"));
//
//        if (fc.showSaveDialog(parentWindow()) == JFileChooser.APPROVE_OPTION) {
//            File f = fc.getSelectedFile();
//            String path = f.getAbsolutePath();
//            if (!path.toLowerCase().endsWith(".xml")) path += ".xml";
//
//            filenameOutput = path;
//
//            outputField.setText(Paths.get(filenameOutput).getFileName().toString());
//            outputField.setToolTipText(filenameOutput);
//
//            resetOutputs();
//        }
//    }

    private Component parentWindow() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return (w != null) ? w : this;
    }

    private void resetOutputs() {
        // Hide success + table when user changes inputs
        successLabel.setVisible(false);
        tableScroll.setVisible(false);

        revalidate();
        repaint();
    }

    // ------------------------
    // Public API used by MainFrame / generator
    // ------------------------

    public LocalDate getExecutionDate() { return flatDatePickerField.getDate(); }

    public void showSuccessMessage(String outputFilePath, String message) {
        filenameOutput = outputFilePath;
        Path p = Paths.get(outputFilePath);

        successLabel.setText("<html>Generated: <a href=''>" + p.getFileName() + "</a> — " + message + "</html>");
        successLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        successLabel.setVisible(true);

        // open file on click
        for (MouseListener ml : successLabel.getMouseListeners()) successLabel.removeMouseListener(ml);
        successLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().open(p.toFile());
                } catch (Exception ex) {
                    owner.showErrorMessage("Could not open the file: " + ex.getMessage());
                }
            }
        });

        revalidate();
        repaint();
    }

    public void showTableResult(List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(COLUMN_NAMES, 0);
        for (Object[] r : rows) model.addRow(r);

        tableResult.setModel(model);
        tableScroll.setVisible(true);

        revalidate();
        repaint();
    }
}
