package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.AppEdition;
import com.pcariou.view.AppLinks;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.main.center.FormPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class FooterPanel extends JPanel {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    public FooterPanel(MainFrame owner, FormPanel formPanel, String version) {
        super(new MigLayout(
                "insets 3 16 3 16, fillx, hidemode 3",
                "[grow][][right][][][][]",
                "[]"
        ));

        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("App.borderColor")));

        JLabel status = new JLabel();
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        progress.setPreferredSize(new Dimension(120, 6));

        JLabel edition = new JLabel(AppEdition.LABEL + "  •  v" + version);
        edition.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground;");

        JButton website = linkButton("Website",           AppLinks.WEBSITE);
        JButton privacy = linkButton("Privacy",           AppLinks.PRIVACY);
        JButton contact = linkButton("Contact",           AppLinks.CONTACT);
        JButton updates = linkButton("Check for updates",  AppLinks.CHECK_UPDATES);

        setOpaque(false);
        add(status,   "growx");
        add(progress, "w 120!, gapright 12");
        add(edition,  "gapright 12");
        add(website);
        add(privacy);
        add(contact);
        add(updates);

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

    private JButton linkButton(String text, String url) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        b.addActionListener(e -> ExternalLinks.open(url, this));
        return b;
    }
}
