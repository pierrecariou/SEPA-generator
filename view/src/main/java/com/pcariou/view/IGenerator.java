package com.pcariou.view;

import java.time.LocalDate;
import java.util.Date;

public interface IGenerator {
    void generate(String inputFile, String outputFile, LocalDate date);
}
