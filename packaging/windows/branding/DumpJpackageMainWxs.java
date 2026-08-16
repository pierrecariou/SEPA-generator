/*
 * Prints the main.wxs template that THIS JDK's jpackage would use by default.
 *
 * packaging/community/package-windows.ps1 uses it to prove that our branded
 * override (packaging/windows/branding/main.wxs) is still byte-identical to the
 * JDK's template apart from the branding block we deliberately add. Overriding
 * main.wxs is the supported jpackage mechanism for reaching WiX's
 * WixUIBannerBmp / WixUIDialogBmp variables, but it pins us to one JDK's
 * template; this check turns a silent drift on a JDK upgrade into a loud
 * packaging failure instead of a subtly broken installer.
 *
 * Usage (needs no compilation step, JDK 17+):
 *   java packaging/windows/branding/DumpJpackageMainWxs.java
 */
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class DumpJpackageMainWxs {

    private static final String RESOURCE =
            "/modules/jdk.jpackage/jdk/jpackage/internal/resources/main.wxs";

    public static void main(String[] args) throws Exception {
        var jrt = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of());
        Path resource = jrt.getPath(RESOURCE);
        if (!Files.isRegularFile(resource)) {
            throw new IllegalStateException("jpackage main.wxs not found in this JDK: " + RESOURCE);
        }
        // Write raw bytes so the comparison is not perturbed by line-ending or
        // charset translation on the way out.
        System.out.write(Files.readAllBytes(resource));
        System.out.flush();
    }

    private DumpJpackageMainWxs() {
    }
}
