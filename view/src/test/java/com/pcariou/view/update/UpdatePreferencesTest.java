package com.pcariou.view.update;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the pure once-per-day decision used to throttle automatic checks.
 */
public class UpdatePreferencesTest {

    @Test
    public void dueWhenNeverChecked() {
        assertTrue(UpdatePreferences.isDue(Long.MIN_VALUE, 20_000L));
    }

    @Test
    public void dueWhenLastCheckWasAnEarlierDay() {
        assertTrue(UpdatePreferences.isDue(19_999L, 20_000L));
    }

    @Test
    public void notDueWhenAlreadyCheckedToday() {
        assertFalse(UpdatePreferences.isDue(20_000L, 20_000L));
    }

    @Test
    public void notDueWhenClockMovedBackwards() {
        assertFalse(UpdatePreferences.isDue(20_001L, 20_000L));
    }
}
