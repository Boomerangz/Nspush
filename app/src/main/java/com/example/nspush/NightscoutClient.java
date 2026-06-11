package com.example.nspush;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class NightscoutClient {
    private NightscoutClient() {
    }

    static NightscoutEntry fetchLatest(String baseUrl, String secretOrToken) throws Exception {
        String endpoint = AppConfig.normalizeBaseUrl(baseUrl) + "/api/v1/entries/sgv.json?count=1";
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        if (!TextUtils.isEmpty(secretOrToken)) {
            String trimmed = secretOrToken.trim();
            connection.setRequestProperty("api-secret", sha1(trimmed));
            connection.setRequestProperty("Authorization", "Bearer " + trimmed);
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Nightscout returned HTTP " + code + ": " + body);
        }

        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            throw new IllegalStateException("Nightscout returned no SGV entries");
        }
        JSONObject object = array.getJSONObject(0);
        return new NightscoutEntry(
                object.getInt("sgv"),
                object.optLong("date", System.currentTimeMillis()),
                object.optString("direction", "")
        );
    }

    private static String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String sha1(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format(Locale.US, "%02x", b));
        }
        return hex.toString();
    }
}
