package com.pcariou.view.custom;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.common.UI;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class FlatDatePickerField extends JPanel {

    private final DatePicker engine;
    private final JTextField field;
    private final JButton btn;

    private final DateTimeFormatter formatter;

    public FlatDatePickerField(DatePickerSettings settings, Dimension btnPreferredSize) {
        super(new BorderLayout());

        // Engine date picker (kept in hierarchy so popup works)
        this.engine = new DatePicker(settings);
        engine.setOpaque(false);
        engine.setBorder(null);

        // Hide engine UI so it cannot affect visuals/layout
        engine.getComponentDateTextField().setVisible(false);
//        engine.getComponentToggleCalendarButton().setVisible(false);

        // Make it take effectively zero space in layout
        Dimension zero = new Dimension(0, 0);
        engine.setPreferredSize(zero);
        engine.setMinimumSize(zero);
        engine.setMaximumSize(zero);

        // Visible field (your controlled UI)
        this.field = new JTextField();
        field.setEditable(false);
        field.setFocusable(false);

        // Force consistent height (this kills clipping permanently)
//        field.putClientProperty(FlatClientProperties.STYLE, "minimumHeight: " + minHeightPx + ";");

        // Trailing button inside field
        this.btn = engine.getComponentToggleCalendarButton();
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, btn);

//        btn.size
        btn.setPreferredSize(btnPreferredSize);
//        btn.addActionListener(e -> {
//            engine.openPopup();
//        });

        // Format
        this.formatter = DateTimeFormatter.ofPattern("MMMM d, uuuu"); // or use your locale/format

        // Sync engine -> visible field
        engine.addDateChangeListener(event -> {
            LocalDate d = event.getNewDate();
            field.setText(d == null ? "" : formatter.format(d));
        });

        // Layout: visible field + hidden engine
        add(field, BorderLayout.CENTER);
        add(engine, BorderLayout.WEST);
    }

    public LocalDate getDate() { return engine.getDate(); }
    public void setDate(LocalDate date) { engine.setDate(date); }
    public JTextField getTextField() { return field; }
}
