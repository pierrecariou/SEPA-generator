package com.pcariou.view.main;

import com.pcariou.view.AppDialogs;
import com.pcariou.view.AppResources;
import com.pcariou.view.GenerationFailureDialog;
import com.pcariou.view.IGenerator;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.custom.WindowBoundsPolicy;
import com.pcariou.view.main.center.FormPanel;
import com.pcariou.view.update.UpdateUi;

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
	private UpdateUi updateUi;

	/** Stable default window size (within ~920–980 x 600–640) before screen clamping. */
	private static final int DEFAULT_WIDTH  = 960;
	private static final int DEFAULT_HEIGHT = 640;

	/** Reasonable minimum: the cards stay fully visible; the scroll pane handles overflow. */
	private static final int MIN_WIDTH  = 860;
	private static final int MIN_HEIGHT = 520;

	/** Modest gap kept between the window and the usable screen edges when room allows. */
	private static final int SCREEN_MARGIN = 24;

	/** Smallest size the layout needs without the generation summary visible. */
	private Dimension baseMinimumSize;

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

        updateUi = new UpdateUi(this, version);
        add(new HeaderPanel(this, updateUi), BorderLayout.NORTH);

        // Header and footer stay fixed; the main content scrolls so taller content
        // (e.g. the generation summary or future report panels) stays accessible on
        // smaller windows.
        formPanel = new FormPanel(this);
        JScrollPane contentScroll = new JScrollPane(formPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.setViewportBorder(null);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(contentScroll, BorderLayout.CENTER);

        footerPanel = new FooterPanel(this, formPanel, version);
        add(footerPanel, BorderLayout.SOUTH);

        applyInitialSize();

        setResizable(true);
        setVisible(true);

        refreshStatus();

        // Non-blocking, once-per-day background check. Runs after the window is
        // shown; failures are silent and never interrupt normal use.
        updateUi.startAutomaticCheck();
    }

    /**
     * Applies a stable, clamped initial window size. The default size stays in the
     * 920–980 x 600–640 range, clamped to the usable screen (accounting for
     * taskbars/docks) and centred within that work area; content overflow is handled
     * by the scrollable content area rather than by forcing the window to grow.
     */
    private void applyInitialSize() {
        // Realise the layout so child components report valid preferred sizes.
        pack();

        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usable = new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width  - insets.left - insets.right,
                bounds.height - insets.top  - insets.bottom);

        Rectangle target = WindowBoundsPolicy.compute(
                DEFAULT_WIDTH, DEFAULT_HEIGHT, MIN_WIDTH, MIN_HEIGHT, usable, SCREEN_MARGIN);
        baseMinimumSize = WindowBoundsPolicy.effectiveMinimum(MIN_WIDTH, MIN_HEIGHT, usable);
        setMinimumSize(baseMinimumSize);
        setBounds(target);
    }

	public void showErrorMessage(String message) {
		SwingUtilities.invokeLater(() -> {
			setStatus(AppStatus.GENERATION_FAILED);
			// A prior successful result must not remain looking current after a
			// failed attempt; the failure dialog owns all failure detail.
			formPanel.markResultStaleAfterFailure();
			GenerationFailureDialog.show(this, message);
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
	 * Refreshes the layout when the visible content changes (for example when the
	 * generation summary card becomes visible).
	 *
	 * The main content lives in a scroll pane, so taller content stays reachable
	 * without resizing the window; the vertical scroll bar appears automatically
	 * when needed.
	 */
	public void ensureContentVisible() {
		revalidate();
		repaint();
	}

	/**
	 * Restores the minimum window size to the layout's base requirement
	 * (used when the generation summary is hidden again, e.g. on reset),
	 * so the window can be made compact once more.
	 */
	public void restoreBaseMinimumSize() {
		if (baseMinimumSize != null) {
			setMinimumSize(baseMinimumSize);
		}
	}

	/**
	 * Recomputes the appropriate status from current application state
	 * (config validity, file selected, date selected) and applies it.
	 * Call this whenever a relevant input changes.
	 */
	public void refreshStatus() {
		// While the generation summary is visible, keep the success status instead
		// of reverting to "Ready", so the footer stays consistent with the result.
		if (formPanel.isSummaryVisible()) {
			footerPanel.setStatus(AppStatus.GENERATED);
			return;
		}

		AppStatus debtorStatus = configStore.isDebtorConfigured() ? null : AppStatus.DEBTOR_INFO_REQUIRED;
		AppStatus fileStatus   = formPanel.hasInputFile() ? null : AppStatus.SELECT_FILE;
		AppStatus dateStatus   = formPanel.hasExecutionDate() ? null : AppStatus.SELECT_DATE;

		footerPanel.setStatus(AppStatus.highest(debtorStatus, fileStatus, dateStatus));
	}
}

