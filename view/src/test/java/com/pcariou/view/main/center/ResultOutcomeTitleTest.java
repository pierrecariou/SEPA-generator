package com.pcariou.view.main.center;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * G2 result-presentation integration — the generation result surface is titled
 * by outcome rather than the generic "Generation summary". Community has no
 * warning states, so it only ever shows the clean-success title (plus the
 * neutral generating/stale headings during regeneration and after a failure).
 */
public class ResultOutcomeTitleTest {

    @Test
    public void cleanSuccessTitleIsOutcomeOriented() {
        assertEquals("File generated successfully", FormPanel.RESULT_TITLE_SUCCESS);
    }

    @Test
    public void staleAndGeneratingTitlesAreNeutralAndCannotReadAsSuccess() {
        assertEquals("Generating\u2026", FormPanel.RESULT_TITLE_GENERATING);
        assertEquals("No file generated \u2014 last attempt failed",
                FormPanel.RESULT_TITLE_STALE_FAILED);
    }
}
