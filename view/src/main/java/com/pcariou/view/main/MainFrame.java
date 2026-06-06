package com.pcariou.view.main;

import com.google.gson.Gson;
import com.pcariou.view.IGenerator;
import com.pcariou.view.SettingsFrame;
import com.pcariou.view.main.center.FormPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Main application window.
 *
 * Header: settings
 * Center: form panel (file/date inputs + results)
 * Footer: generate button
 */
public class MainFrame extends JFrame {
	private final IGenerator generator;
	private final FormPanel formPanel;
	private FooterPanel footerPanel;

	public IGenerator getGenerator() { return generator; }
    public MainFrame(IGenerator generator, String version) {
        super("SEPA Generator v" + version);
        this.generator = generator;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            java.util.List<Image> icons = new java.util.ArrayList<>();
            for (int s : new int[]{16, 32, 48, 64, 256}) {
                java.net.URL u = getClass().getResource("/sepa-generator-icon-app-compact-" + s + ".png");
                if (u != null) icons.add(new ImageIcon(u).getImage());
            }
            if (!icons.isEmpty()) setIconImages(icons);
        } catch (Exception ignored) {}

        add(new HeaderPanel(this), BorderLayout.NORTH);

        formPanel = new FormPanel(this);
        add(formPanel, BorderLayout.CENTER);

        footerPanel = new FooterPanel(this, formPanel, version);
        add(footerPanel, BorderLayout.SOUTH);

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

        refreshStatus();
    }

	public void showErrorMessage(String message) {
		SwingUtilities.invokeLater(() -> {
			setStatus(AppStatus.GENERATION_FAILED);
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
		});
	}

	// Generator calls showSuccessMessage then showTableResult sequentially.
	// We buffer the file path so showTableResult can combine both into one summary call.
	private volatile String pendingOutputFilePath;

	public void showSuccessMessage(String outputFilePath, String message) {
		pendingOutputFilePath = outputFilePath;
	}

	public void showTableResult(List<String> resultList) {
		final String outputFilePath = pendingOutputFilePath;
		SwingUtilities.invokeLater(() -> {
			setStatus(AppStatus.GENERATED);
			formPanel.showGenerationSummary(outputFilePath, resultList);
		});
	}

	/** Directly set a status (use for transient states like GENERATING). */
	public void setStatus(AppStatus status) {
		footerPanel.setStatus(status);
	}

	/**
	 * Recomputes the appropriate status from current application state
	 * (config validity, file selected, date selected) and applies it.
	 * Call this whenever a relevant input changes.
	 */
	public void refreshStatus() {
		AppStatus debtorStatus = isDebtorConfigured() ? null : AppStatus.DEBTOR_INFO_REQUIRED;
		AppStatus fileStatus   = formPanel.hasInputFile() ? null : AppStatus.SELECT_FILE;
		AppStatus dateStatus   = formPanel.hasExecutionDate() ? null : AppStatus.SELECT_DATE;

		footerPanel.setStatus(AppStatus.highest(debtorStatus, fileStatus, dateStatus));
	}

	private static final File CONFIG_FILE =
			new File(System.getProperty("user.home"), ".sepa-generator-config.json");

	private boolean isDebtorConfigured() {
		if (!CONFIG_FILE.exists()) return false;
		try (FileReader r = new FileReader(CONFIG_FILE)) {
			SettingsFrame.ConfigData cfg = new Gson().fromJson(r, SettingsFrame.ConfigData.class);
			return cfg != null
					&& cfg.debtor != null
					&& notBlank(cfg.debtor.name)
					&& notBlank(cfg.debtor.iban)
					&& notBlank(cfg.debtor.bic)
					&& cfg.initiatingParty != null
					&& notBlank(cfg.initiatingParty.name)
					&& notBlank(cfg.initiatingParty.siret);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}
}

