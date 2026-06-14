package com.pcariou.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Access to the bundled input templates shipped on the classpath.
 *
 * <p>Templates help new users start from a valid input file structure. They are
 * copied locally on demand and are never fetched from the network.</p>
 */
public final class InputTemplates {

    /** A bundled template kind exposed to the user. */
    public enum Kind {
        CSV_BASIC(
                "Basic CSV template",
                "/templates/sepa-template-basic.csv",
                "sepa-template-basic.csv"),
        EXCEL_BASIC(
                "Basic Excel template",
                "/templates/sepa-template-basic.xlsx",
                "sepa-template-basic.xlsx"),
        CSV_ADDRESS(
                "CSV + optional addresses (.09)",
                "/templates/sepa-template-with-optional-address.csv",
                "sepa-template-with-optional-address.csv"),
        EXCEL_ADDRESS(
                "Excel + optional addresses (.09)",
                "/templates/sepa-template-with-optional-address.xlsx",
                "sepa-template-with-optional-address.xlsx");

        private final String label;
        private final String resourcePath;
        private final String defaultFileName;

        Kind(String label, String resourcePath, String defaultFileName) {
            this.label = label;
            this.resourcePath = resourcePath;
            this.defaultFileName = defaultFileName;
        }

        public String label()           { return label; }
        public String resourcePath()    { return resourcePath; }
        public String defaultFileName() { return defaultFileName; }
    }

    private InputTemplates() {
    }

    /** Returns {@code true} if the bundled resource for the given kind is present on the classpath. */
    public static boolean isAvailable(Kind kind) {
        return InputTemplates.class.getResource(kind.resourcePath()) != null;
    }

    /**
     * Copies the bundled template for {@code kind} to {@code target}, replacing any existing file.
     *
     * @throws IOException if the resource is missing or the copy fails
     */
    public static void copyTo(Kind kind, Path target) throws IOException {
        try (InputStream in = InputTemplates.class.getResourceAsStream(kind.resourcePath())) {
            if (in == null) {
                throw new IOException("Bundled template not found: " + kind.resourcePath());
            }
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }
}
