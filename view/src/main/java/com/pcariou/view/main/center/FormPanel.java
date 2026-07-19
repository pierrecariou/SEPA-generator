package com.pcariou.view.main.center;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.model.PainVersion;
import com.pcariou.view.AppDialogs;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.InputTemplates;
import com.pcariou.view.SvgIcons;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.custom.Cards;
import com.pcariou.view.custom.FlatDatePickerField;
import com.pcariou.view.custom.Links;
import com.pcariou.view.custom.SectionHeader;
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

public class FormPanel extends JPanel implements Scrollable {

    private final MainFrame owner;
    private final ConfigStore configStore = new ConfigStore();

    // Form section headers (visual grouping of the input card, in display order).
    static final String SECTION_INPUT = "Input";
    static final String SECTION_OUTPUT = "Output";

    /** The form's section headers, in the order they appear on the card. */
    static java.util.List<String> formSectionOrder() {
        return java.util.Arrays.asList(SECTION_INPUT, SECTION_OUTPUT);
    }

    private JTextField inputField;
    private FlatDatePickerField flatDatePickerField;
    private JComboBox<PainVersion> formatCombo;
    private JButton generateButton;

    // Summary card fields
    private JPanel summaryCard;
    private JLabel summaryTitle;
    private JLabel summaryFileName;
    private JLabel summaryTxCount;
    private JLabel summaryAmount;
    private JLabel summaryDate;

    /**
     * True once the result surface has appeared during this session. After that
     * its layout space stays reserved: regeneration updates it in place instead
     * of collapsing and re-expanding the card. Reset (and selecting a new input)
     * clears it back to false so the surface fully collapses again.
     */
    private boolean summaryShownOnce;

    // Result-surface outcome headings (semantic icon carries the state colour).
    static final String RESULT_TITLE_SUCCESS = "File generated successfully";
    static final String RESULT_TITLE_GENERATING = "Generating\u2026";
    static final String RESULT_TITLE_STALE_FAILED = "No file generated \u2014 last attempt failed";


    @Getter private String filenameInput;
    @Getter private String filenameOutput;

    public FormPanel(MainFrame owner) {
        super(new MigLayout("fillx, insets 24 24 24 24, hidemode 3", "[grow,center]", "[top][top]"));
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

        JPanel inputColumn = new JPanel(new MigLayout("insets 0, fillx, gapy 2", "[grow]", "[][]"));
        inputColumn.setOpaque(false);
        inputColumn.add(inputField, "growx, wrap");
        inputColumn.add(createTemplateLink(), "alignx left, gapleft 0");

        grid.add(new SectionHeader(SECTION_INPUT), "span 2, growx, wrap");
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
        grid.add(new SectionHeader(SECTION_OUTPUT), "span 2, growx, gaptop 6, wrap");
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
        combo.setSelectedItem(persisted != null ? persisted : PainVersion.PAIN_001_001_09);
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
        reset.setFocusable(false);
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        JLabel title = new JLabel(RESULT_TITLE_SUCCESS);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setIconTextGap(8);
        // Focusable so keyboard/assistive users land on the outcome after a run;
        // it carries an accessible name describing the result surface.
        title.setFocusable(true);
        title.getAccessibleContext().setAccessibleName("Generation result");
        this.summaryTitle = title;

        summaryFileName = new JLabel();
        summaryFileName.setIcon(SvgIcons.linkIcon(SvgIcons.FILE_CODE, "Component.accentColor"));
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
        });
        Links.asLink(summaryFileName);

        JLabel openFolder = new JLabel("Show in folder");
        openFolder.setIcon(SvgIcons.linkIcon(SvgIcons.FOLDER_OPEN, "Label.disabledForeground"));
        openFolder.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Label.disabledForeground;");
        openFolder.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filenameOutput != null) {
                    ExternalLinks.showInFolder(new File(filenameOutput), FormPanel.this);
                }
            }
        });
        Links.asLink(openFolder);

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
            int choice = AppDialogs.confirm(parentWindow(), "Replace file?",
                    "\"" + selected.getName() + "\" already exists.\nDo you want to replace it?",
                    AppDialogs.Kind.WARNING, JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        filenameOutput = selected.getAbsolutePath();

        owner.setStatus(AppStatus.GENERATING);
        generateButton.setEnabled(false);
        // Reserved-space: once the result surface has been shown, keep it and move
        // it to the in-progress state instead of collapsing it; on the first run
        // it stays hidden so there is no empty placeholder before any generation.
        if (summaryShownOnce) {
            markResultGenerating();
        } else {
            summaryCard.setVisible(false);
        }

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
        summaryShownOnce = false;
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

        // Selecting a new input is a deliberate context change, not a
        // regeneration: collapse the result surface so the next run is treated
        // as a first result again (reserved-space applies to consecutive runs).
        summaryCard.setVisible(false);
        summaryShownOnce = false;
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
        link.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        link.setToolTipText("Save a ready-to-edit CSV or Excel template");
        link.addActionListener(e -> showTemplateMenu(link));
        Links.asLink(link);
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
            int overwrite = AppDialogs.confirm(parentWindow(), "Replace file?",
                    "\"" + target.getName() + "\" already exists. Replace it?",
                    AppDialogs.Kind.WARNING, JOptionPane.YES_NO_OPTION);
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

        int use = AppDialogs.confirm(parentWindow(), "Template saved",
                "Template saved to:\n" + target.getAbsolutePath()
                        + "\n\nUse it as the current input file?",
                AppDialogs.Kind.SUCCESS, JOptionPane.YES_NO_OPTION);
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
        AppDialogs.show(parentWindow(), "SEPA Generator", message, AppDialogs.Kind.WARNING);
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
    public boolean isSummaryVisible() { return summaryCard != null && summaryCard.isVisible(); }
    public LocalDate getExecutionDate() { return flatDatePickerField.getDate(); }

    /**
     * Called from MainFrame after successful generation.
     * {@code resultList} = [txCount, totalAmount, executionDate] from CsvToBeans.
     */
    public void showGenerationSummary(String outputFilePath, List<String> resultList) {
        filenameOutput = outputFilePath;
        Path p = Paths.get(outputFilePath);

        summaryFileName.setText(p.getFileName().toString());
        summaryFileName.setToolTipText(outputFilePath);

        if (resultList != null && resultList.size() >= 3) {
            summaryTxCount.setText(resultList.get(0));
            summaryAmount.setText(resultList.get(1) + " EUR");
            summaryDate.setText(resultList.get(2));
        }

        // Clean success: the result surface has no warning states in Community.
        setOutcomeTitle(RESULT_TITLE_SUCCESS, SvgIcons.CIRCLE_CHECK, "App.successColor");
        summaryShownOnce = true;
        summaryCard.setVisible(true);
        revalidate();
        repaint();
        owner.ensureContentVisible();
        revealAndFocusSummary();
    }

    /**
     * Called from MainFrame when a generation attempt fails. If a previous
     * successful result is on screen, mark the reserved surface as carrying no
     * current result (blanked metrics, neutral heading) so it can never be
     * mistaken for the outcome of the failed attempt. All failure detail is
     * owned by the failure dialog MainFrame shows.
     */
    public void markResultStaleAfterFailure() {
        if (!summaryShownOnce) {
            return;
        }
        setOutcomeTitle(RESULT_TITLE_STALE_FAILED, SvgIcons.CIRCLE_INFO, "Label.disabledForeground");
        blankSummaryMetrics();
        revalidate();
        repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result-surface state (G2): stable space, in-place updates, stale clarity
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sets the outcome heading text and its semantic icon (the icon carries the
     * state colour; the text stays in the default foreground so it reads calmly).
     */
    private void setOutcomeTitle(String text, String iconName, String colorKey) {
        summaryTitle.setText(text);
        summaryTitle.setIcon(SvgIcons.linkIcon(iconName, colorKey));
    }

    /** Clears the metric values so a stale result never reads as current. */
    private void blankSummaryMetrics() {
        summaryTxCount.setText("");
        summaryAmount.setText("");
        summaryDate.setText("");
    }

    /**
     * Regeneration: keep the reserved result surface visible but clearly
     * in-progress — a neutral "Generating…" heading and blanked metrics — so the
     * previous result can never be mistaken for the new one.
     */
    private void markResultGenerating() {
        setOutcomeTitle(RESULT_TITLE_GENERATING, SvgIcons.CIRCLE_INFO, "Label.disabledForeground");
        blankSummaryMetrics();
        revalidate();
        repaint();
    }

    /**
     * After layout settles, scroll the result surface fully into the viewport and
     * move keyboard focus to its outcome title. Deferred with invokeLater so the
     * pending revalidate/layout has completed before we measure and scroll.
     */
    private void revealAndFocusSummary() {
        SwingUtilities.invokeLater(() -> {
            if (summaryCard == null || !summaryCard.isVisible()) {
                return;
            }
            summaryCard.scrollRectToVisible(
                    new Rectangle(0, 0, summaryCard.getWidth(), summaryCard.getHeight()));
            summaryTitle.requestFocusInWindow();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scrollable: fill the viewport width (cards stretch horizontally) but never
    // stretch vertically, so extra height stays as flexible filler below the form.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
