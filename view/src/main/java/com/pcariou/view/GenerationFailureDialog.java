package com.pcariou.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.custom.Cards;

import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Polished generation-failure dialog for validation-driven failures.
 *
 * <p>Shows a clear failure headline, a structured "Input validation" result tile
 * with the semantic error icon/color, a concise issue count, and the full
 * validation error list in a compact, height-capped, scrollable area — so any
 * number of issues remains readable without an oversized dialog.
 *
 * <p>Only changes how failures are <em>presented</em>; validation rules,
 * generation logic, and error wording are never touched here.
 */
public final class GenerationFailureDialog
{
	private static final String CLOSE = "Close";
	private static final String DIALOG_TITLE = "Generation failed";

	/** Muted dimension label for the status tile. */
	private static final String DIMENSION = "Input validation";

	/**
	 * Lead line that flags an enumerated row-validation list (emitted by the
	 * CSV reader as {@code "Invalid input file\n<issue>\n<issue>..."}).
	 * Any other message is treated as a single concise reason block.
	 */
	private static final String ENUMERATED_LIST_LEAD = "Invalid input file";

	/** Wrap/scroll width for the content, keeping the dialog compact. */
	private static final int CONTENT_WIDTH_PX = 400;
	/** Minimum height of the scrollable details area — short lists get readable space. */
	private static final int DETAILS_MIN_HEIGHT_PX = 80;
	/** Height cap for the scrollable details area; longer lists scroll. */
	private static final int DETAILS_MAX_HEIGHT_PX = 220;

	private GenerationFailureDialog()
	{
	}

	/** Shows the polished generation-failure dialog with a Close action. */
	public static void show(Component parent, String message)
	{
		String headline = "The SEPA file could not be generated";
		JPanel panel = buildContent(headline, message);
		AppDialogs.showOptions(parent, DIALOG_TITLE, panel,
				AppDialogs.Kind.ERROR, new Object[] { CLOSE }, CLOSE);
	}

	/**
	 * Builds the failure message panel. Package-visible so tests can verify
	 * the content without opening a modal dialog.
	 */
	static JPanel buildContent(String headline, String message)
	{
		List<String> issues = enumeratedIssues(message);
		boolean enumerated = !issues.isEmpty();

		JPanel panel = new JPanel(new MigLayout(
				"insets 0, wrap 1, gapy 8", "[" + CONTENT_WIDTH_PX + "!]"));
		panel.setOpaque(false);

		String meta = enumerated ? issueCountSummary(issues.size()) : null;
		panel.getAccessibleContext().setAccessibleDescription(
				accessibleText(headline, meta, message));

		panel.add(header(headline, meta), "growx");

		String tileStatus = enumerated
				? issueCountShort(issues.size())
				: "Generation could not be completed";
		String tileDetail = enumerated ? null : (hasText(message) ? message.trim() : null);
		panel.add(statusTile(tileStatus, tileDetail), "growx");

		if (enumerated) {
			panel.add(detailsArea(join(issues)), "growx");
		}
		return panel;
	}

	// ── Message parsing ───────────────────────────────────────────────────────

	private static List<String> enumeratedIssues(String message)
	{
		List<String> issues = new ArrayList<String>();
		if (!hasText(message)) {
			return issues;
		}
		String[] lines = message.split("\n");
		if (lines.length == 0 || !lines[0].trim().equals(ENUMERATED_LIST_LEAD)) {
			return issues;
		}
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i].trim();
			if (!line.isEmpty()) {
				issues.add(line);
			}
		}
		return issues;
	}

	private static String issueCountSummary(int count)
	{
		return count == 1
				? "1 validation issue was found in the input file."
				: count + " validation issues were found in the input file.";
	}

	private static String issueCountShort(int count)
	{
		return count == 1 ? "1 validation issue found" : count + " validation issues found";
	}

	private static String accessibleText(String headline, String meta, String message)
	{
		StringBuilder sb = new StringBuilder(headline);
		if (meta != null) {
			sb.append(". ").append(meta);
		} else if (hasText(message)) {
			sb.append(". ").append(message.trim());
		}
		return sb.toString();
	}

	private static boolean hasText(String s)
	{
		return s != null && !s.trim().isEmpty();
	}

	private static String join(List<String> lines)
	{
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(line);
		}
		return sb.toString();
	}

	// ── Styled components ─────────────────────────────────────────────────────

	/**
	 * Bold headline over an optional muted meta line (the issue-count summary).
	 * A {@code null} meta is omitted.
	 */
	private static JPanel header(String headline, String metaLine)
	{
		JPanel header = new JPanel(new MigLayout("insets 0, wrap 1, gapy 1", "[grow,fill]"));
		header.setOpaque(false);

		JLabel title = new JLabel(headline);
		title.putClientProperty(FlatClientProperties.STYLE, "font: bold;");
		header.add(title);

		if (metaLine != null) {
			JLabel meta = new JLabel(metaLine);
			meta.putClientProperty(FlatClientProperties.STYLE,
					"font: -1; foreground: $Label.disabledForeground;");
			header.add(meta);
		}
		return header;
	}

	/**
	 * Rounded status tile with the semantic error icon, colored status headline
	 * and an optional muted detail line. Re-styles itself on theme change.
	 */
	private static JPanel statusTile(String status, String detail)
	{
		JPanel tile = new JPanel(new MigLayout(
				"insets 8 12 8 12, wrap 1, gapy 2", "[grow,fill]")) {
			@Override public void updateUI() {
				super.updateUI();
				refreshTileAppearance(this);
			}
		};
		tile.putClientProperty(FlatClientProperties.STYLE, "arc:10");
		refreshTileAppearance(tile);

		JLabel dimensionLabel = new JLabel(DIMENSION);
		dimensionLabel.putClientProperty(FlatClientProperties.STYLE,
				"font: -2; foreground: $Label.disabledForeground;");
		tile.add(dimensionLabel);

		JLabel statusLabel = new JLabel(status,
				SvgIcons.linkIcon(SvgIcons.CIRCLE_X, "App.errorColor"), JLabel.LEADING);
		statusLabel.setIconTextGap(6);
		statusLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold;");
		Color errorColor = UIManager.getColor("App.errorColor");
		if (errorColor != null) {
			statusLabel.setForeground(errorColor);
		}
		tile.add(statusLabel);

		if (hasText(detail)) {
			tile.add(wrappedText(detail.trim(), true), "gapleft 22, growx");
		}
		return tile;
	}

	private static void refreshTileAppearance(JPanel tile)
	{
		tile.setOpaque(true);
		Color bg = UIManager.getColor("App.tileBackground");
		if (bg == null) {
			bg = UIManager.getColor("App.cardBackground");
		}
		if (bg != null) {
			tile.setBackground(bg);
		}
		Color border = UIManager.getColor("App.cardBorderColor");
		if (border == null) {
			border = UIManager.getColor("Component.borderColor");
		}
		if (border != null) {
			tile.setBorder(Cards.roundedBorder(border, 10));
		}
	}

	/** Read-only, transparent, word-wrapped label-styled text area. */
	private static JTextArea wrappedText(String text, boolean muted)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setBorder(null);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		Font base = UIManager.getFont("Label.font");
		if (base != null) {
			area.setFont(muted ? base.deriveFont(base.getSize2D() - 1f) : base);
		}
		String fg = muted ? "$Label.disabledForeground" : "$Label.foreground";
		area.putClientProperty(FlatClientProperties.STYLE, "foreground: " + fg + ";");
		return area;
	}

	/**
	 * Compact, theme-aware scrollable area for the full detected-issue text.
	 * Height is capped so the dialog never grows unbounded on long lists.
	 */
	private static JScrollPane detailsArea(String text)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		Font base = UIManager.getFont("Label.font");
		if (base != null) {
			area.setFont(base);
		}

		JScrollPane scroll = new JScrollPane(area,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.putClientProperty(FlatClientProperties.STYLE, "arc:10");
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		// Measure at the actual display width so word-wrap is reflected in the
		// preferred height. Without this, JTextArea returns a near-single-line
		// height before it has a container width, making short content cramped.
		area.setSize(CONTENT_WIDTH_PX, Short.MAX_VALUE);
		int wanted = area.getPreferredSize().height + 4;
		int height = Math.max(DETAILS_MIN_HEIGHT_PX, Math.min(wanted, DETAILS_MAX_HEIGHT_PX));
		scroll.setPreferredSize(new Dimension(CONTENT_WIDTH_PX, height));
		return scroll;
	}
}
