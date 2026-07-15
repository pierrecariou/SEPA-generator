package com.pcariou.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;

/**
 * Verifies the Community generation-failure dialog:
 * <ul>
 *   <li>Enumerated validation errors are shown in a scrollable details area.</li>
 *   <li>Long error lists are height-capped so the dialog never grows unbounded.</li>
 *   <li>Non-enumerated messages fall back to a concise single-reason tile.</li>
 *   <li>Singular/plural issue-count wording is correct.</li>
 *   <li>Only a Close action is offered.</li>
 *   <li>The "Open validation report" call-to-action never appears in Community.</li>
 * </ul>
 */
public class GenerationFailureDialogTest
{
	private static final String HEADLINE = "The SEPA file could not be generated";

	private static final String ENUMERATED = "Invalid input file\n"
			+ "Row 2: The IBAN is invalid.\n"
			+ "Row 5: The amount must be greater than 0.\n"
			+ "Row 9: The BIC is invalid.\n";

	@Test
	public void enumeratedErrorsAreInAScrollableDetailsArea()
	{
		JPanel panel = GenerationFailureDialog.buildContent(HEADLINE, ENUMERATED);
		String text = allText(panel);

		assertTrue("headline present", text.contains(HEADLINE));
		assertTrue("issue-count summary present",
				text.contains("3 validation issues were found"));
		assertTrue("tile dimension label present", text.contains("Input validation"));
		assertTrue("short tile headline present",
				text.contains("3 validation issues found"));
		assertTrue("scrollable area present", containsScroll(panel));
		assertTrue("full error list preserved", text.contains("Row 2: The IBAN is invalid."));
		assertTrue("full error list preserved", text.contains("Row 9: The BIC is invalid."));
		assertFalse("no report call-to-action in Community",
				text.contains("Open the validation report"));
	}

	@Test
	public void shortListHasComfortableMinimumHeight()
	{
		JPanel panel = GenerationFailureDialog.buildContent(
				HEADLINE, "Invalid input file\nRow 2: The IBAN is invalid.\n");
		JScrollPane scroll = firstScroll(panel);
		assertNotNull("single-issue list still has a scroll pane", scroll);
		assertTrue("short content gets at least 80 px of readable height",
				scroll.getPreferredSize().height >= 80);
	}

	@Test
	public void longListIsHeightCappedAndScrollable()
	{
		StringBuilder big = new StringBuilder("Invalid input file\n");
		for (int i = 2; i <= 200; i++) {
			big.append("Row ").append(i).append(": The IBAN is invalid.\n");
		}
		JPanel panel = GenerationFailureDialog.buildContent(HEADLINE, big.toString());

		JScrollPane scroll = firstScroll(panel);
		assertNotNull("a long list is rendered in a scroll pane", scroll);
		assertTrue("height is capped so the dialog never grows unbounded",
				scroll.getPreferredSize().height <= 220);
	}

	@Test
	public void nonEnumeratedMessageAppearsAsConciseTileDetail()
	{
		JPanel panel = GenerationFailureDialog.buildContent(
				HEADLINE, "Please select a valid input file.");
		String text = allText(panel);

		assertTrue("headline present", text.contains(HEADLINE));
		assertTrue("concise reason shown in tile", text.contains("Please select a valid input file."));
		assertFalse("no scroll for a single-reason message", containsScroll(panel));
		assertFalse("no report call-to-action in Community",
				text.contains("Open the validation report"));
	}

	@Test
	public void singleIssueUsesSingularWording()
	{
		JPanel panel = GenerationFailureDialog.buildContent(
				HEADLINE, "Invalid input file\nRow 2: The IBAN is invalid.\n");
		String text = allText(panel);

		assertTrue(text.contains("1 validation issue was found"));
		assertTrue(text.contains("1 validation issue found"));
	}

	@Test
	public void pluralWording()
	{
		JPanel panel = GenerationFailureDialog.buildContent(HEADLINE, ENUMERATED);
		String text = allText(panel);
		assertTrue(text.contains("3 validation issues were found"));
		assertTrue(text.contains("3 validation issues found"));
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static String allText(Component root)
	{
		StringBuilder sb = new StringBuilder();
		collect(root, sb);
		return sb.toString();
	}

	private static void collect(Component c, StringBuilder out)
	{
		if (c instanceof JTextComponent) {
			out.append(((JTextComponent) c).getText()).append('\n');
		} else if (c instanceof javax.swing.JLabel) {
			String t = ((javax.swing.JLabel) c).getText();
			if (t != null) {
				out.append(t).append('\n');
			}
		}
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				collect(child, out);
			}
		}
	}

	private static boolean containsScroll(Component c)
	{
		if (c instanceof JScrollPane) {
			return true;
		}
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				if (containsScroll(child)) {
					return true;
				}
			}
		}
		return false;
	}

	private static JScrollPane firstScroll(Component c)
	{
		if (c instanceof JScrollPane) {
			return (JScrollPane) c;
		}
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				JScrollPane found = firstScroll(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
