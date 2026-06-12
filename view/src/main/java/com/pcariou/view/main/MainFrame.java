package com.pcariou.view.main;

import com.pcariou.view.AppResources;
import com.pcariou.view.IGenerator;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.main.center.FormPanel;

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
	private final IGenerator generator;
	private final FormPanel formPanel;
	private final ConfigStore configStore = new ConfigStore();
	private FooterPanel footerPanel;

	public IGenerator getGenerator() { return generator; }
    public MainFrame(IGenerator generator, String version) {
        super("SEPA Generator v" + version);
        this.generator = generator;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            java.util.List<Image> icons = new java.util.ArrayList<>();
            for (int s : AppResources.APP_ICON_SIZES) {
                java.net.URL u = getClass().getResource(AppResources.appIcon(s));
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
			JOptionPane.showMessageDialog(this,
					"The SEPA file could not be generated:\n\n" + message,
					"Generation failed", JOptionPane.ERROR_MESSAGE);
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
		AppStatus debtorStatus = configStore.isDebtorConfigured() ? null : AppStatus.DEBTOR_INFO_REQUIRED;
		AppStatus fileStatus   = formPanel.hasInputFile() ? null : AppStatus.SELECT_FILE;
		AppStatus dateStatus   = formPanel.hasExecutionDate() ? null : AppStatus.SELECT_DATE;

		footerPanel.setStatus(AppStatus.highest(debtorStatus, fileStatus, dateStatus));
	}
}

