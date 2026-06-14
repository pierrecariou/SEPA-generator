package com.pcariou.view;

import com.pcariou.model.PainVersion;

import java.time.LocalDate;

public interface IGenerator {
    void generate(String inputFile, String outputFile, LocalDate date, PainVersion version);

    /** Historical entry point; kept for compatibility — defaults to pain.001.001.02. */
    default void generate(String inputFile, String outputFile, LocalDate date) {
        generate(inputFile, outputFile, date, PainVersion.PAIN_001_001_02);
    }
}
