package com.pcariou.view.custom;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public final class FlatDatePickerField extends JPanel {

    private final DatePicker engine;
    private final JTextField field;
    private final JButton btn;

    private final DateTimeFormatter formatter;

    public FlatDatePickerField(LocalDate firstAllowedDate, LocalDate lastAllowedDate, Dimension btnPreferredSize) {
        super(new BorderLayout());

        // Engine date picker (kept in hierarchy so popup works)
        this.engine = new DatePicker(new DatePickerSettings());
        engine.getSettings().setDateRangeLimits(firstAllowedDate, lastAllowedDate);

        engine.setOpaque(false);
        engine.setBorder(null);

        // Hide engine UI so it cannot affect visuals/layout
        engine.getComponentDateTextField().setVisible(false);

        // Make it take effectively zero space in layout
        Dimension zero = new Dimension(0, 0);
        engine.setPreferredSize(zero);
        engine.setMinimumSize(zero);
        engine.setMaximumSize(zero);

        // Visible field (controlled UI)
        this.field = new JTextField();
        field.setEditable(false);
        field.setFocusable(false);

        // Trailing button inside field
        this.btn = engine.getComponentToggleCalendarButton();
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, btn);
        btn.setPreferredSize(btnPreferredSize);

        // Locale-aware long date format (e.g. "9 June 2026" / "June 9, 2026")
        this.formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

        // Sync engine -> visible field
        engine.addDateChangeListener(event -> {
            LocalDate d = event.getNewDate();
            field.setText(d == null ? "" : formatter.format(d));
        });

        // Layout: visible field + hidden engine
        add(field, BorderLayout.CENTER);
        add(engine, BorderLayout.WEST);
    }

    public DatePicker getEngine() { return engine; }
    public LocalDate getDate() { return engine.getDate(); }
    public void setDate(LocalDate date) { engine.setDate(date); }
    public JTextField getTextField() { return field; }
}
