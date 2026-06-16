package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-app stream setting overrides (resolution / FPS / bitrate).
 *
 * Each (PC, app) pair gets its own SharedPreferences file keyed by the computer UUID and app id.
 * The file reuses the same keys as the global stream settings ({@link PreferenceConfiguration})
 * so the per-app editor can be backed by the standard preference framework, plus an extra
 * "override enabled" flag. When the flag is set, {@link #applyOverrides} replaces the matching
 * fields on an already-populated (global) {@link PreferenceConfiguration} at launch time.
 */
public class AppSpecificSettings {
    static final String OVERRIDE_ENABLED_PREF_STRING = "checkbox_override_app_settings";

    public static String getAppPrefsName(String computerUuid, int appId) {
        return "app_specific_settings_" + computerUuid + "_" + appId;
    }

    private static SharedPreferences getAppPrefs(Context context, String computerUuid, int appId) {
        return context.getSharedPreferences(getAppPrefsName(computerUuid, appId), Context.MODE_PRIVATE);
    }

    /**
     * Populate the per-app file with the user's current global settings the first time it is
     * opened, so the editor starts from the global values rather than blank defaults. The
     * override flag is seeded to false (off) so existing apps are unaffected until the user
     * explicitly enables it.
     */
    public static void seedFromGlobalIfNeeded(Context context, String computerUuid, int appId) {
        if (computerUuid == null) {
            return;
        }

        SharedPreferences appPrefs = getAppPrefs(context, computerUuid, appId);
        if (appPrefs.contains(OVERRIDE_ENABLED_PREF_STRING)) {
            // Already initialized
            return;
        }

        PreferenceConfiguration global = PreferenceConfiguration.readPreferences(context);
        appPrefs.edit()
                .putBoolean(OVERRIDE_ENABLED_PREF_STRING, false)
                .putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, global.width + "x" + global.height)
                .putString(PreferenceConfiguration.FPS_PREF_STRING, Integer.toString(global.fps))
                .putInt(PreferenceConfiguration.BITRATE_PREF_STRING, global.bitrate)
                .apply();
    }

    public static boolean hasOverride(Context context, String computerUuid, int appId) {
        if (computerUuid == null) {
            return false;
        }
        return getAppPrefs(context, computerUuid, appId).getBoolean(OVERRIDE_ENABLED_PREF_STRING, false);
    }

    /**
     * Overwrite the resolution / FPS / bitrate fields of {@code prefConfig} with this app's
     * overrides, if the user has enabled them. No-op otherwise.
     */
    public static void applyOverrides(Context context, PreferenceConfiguration prefConfig,
                                      String computerUuid, int appId) {
        if (computerUuid == null) {
            return;
        }

        SharedPreferences appPrefs = getAppPrefs(context, computerUuid, appId);
        if (!appPrefs.getBoolean(OVERRIDE_ENABLED_PREF_STRING, false)) {
            return;
        }

        // Resolution is stored as a "WxH" string (same format as the global setting).
        String resStr = appPrefs.getString(PreferenceConfiguration.RESOLUTION_PREF_STRING, null);
        if (resStr != null && resStr.contains("x")) {
            String[] parts = resStr.split("x");
            try {
                int width = Integer.parseInt(parts[0]);
                int height = Integer.parseInt(parts[1]);
                if (width > 0 && height > 0) {
                    prefConfig.width = width;
                    prefConfig.height = height;
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                // Leave the global resolution in place on a malformed value
            }
        }

        String fpsStr = appPrefs.getString(PreferenceConfiguration.FPS_PREF_STRING, null);
        if (fpsStr != null) {
            try {
                int fps = Integer.parseInt(fpsStr);
                if (fps > 0) {
                    prefConfig.fps = fps;
                }
            } catch (NumberFormatException ignored) {
                // Leave the global FPS in place on a malformed value
            }
        }

        int bitrate = appPrefs.getInt(PreferenceConfiguration.BITRATE_PREF_STRING, -1);
        if (bitrate > 0) {
            prefConfig.bitrate = bitrate;
        }
    }
}
