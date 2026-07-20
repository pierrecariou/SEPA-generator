package com.pcariou.view.update;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Fetches and parses the static Community update manifest over HTTPS.
 *
 * <p>Uses short connect/read timeouts so a slow or unreachable network never
 * holds up the caller. It performs a single plain {@code GET} of a static file;
 * it does not call any API or send identifying information beyond a generic
 * user agent.</p>
 */
public class UpdateManifestClient {

    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 4000;
    private static final int MAX_BODY_CHARS = 64 * 1024;
    private static final String USER_AGENT = "SEPA-Generator-Community";

    private final Gson gson = new Gson();

    /**
     * Downloads and parses the manifest at {@code manifestUrl}.
     *
     * @throws IOException if the request fails, times out, returns a non-200
     *                     status, or the body is not a valid manifest
     */
    public UpdateInfo fetch(String manifestUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP status " + status + " for " + manifestUrl);
            }
            try (InputStream in = connection.getInputStream()) {
                return parse(readBody(in));
            }
        } finally {
            connection.disconnect();
        }
    }

    /** Parses a manifest JSON document; visible for testing. */
    public UpdateInfo parse(String json) throws IOException {
        UpdateInfo info;
        try {
            info = gson.fromJson(json, UpdateInfo.class);
        } catch (JsonParseException malformed) {
            throw new IOException("Malformed manifest JSON", malformed);
        }
        if (info == null) {
            throw new IOException("Manifest is empty");
        }
        if (!info.isCommunityEdition()) {
            throw new IOException("Manifest edition is not \"" + UpdateInfo.EDITION_COMMUNITY
                    + "\" (was: " + info.getEdition() + ")");
        }
        if (!info.hasUsableVersion()) {
            throw new IOException("Manifest is missing a usable latest version");
        }
        if (!info.hasUsableDownloadUrl()) {
            throw new IOException("Manifest has no usable download URL");
        }
        return info;
    }

    private static String readBody(InputStream in) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                body.append(buffer, 0, read);
                if (body.length() > MAX_BODY_CHARS) {
                    throw new IOException("Manifest is unexpectedly large");
                }
            }
        }
        return body.toString();
    }
}
