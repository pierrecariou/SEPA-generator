package com.pcariou.view.main.center;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.custom.FlatDatePickerField;
import com.pcariou.view.main.AppStatus;
import com.pcariou.view.main.MainFrame;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class FormPanel extends JPanel {

    private final MainFrame owner;
    private final ConfigStore configStore = new ConfigStore();

    private JTextField inputField;
    private FlatDatePickerField flatDatePickerField;
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

        inputField = createFileField("No input selected");
        JButton browseInput = new JButton("...");
        browseInput.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, browseInput);
        browseInput.addActionListener(e -> chooseInputFile());

        JLabel inputLabel = new JLabel("Credit transfer");
        inputLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        grid.add(inputLabel, "alignx right");
        grid.add(inputField, "growx, wrap");

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

        return grid;
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
        summaryFileName.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filenameOutput != null) {
                    try { Desktop.getDesktop().open(new File(filenameOutput)); }
                    catch (Exception ex) { owner.showErrorMessage("Could not open the file: " + ex.getMessage()); }
                }
            }
        });

        JLabel openFolder = new JLabel("📂 Show in folder");
        openFolder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openFolder.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Label.disabledForeground;");
        openFolder.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filenameOutput != null) {
                    try { Desktop.getDesktop().open(new File(filenameOutput).getParentFile()); }
                    catch (Exception ex) { owner.showErrorMessage("Could not open folder: " + ex.getMessage()); }
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
        if (border != null) tile.setBorder(roundedBorder(border, 10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared card styling
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a new JPanel that refreshes its background and border on every L&F change. */
    private static JPanel createCard(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override public void updateUI() {
                super.updateUI();
                refreshCardAppearance(this);
            }
        };
        card.putClientProperty(FlatClientProperties.STYLE, "arc:16");
        refreshCardAppearance(card);
        return card;
    }

    private static void refreshCardAppearance(JPanel card) {
        card.setOpaque(true);
        Color bg = UIManager.getColor("App.cardBackground");
        if (bg != null) card.setBackground(bg);
        Color border = UIManager.getColor("App.cardBorderColor");
        if (border == null) border = UIManager.getColor("Separator.foreground");
        if (border != null) card.setBorder(roundedBorder(border, 16));
    }

    /** Anti-aliased rounded-rect border — matches FlatLaf arc radius. */
    private static AbstractBorder roundedBorder(Color color, int arc) {
        return new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
        };
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

        filenameOutput = fc.getSelectedFile().getAbsolutePath();

        owner.setStatus(AppStatus.GENERATING);
        generateButton.setEnabled(false);
        summaryCard.setVisible(false);

        final String inputPath  = filenameInput;
        final String outputPath = filenameOutput;
        final LocalDate date    = flatDatePickerField.getDate();

        new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                owner.getGenerator().generate(inputPath, outputPath, date);
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
        revalidate();
        repaint();
    }

    private void chooseInputFile() {
        JFileChooser fc = new JFileChooser(configStore.lastInputDirectory());
        fc.setDialogTitle("Select credit transfer file");
        fc.setFileFilter(new FileNameExtensionFilter("CSV / Excel", "csv", "xls", "xlsx"));

        if (fc.showOpenDialog(parentWindow()) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            filenameInput = f.getAbsolutePath();
            inputField.setText(f.getName());
            inputField.setToolTipText(filenameInput);

            configStore.saveLastInputDirectory(f.getParentFile());

            summaryCard.setVisible(false);
            updateGenerateButtonState();
            owner.refreshStatus();
        }
    }

    private void updateGenerateButtonState() {
        generateButton.setEnabled(
                filenameInput != null && !filenameInput.isEmpty()
                && flatDatePickerField.getDate() != null);
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
    }
}

