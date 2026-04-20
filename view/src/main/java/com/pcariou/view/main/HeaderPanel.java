package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.AppTheme;
import com.pcariou.view.SettingsFrame;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private final JButton themeButton = new JButton("🌙");
    private final JButton profilesButton = new JButton("👤");
	public HeaderPanel(MainFrame owner) {
		super(new BorderLayout());
//		add(createSettingsBar(owner), BorderLayout.EAST);
        setOpaque(true);
        setBackground(UIManager.getColor("App.cardBackground"));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("App.borderColor")));

//        JLabel titleLabel = new JLabel("SEPA Generator");
//        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
//        add(titleLabel, BorderLayout.WEST);

//        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
//        rightPanel.setOpaque(false);
        JToolBar rightPanel = new JToolBar();
        rightPanel.setFloatable(false);
        rightPanel.setOpaque(false);

        themeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        profilesButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        themeButton.setFocusable(false);
        profilesButton.setFocusable(false);
        themeButton.setToolTipText("Toggle Theme");
        profilesButton.setToolTipText("Profiles / Settings");
//        themeButton.setPreferredSize(new Dimension(36, 30));
//        profilesButton.setPreferredSize(new Dimension(36, 30));

        themeButton.addActionListener(e -> AppTheme.switchMode());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem debtor = new JMenuItem("Debtor profile...");
        debtor.addActionListener(e -> {
            JFrame settingsFrame = new SettingsFrame(owner);
            settingsFrame.setVisible(true);
        });
        menu.add(debtor);

//        menu.add(new JMenuItem("Preferences...")); // Placeholder for future features
//        menu.add(new JMenuItem("About...")); // Placeholder for future features

        profilesButton.addActionListener(e -> menu.show(profilesButton, 0, profilesButton.getHeight()));

        rightPanel.add(themeButton);
        rightPanel.add(profilesButton);
        add(rightPanel, BorderLayout.EAST);

//        add(new JSeparator(), BorderLayout.SOUTH);
	}

//	private JComponent createSettingsBar(MainFrame owner) {
//		JButton settingsButton = new JButton();
//		try {
//			Image img = ImageIO.read(new File("view/resources/cogwheel.png"));
//			Image newImg = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
//			settingsButton.setIcon(new ImageIcon(newImg));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		settingsButton.setPreferredSize(new Dimension(60, 60));
//		settingsButton.setBorderPainted(false);
//		settingsButton.setFocusPainted(false);
//		settingsButton.setBackground(owner.getBackground());
//		settingsButton.setForeground(owner.getBackground());
//		settingsButton.setToolTipText("Settings");
//
//		settingsButton.addActionListener(e -> {
//			JFrame settingsFrame = new SettingsFrame(owner);
//			settingsFrame.setVisible(true);
//		});
//
//		JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
//		settingsPanel.add(settingsButton);
//		return settingsPanel;
//	}
}
