package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.main.center.FormPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

public class FooterPanel extends JPanel {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    public FooterPanel(MainFrame owner, FormPanel formPanel, String version) {
        super(new MigLayout(
                "insets 3 16 3 16, fillx, hidemode 3",
                "[grow][][right][][][]",
                "[]"
        ));

        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("App.borderColor")));

        JLabel status = new JLabel();
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        progress.setPreferredSize(new Dimension(120, 6));

        JLabel edition = new JLabel("Community Edition  •  v" + version);
        edition.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground;");

        JButton github = linkButton("GitHub",       "https://github.com/pierrecariou/SEPA-generator");
        JButton docs   = linkButton("Docs",          "https://github.com/pierrecariou/SEPA-generator/blob/main/docs/usage.md");
        JButton issue  = linkButton("Report issue",  "https://github.com/pierrecariou/SEPA-generator/issues/new");

        setOpaque(false);
        add(status,   "growx");
        add(progress, "w 120!, gapright 12");
        add(edition,  "gapright 12");
        add(github);
        add(docs);
        add(issue);

        this.statusLabel = status;
        this.progressBar = progress;

        setStatus(AppStatus.READY);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        Color sep = UIManager.getColor("App.borderColor");
        if (sep != null) setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, sep));
    }

    public void setStatus(AppStatus status) {
        statusLabel.setText("● " + status.label);
        statusLabel.putClientProperty(FlatClientProperties.STYLE,
                "font: -1; foreground: " + status.styleColor + ";");
        progressBar.setVisible(status.showProgress);
        revalidate();
        repaint();
    }

    private static final Logger LOG = Logger.getLogger(FooterPanel.class.getName());

    private JButton linkButton(String text, String url) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        b.addActionListener(e -> openUrl(url));
        return b;
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            LOG.warning("Could not open URL: " + url + " — " + ex.getMessage());
        }
    }
}
