package com.pcariou.view.main.center;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class FormGrid extends JPanel {

    public final JTextField creditFile = new JTextField();
    public final JButton browseCredit = new JButton("Browse");

    public final JTextField outputFile = new JTextField();
    public final JButton browseOutput = new JButton("Browse");

    public final JTextField execDate = new JTextField();
    public final JButton pickDate = new JButton("…");

    public FormGrid() {
        super(new MigLayout(
                "fillx, insets 0, wrap 3",
                "[right]12[grow,fill]10[]",
                "[]12[]12[]"
        ));
        setOpaque(false);

        Dimension browseSize = new Dimension(90, 30);
        browseCredit.setPreferredSize(browseSize);
        browseOutput.setPreferredSize(browseSize);
        pickDate.setPreferredSize(new Dimension(44, 30));

        creditFile.setEditable(false);
        outputFile.setEditable(false);

        add(new JLabel("Credit transfer file"));
        add(creditFile, "growx");
        add(browseCredit, "right, wrap");

        add(new JLabel("SEPA output file"));
        add(outputFile, "growx");
        add(browseOutput, "right, wrap");

        add(new JLabel("Execution date"));
        add(execDate, "growx");
        add(pickDate, "right");
    }
}