package com.pcariou.view.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single entry point for reading and writing the application configuration file
 * ({@code ~/.sepa-generator-config.json}).
 *
 * <p>Centralises the config-file location, JSON (de)serialisation and the small
 * set of domain queries the UI needs, so the view classes never touch the file
 * directly. The location can be overridden with the {@code sepa.config.file}
 * system property (used by tests and kept consistent with the service layer);
 * production behaviour is unchanged when it is not set.
 */
public final class ConfigStore {

    private static final Logger LOGGER = Logger.getLogger(ConfigStore.class.getName());
    private static final String OVERRIDE_PROPERTY = "sepa.config.file";
    private static final String DEFAULT_FILE_NAME = ".sepa-generator-config.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Resolves the config file, honouring the {@code sepa.config.file} override. */
    public File file() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return new File(override);
        }
        return new File(System.getProperty("user.home"), DEFAULT_FILE_NAME);
    }

    /** Reads the config, or {@code null} if it is missing or unreadable. */
    public AppConfig read() {
        File f = file();
        if (!f.exists()) {
            return null;
        }
        try (FileReader r = new FileReader(f)) {
            return gson.fromJson(r, AppConfig.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read config from " + f.getAbsolutePath(), e);
            return null;
        }
    }

    /** Writes the given config, overwriting the file. Returns {@code true} on success. */
    public boolean write(AppConfig config) {
        File f = file();
        try (FileWriter w = new FileWriter(f)) {
            gson.toJson(config, w);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to write config to " + f.getAbsolutePath(), e);
            return false;
        }
    }

    /** True when all mandatory debtor and initiating-party fields are present. */
    public boolean isDebtorConfigured() {
        AppConfig cfg = read();
        return cfg != null
                && cfg.debtor != null
                && notBlank(cfg.debtor.name)
                && notBlank(cfg.debtor.iban)
                && notBlank(cfg.debtor.bic)
                && cfg.initiatingParty != null
                && notBlank(cfg.initiatingParty.name)
                && notBlank(cfg.initiatingParty.siret);
    }

    /**
     * Returns the last-used input directory if it still exists, otherwise the
     * user's home directory (never {@code null}).
     */
    public File lastInputDirectory() {
        AppConfig cfg = read();
        if (cfg != null && cfg.fileSettings != null) {
            String path = cfg.fileSettings.defaultInputPath;
            if (path != null && !path.trim().isEmpty()) {
                File dir = new File(path);
                if (dir.isDirectory()) {
                    return dir;
                }
            }
        }
        return new File(System.getProperty("user.home"));
    }

    /**
     * Persists {@code dir} as the last-used input directory, preserving all other
     * config fields. No-op if {@code dir} is null or not a directory.
     */
    public void saveLastInputDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        AppConfig cfg = read();
        if (cfg == null) {
            cfg = new AppConfig();
        }
        if (cfg.fileSettings == null) {
            cfg.fileSettings = new AppConfig.FileSettings();
        }
        cfg.fileSettings.defaultInputPath = dir.getAbsolutePath();
        write(cfg);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Returns the persisted theme name (e.g. {@code "LIGHT"} / {@code "DARK"}), or {@code null} if unset. */
    public String readTheme() {
        AppConfig cfg = read();
        if (cfg != null && cfg.appearance != null) {
            return cfg.appearance.theme;
        }
        return null;
    }

    /**
     * Persists {@code theme} as the last-used appearance theme, preserving all
     * other config fields. No-op if {@code theme} is blank.
     */
    public void saveTheme(String theme) {
        if (theme == null || theme.trim().isEmpty()) {
            return;
        }
        AppConfig cfg = read();
        if (cfg == null) {
            cfg = new AppConfig();
        }
        if (cfg.appearance == null) {
            cfg.appearance = new AppConfig.Appearance();
        }
        cfg.appearance.theme = theme;
        write(cfg);
    }
}
