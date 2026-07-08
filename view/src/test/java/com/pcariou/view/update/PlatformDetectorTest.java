package com.pcariou.view.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Verifies OS/arch mapping to the manifest download keys.
 */
public class PlatformDetectorTest {

    @Test
    public void windowsMapsToWindowsX64() {
        assertEquals("windows-x64", PlatformDetector.keyFor("Windows 11", "amd64"));
    }

    @Test
    public void macAppleSiliconMapsToArm() {
        assertEquals("macos-arm64", PlatformDetector.keyFor("Mac OS X", "aarch64"));
    }

    @Test
    public void macIntelMapsToX64() {
        assertEquals("macos-x64", PlatformDetector.keyFor("Mac OS X", "x86_64"));
    }

    @Test
    public void linuxMapsToLinuxX64() {
        assertEquals("linux-x64", PlatformDetector.keyFor("Linux", "amd64"));
    }

    @Test
    public void unknownOsIsNull() {
        assertNull(PlatformDetector.keyFor("Solaris", "sparc"));
        assertNull(PlatformDetector.keyFor(null, "amd64"));
    }
}
