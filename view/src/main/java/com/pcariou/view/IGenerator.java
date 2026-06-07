package com.pcariou.view;

import java.time.LocalDate;

public interface IGenerator {
    void generate(String inputFile, String outputFile, LocalDate date);
}
