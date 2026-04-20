package com.pcariou.view.main;

import com.pcariou.view.main.center.FormPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class FooterPanel extends JPanel {
//	private final JButton buttonGenerate;

	public FooterPanel(MainFrame owner, FormPanel formPanel) {
		super(new MigLayout(
                "insets 8 24 12 24, fillx",
                "[grow]",
                "[]"
        ));

//		buttonGenerate = new JButton("Generate");
//		buttonGenerate.addActionListener(e -> {
//			formPanel.resetResultVisibility();
//			owner.getGenerator().generate(
//                    "test1.xml",
//                    "output.xml",
//                    LocalDate.now()
//					formPanel.getFilenameInput(),
//					formPanel.getFilenameOutput(),
//					formPanel.getExecutionDate()
//			);
//		});
//
//		add(buttonGenerate);
//		owner.getRootPane().setDefaultButton(buttonGenerate);
	}
}
