package com.pcariou.view;

import javax.swing.*;

public final class StyleUtils {
	private StyleUtils() {}

	/**
	 * Applies minimal style to file path fields: not editable + default text.
	 */
	public static void designFileTextField(JTextField textField, String text) {
		textField.setEditable(false);
		textField.setText(text);
	}
}
