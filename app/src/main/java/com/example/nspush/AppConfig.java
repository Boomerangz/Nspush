package com.example.nspush;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class AppConfig {
    static final String PREFS = "nightscout_settings";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_BASE_URL = "base_url";
    static final String KEY_SECRET = "secret";
    static final String KEY_INTERVAL_MINUTES = "interval_minutes";
    static final String KEY_LOW = "low_threshold";
    static final String KEY_HIGH = "high_threshold";
    static final String KEY_NOTIFY_EVERY_CHECK = "notify_every_check";

    static final int DEFAULT_INTERVAL_MINUTES = 15;
    static final int DEFAULT_LOW = 70;
    static final int DEFAULT_HIGH = 180;

    private final SharedPreferences prefs;

    private AppConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static AppConfig from(Context context) {
        return new AppConfig(context);
    }

    boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, "");
    }

    String getSecret() {
        return prefs.getString(KEY_SECRET, "");
    }

    int getIntervalMinutes() {
        return prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES);
    }

    int getLowThreshold() {
        return prefs.getInt(KEY_LOW, DEFAULT_LOW);
    }

    int getHighThreshold() {
        return prefs.getInt(KEY_HIGH, DEFAULT_HIGH);
    }

    boolean shouldNotifyEveryCheck() {
        return prefs.getBoolean(KEY_NOTIFY_EVERY_CHECK, true);
    }

    void save(String baseUrl, String secret, int intervalMinutes, int low, int high, boolean notifyEveryCheck, boolean enabled) {
        prefs.edit()
                .putString(KEY_BASE_URL, normalizeBaseUrl(baseUrl))
                .putString(KEY_SECRET, secret == null ? "" : secret.trim())
                .putInt(KEY_INTERVAL_MINUTES, Math.max(getMinimumIntervalMinutes(), intervalMinutes))
                .putInt(KEY_LOW, low)
                .putInt(KEY_HIGH, high)
                .putBoolean(KEY_NOTIFY_EVERY_CHECK, notifyEveryCheck)
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    static int getMinimumIntervalMinutes() {
        return (int) TimeUnit.MILLISECONDS.toMinutes(androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS);
    }

    static String normalizeBaseUrl(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    static boolean isValidUrl(String input) {
        try {
            URL url = new URL(normalizeBaseUrl(input));
            String protocol = url.getProtocol().toLowerCase(Locale.US);
            return ("https".equals(protocol) || "http".equals(protocol)) && url.getHost() != null && !url.getHost().isEmpty();
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
