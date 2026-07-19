package com.pcariou.view.main.center;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * G1 visual coherence pass — the Community generation form is grouped into two
 * labelled sections in a fixed order (Community has no payment-type selector, so
 * there is no Payment section). The actual visual placement is verified in manual
 * acceptance; this locks the intended section set and order.
 */
public class FormSectionOrderTest {

    @Test
    public void communityFormIsGroupedIntoInputOutputInOrder() {
        assertEquals(Arrays.asList("Input", "Output"),
                FormPanel.formSectionOrder());
    }
}
