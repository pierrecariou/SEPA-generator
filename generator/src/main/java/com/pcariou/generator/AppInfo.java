package com.pcariou.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppInfo {
    private static final String GROUP = "com.pcariou";
    private static final String ARTIFACT = "generator";
    private static final Properties props = new Properties();
    private static final String VERSION;

    static {
        String v = null;

        // 1) try manifest Implementation-Version
        Package pkg = AppInfo.class.getPackage();
        if (pkg != null) {
            v = pkg.getImplementationVersion();
        }

        // 2) try META-INF/maven/.../pom.properties
        if (v == null) {
            String pomPath = "META-INF/maven/" + GROUP + "/" + ARTIFACT + "/pom.properties";
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(pomPath)) {
                if (in != null) {
                    Properties pom = new Properties();
                    pom.load(in);
                    v = pom.getProperty("version");
                }
            } catch (IOException ignored) {
            }
        }

        try (InputStream in = AppInfo.class.getResourceAsStream("/app.properties")) {
            props.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }

        VERSION = v != null ? v : "unknown";
    }

    public static String getVersion() {
        return props.getProperty("version", "unknown");
    }
}