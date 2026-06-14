package com.pcariou.view.main.center;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.model.PainVersion;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.InputTemplates;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.custom.Cards;
import com.pcariou.view.custom.FlatDatePickerField;
import com.pcariou.view.main.AppStatus;
import com.pcariou.view.main.MainFrame;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class FormPanel extends JPanel {

    private final MainFrame owner;
    private final ConfigStore configStore = new ConfigStore();

    private JTextField inputField;
    private FlatDatePickerField flatDatePickerField;
    private JComboBox<PainVersion> formatCombo;
    private JButton generateButton;

    // Summary card fields
    private JPanel summaryCard;
    private JLabel summaryFileName;
    private JLabel summaryTxCount;
    private JLabel summaryAmount;
    private JLabel summaryDate;

    @Getter private String filenameInput;
    @Getter private String filenameOutput;

    public FormPanel(MainFrame owner) {
        super(new MigLayout("fillx, insets 24 24 0 24", "[grow,center]", "[top][top]"));
        this.owner = owner;

        add(createInputCard(),   "w 760::, growx, wrap 16");
        add(createSummaryCard(), "w 760::, growx, wrap");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input card
    // ─────────────────────────────────────────────────────────────────────────

    private JComponent createInputCard() {
        JPanel card = createCard(new MigLayout(
                "fillx, insets 20, hidemode 3",
                "[grow,fill]",
                "[]12[]16[]"
        ));

        card.add(createCardHeader(), "growx, wrap");
        card.add(createFormGrid(),   "growx, wrap");
        card.add(createActionsRow(), "growx, wrap");

        return card;
    }

    private JComponent createCardHeader() {
        JPanel p = new JPanel(new MigLayout("insets 0", "[grow,fill]", "[]2[]"));
        p.setOpaque(false);

        JLabel title = new JLabel("Create SEPA payment file");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subtitle = new JLabel("Select an input file, execution date, and output format.");
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

        inputField = createFileField("Select a CSV or Excel file…");
        JButton browseInput = new JButton("...");
        browseInput.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        browseInput.setToolTipText("Browse...");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, browseInput);
        browseInput.addActionListener(e -> chooseInputFile());

        JLabel inputLabel = new JLabel("Input file");
        inputLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel inputColumn = new JPanel(new MigLayout("insets 0, fillx, gapy 2", "[grow,fill]", "[][]"));
        inputColumn.setOpaque(false);
        inputColumn.add(inputField, "growx, wrap");
        inputColumn.add(createTemplateLink(), "alignx left, gapleft 0");

        grid.add(inputLabel, "alignx right, aligny top, gaptop 6");
        grid.add(inputColumn, "growx, wrap");

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        flatDatePickerField = new FlatDatePickerField(tomorrow, LocalDate.now().plusYears(5), browseInput.getPreferredSize());
        flatDatePickerField.setDate(tomorrow);
        flatDatePickerField.getEngine().addDateChangeListener(e -> {
            updateGenerateButtonState();
            owner.refreshStatus();
        });

        JLabel dateLabel = new JLabel("Execution date");
        dateLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        grid.add(dateLabel, "alignx right");
        grid.add(flatDatePickerField, "growx, wrap");

        formatCombo = createFormatCombo();
        JLabel formatLabel = new JLabel("SEPA format");
        formatLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        grid.add(formatLabel, "alignx right");
        grid.add(formatCombo, "growx, wrap");

        return grid;
    }

    private JComboBox<PainVersion> createFormatCombo() {
        JComboBox<PainVersion> combo = new JComboBox<>(PainVersion.values());
        combo.setToolTipText("ISO 20022 message version of the generated XML");
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PainVersion) {
                    setText(formatLabelFor((PainVersion) value));
                }
                return this;
            }
        });

        PainVersion persisted = PainVersion.fromCode(configStore.readPainFormat());
        combo.setSelectedItem(persisted != null ? persisted : PainVersion.PAIN_001_001_02);
        combo.addActionListener(e -> {
            PainVersion selected = (PainVersion) combo.getSelectedItem();
            if (selected != null) {
                configStore.savePainFormat(selected.getCode());
            }
        });
        return combo;
    }

    private static String formatLabelFor(PainVersion version) {
        switch (version) {
            case PAIN_001_001_09: return "pain.001.001.09 (modern ISO 20022)";
            case PAIN_001_001_02:
            default:              return "pain.001.001.02 (classic)";
        }
    }

    /** Currently selected pain.001 version (never {@code null}). */
    public PainVersion getSelectedPainVersion() {
        PainVersion selected = (PainVersion) formatCombo.getSelectedItem();
        return selected != null ? selected : PainVersion.PAIN_001_001_02;
    }

    private JComponent createActionsRow() {
        JPanel p = new JPanel(new MigLayout("insets 0", "[grow][][]", "[]"));
        p.setOpaque(false);

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> resetAll());

        JButton generate = new JButton("Generate");
        generate.putClientProperty("JButton.buttonType", "roundRect");
        generate.putClientProperty("JComponent.sizeVariant", "large");
        generate.putClientProperty("FlatLaf.style", "minimumWidth:140; minimumHeight:36;");
        generate.addActionListener(e -> onGenerate());
        generate.setEnabled(false);
        generate.setToolTipText("Select an input file and an execution date first");
        this.generateButton = generate;

        p.add(new JLabel(), "growx, pushx");
        p.add(reset, "gapright 8");
        p.add(generate, "right");

        owner.getRootPane().setDefaultButton(generate);
        SwingUtilities.invokeLater(() -> {
            JRootPane rp = SwingUtilities.getRootPane(this);
            if (rp != null) rp.setDefaultButton(generate);
        });

        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Summary card (hidden until generation succeeds)
    // ─────────────────────────────────────────────────────────────────────────

    private JComponent createSummaryCard() {
        summaryCard = createCard(new MigLayout(
                "fillx, insets 20, hidemode 3",
                "[grow,fill]",
                "[]12[]"
        ));
        summaryCard.setVisible(false);

        // Header row
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][][]", "[]"));
        header.setOpaque(false);

        JLabel title = new JLabel("Generation summary");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        summaryFileName = new JLabel();
        summaryFileName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        summaryFileName.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor;");
        summaryFileName.setToolTipText("Open the generated file");
        summaryFileName.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filenameOutput != null) {
                    try { Desktop.getDesktop().open(new File(filenameOutput)); }
                    catch (Exception ex) { showLocalError("Could not open the file:\n" + ex.getMessage()); }
                }
            }
            @Override public void mouseEntered(MouseEvent e) {
                summaryFileName.putClientProperty(FlatClientProperties.STYLE,
                        "foreground: $Component.accentColor; font: bold;");
            }
            @Override public void mouseExited(MouseEvent e) {
                summaryFileName.putClientProperty(FlatClientProperties.STYLE,
                        "foreground: $Component.accentColor;");
            }
        });

        JLabel openFolder = new JLabel("📂 Show in folder");
        openFolder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openFolder.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Label.disabledForeground;");
        openFolder.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filenameOutput != null) {
                    ExternalLinks.showInFolder(new File(filenameOutput), FormPanel.this);
                }
            }
            @Override public void mouseEntered(MouseEvent e) {
                openFolder.putClientProperty(FlatClientProperties.STYLE,
                        "foreground: $Component.accentColor;");
            }
            @Override public void mouseExited(MouseEvent e) {
                openFolder.putClientProperty(FlatClientProperties.STYLE,
                        "foreground: $Label.disabledForeground;");
            }
        });

        header.add(title,      "growx");
        header.add(openFolder, "gapright 12");
        header.add(summaryFileName, "right");

        // Stats row — 3 tiles: transactions | amount | date
        JPanel stats = new JPanel(new MigLayout("insets 0, fillx", "[grow,fill][grow,fill][grow,fill]", "[]"));
        stats.setOpaque(false);

        summaryTxCount = new JLabel();
        summaryAmount  = new JLabel();
        summaryDate    = new JLabel();

        stats.add(createStatTile("Transactions", summaryTxCount), "growx");
        stats.add(createStatTile("Total amount", summaryAmount),  "growx");
        stats.add(createStatTile("Execution date", summaryDate),  "growx");

        summaryCard.add(header, "growx, wrap");
        summaryCard.add(stats,  "growx, wrap");

        return summaryCard;
    }

    private JPanel createStatTile(String labelText, JLabel valueLabel) {
        JPanel tile = new JPanel(new MigLayout("insets 12 16 12 16, fill", "[grow,fill]", "[]4[]")) {
            @Override public void updateUI() {
                super.updateUI();
                refreshTileAppearance(this);
            }
        };
        tile.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        refreshTileAppearance(tile);

        JLabel lbl = new JLabel(labelText);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font: -2; foreground: $Label.disabledForeground;");
        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold +2;");

        tile.add(lbl,        "growx, wrap");
        tile.add(valueLabel, "growx");
        return tile;
    }

    private static void refreshTileAppearance(JPanel tile) {
        tile.setOpaque(true);
        Color bg = UIManager.getColor("App.tileBackground");
        if (bg == null) {
            bg = UIManager.getColor("App.cardBackground");
            if (bg != null) bg = bg.darker();
        }
        if (bg != null) tile.setBackground(bg);
        Color border = UIManager.getColor("App.cardBorderColor");
        if (border == null) border = UIManager.getColor("Component.borderColor");
        if (border != null) tile.setBorder(Cards.roundedBorder(border, 10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared card styling (see com.pcariou.view.custom.Cards)
    // ─────────────────────────────────────────────────────────────────────────

    private static JPanel createCard(LayoutManager layout) {
        return Cards.createCard(layout);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    private void onGenerate() {
        Path input = Paths.get(filenameInput);
        String suggestedOutput = FilenameUtils.getBaseName(filenameInput) + ".xml";

        JFileChooser fc = new JFileChooser(input.getParent().toFile());
        fc.setDialogTitle("Save SEPA XML");
        fc.setSelectedFile(new File(input.getParent().toFile(), suggestedOutput));
        fc.setFileFilter(new FileNameExtensionFilter("XML", "xml"));

        if (fc.showSaveDialog(parentWindow()) != JFileChooser.APPROVE_OPTION)
            return;

        File selected = fc.getSelectedFile();
        if (selected.exists()) {
            int choice = JOptionPane.showConfirmDialog(parentWindow(),
                    "\"" + selected.getName() + "\" already exists.\nDo you want to replace it?",
                    "Replace file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        filenameOutput = selected.getAbsolutePath();

        owner.setStatus(AppStatus.GENERATING);
        generateButton.setEnabled(false);
        summaryCard.setVisible(false);

        final String inputPath  = filenameInput;
        final String outputPath = filenameOutput;
        final LocalDate date    = flatDatePickerField.getDate();
        final PainVersion version = getSelectedPainVersion();

        new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                owner.getGenerator().generate(inputPath, outputPath, date, version);
                return null;
            }
            @Override protected void done() {
                generateButton.setEnabled(true);
            }
        }.execute();
    }

    private void resetAll() {
        filenameInput  = null;
        filenameOutput = null;
        inputField.setText("");
        inputField.setToolTipText(null);

        flatDatePickerField.setDate(LocalDate.now().plusDays(1));

        summaryCard.setVisible(false);
        generateButton.setEnabled(false);

        owner.refreshStatus();
        owner.restoreBaseMinimumSize();
        revalidate();
        repaint();
    }

    private void chooseInputFile() {
        JFileChooser fc = new JFileChooser(configStore.lastInputDirectory());
        fc.setDialogTitle("Select credit transfer file");
        fc.setFileFilter(new FileNameExtensionFilter("CSV / Excel", "csv", "xls", "xlsx"));

        if (fc.showOpenDialog(parentWindow()) == JFileChooser.APPROVE_OPTION) {
            useInputFile(fc.getSelectedFile());
        }
    }

    /** Selects {@code f} as the current input file and refreshes the form state. */
    private void useInputFile(File f) {
        filenameInput = f.getAbsolutePath();
        inputField.setText(f.getName());
        inputField.setToolTipText(filenameInput);

        if (f.getParentFile() != null) {
            configStore.saveLastInputDirectory(f.getParentFile());
        }

        summaryCard.setVisible(false);
        updateGenerateButtonState();
        owner.refreshStatus();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input templates (onboarding helper)
    // ─────────────────────────────────────────────────────────────────────────

    private JComponent createTemplateLink() {
        JButton link = new JButton("Get input template...");
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setMargin(new Insets(0, 0, 0, 0));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        link.setToolTipText("Save a ready-to-edit CSV or Excel template");
        link.addActionListener(e -> showTemplateMenu(link));
        return link;
    }

    private void showTemplateMenu(JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        for (InputTemplates.Kind kind : InputTemplates.Kind.values()) {
            JMenuItem item = new JMenuItem(kind.label());
            item.setEnabled(InputTemplates.isAvailable(kind));
            item.addActionListener(e -> saveTemplate(kind));
            menu.add(item);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void saveTemplate(InputTemplates.Kind kind) {
        if (!InputTemplates.isAvailable(kind)) {
            showLocalError("The " + kind.label() + " is not available in this build.");
            return;
        }

        JFileChooser fc = new JFileChooser(configStore.lastInputDirectory());
        fc.setDialogTitle("Save " + kind.label());
        fc.setSelectedFile(new File(kind.defaultFileName()));

        if (fc.showSaveDialog(parentWindow()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = fc.getSelectedFile();
        if (target.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(parentWindow(),
                    "\"" + target.getName() + "\" already exists. Replace it?",
                    "Replace file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            InputTemplates.copyTo(kind, target.toPath());
        } catch (IOException ex) {
            showLocalError("The template could not be saved:\n\n" + ex.getMessage());
            return;
        }

        if (target.getParentFile() != null) {
            configStore.saveLastInputDirectory(target.getParentFile());
        }

        int use = JOptionPane.showConfirmDialog(parentWindow(),
                "Template saved to:\n" + target.getAbsolutePath()
                        + "\n\nUse it as the current input file?",
                "Template saved", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (use == JOptionPane.YES_OPTION) {
            useInputFile(target);
        }
    }

    private void updateGenerateButtonState() {
        boolean ready = filenameInput != null && !filenameInput.isEmpty()
                && flatDatePickerField.getDate() != null;
        generateButton.setEnabled(ready);
        generateButton.setToolTipText(ready ? null
                : "Select an input file and an execution date first");
    }

    /** Error dialog for minor, non-generation failures (does not change the footer status). */
    private void showLocalError(String message) {
        JOptionPane.showMessageDialog(parentWindow(), message,
                "SEPA Generator", JOptionPane.WARNING_MESSAGE);
    }

    private JTextField createFileField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setFocusable(false);
        tf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    private Component parentWindow() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return (w != null) ? w : this;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API used by MainFrame / generator callbacks
    // ─────────────────────────────────────────────────────────────────────────

    public boolean hasInputFile()     { return filenameInput  != null && !filenameInput.isEmpty(); }
    public boolean hasExecutionDate() { return flatDatePickerField.getDate() != null; }
    public LocalDate getExecutionDate() { return flatDatePickerField.getDate(); }

    /**
     * Called from MainFrame after successful generation.
     * {@code resultList} = [txCount, totalAmount, executionDate] from CsvToBeans.
     */
    public void showGenerationSummary(String outputFilePath, List<String> resultList) {
        filenameOutput = outputFilePath;
        Path p = Paths.get(outputFilePath);

        summaryFileName.setText("📄 " + p.getFileName());
        summaryFileName.setToolTipText(outputFilePath);

        if (resultList != null && resultList.size() >= 3) {
            summaryTxCount.setText(resultList.get(0));
            summaryAmount.setText(resultList.get(1) + " EUR");
            summaryDate.setText(resultList.get(2));
        }

        summaryCard.setVisible(true);
        revalidate();
        repaint();
        owner.ensureContentVisible();
    }
}
