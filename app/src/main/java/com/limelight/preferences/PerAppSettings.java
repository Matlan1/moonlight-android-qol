package com.limelight.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.limelight.R;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.utils.UiHelper;

/**
 * Editor for {@link AppSpecificSettings} — lets the user override resolution / FPS / bitrate for a
 * single app on a single PC. Backed by the per-app SharedPreferences file via the standard
 * preference framework (see {@link AppSpecificSettings#getAppPrefsName}).
 */
public class PerAppSettings extends Activity {
    public static final String EXTRA_PC_UUID = "PerAppUuid";
    public static final String EXTRA_APP_ID = "PerAppId";
    public static final String EXTRA_APP_NAME = "PerAppName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);

        setContentView(R.layout.activity_per_app_settings);

        String appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        if (appName != null) {
            setTitle(appName);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.per_app_settings, new SettingsFragment())
                    .commit();
        }

        UiHelper.notifyNewRootView(this);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String uuid = getActivity().getIntent().getStringExtra(EXTRA_PC_UUID);
            int appId = getActivity().getIntent().getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);

            // Initialize the per-app file from the current global settings on first open.
            AppSpecificSettings.seedFromGlobalIfNeeded(getActivity(), uuid, appId);

            // Back this fragment with the per-app SharedPreferences file. Must be set before
            // addPreferencesFromResource() so the preferences read/write the right file.
            getPreferenceManager().setSharedPreferencesName(
                    AppSpecificSettings.getAppPrefsName(uuid, appId));

            addPreferencesFromResource(R.xml.per_app_preferences);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            UiHelper.applyStatusBarPadding(view);
            return view;
        }
    }
}
