/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.core;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.frostwire.android.gui.services.Engine;
import com.frostwire.util.JsonUtils;
import com.frostwire.util.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Looking for default config values? look at {@link ConfigurationDefaults}
 *
 * @author gubatron
 * @author aldenml
 */
public class ConfigurationManager {
    private static final Logger LOG = Logger.getLogger(ConfigurationManager.class);
    private static ConfigurationManager instance;
    private static AtomicReference state = new AtomicReference(State.CREATING);
    private final static CountDownLatch creationLatch = new CountDownLatch(1);
    private SharedPreferences preferences;
    private Editor editor;
    private ConfigurationDefaults defaults;

    private enum State {
        CREATING,
        CREATED
    }

    public synchronized static void create(Application context) {
        if (State.CREATED == state.get()  && instance != null) {
            return;
        }
        instance = new ConfigurationManager(context);
    }

    public static ConfigurationManager instance() {
        if (state.get() == State.CREATING) {
            try {
                creationLatch.await();
            } catch (InterruptedException e) {
                if (instance == null) {
                    throw new RuntimeException("ConfigurationManager not created, creationLatch thread interrupted");
                }
            }
        }
        if (State.CREATED == state.get() && instance == null) {
            throw new RuntimeException("ConfigurationManager not created");
        }
        return instance;
    }

    private ConfigurationManager(Application context) {
        Engine.instance().getThreadPool().execute(new Initializer(this, context));
    }

    private static class Initializer implements Runnable {
        private final ConfigurationManager cm;
        private final Application applicationRef;
        Initializer(ConfigurationManager configurationManager, Application application) {
            cm = configurationManager;
            applicationRef = application;
        }

        @Override
        public void run() {
            try {
                cm.preferences = PreferenceManager.getDefaultSharedPreferences(applicationRef);
                cm.editor = cm.preferences.edit();
                cm.defaults = new ConfigurationDefaults();

                cm.initPreferences(cm.preferences, cm.editor);
                cm.editor.apply();
            }
            catch (Throwable t) {
                LOG.error("Error initializing ConfigurationManager", t);
                throw t;
            } finally {
                state.set(State.CREATED);
                creationLatch.countDown();
            }
        }
    }

    public String getString(String key, String defValue) {
        if (preferences == null) {
            LOG.error("getString(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getString(key="+key+") failed, preferences:SharedPreferences is null");
        }
        return preferences.getString(key, defValue);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public void setString(String key, String value) {
        try {
            setString(editor, key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setString(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public int getInt(String key, int defValue) {
        if (preferences == null) {
            LOG.error("getInt(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getInt(key="+key+") failed, preferences:SharedPreferences is null");
        }
        return preferences.getInt(key, defValue);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public void setInt(String key, int value) {
        try {
            setInt(editor, key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setInt(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public long getLong(String key, long defValue) {
        if (preferences == null) {
            LOG.error("getLong(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getLong(key="+key+") failed, preferences:SharedPreferences is null");
        }
        return preferences.getLong(key, defValue);
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public void setLong(String key, long value) {
        try {
            setLong(editor, key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setLong(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public boolean getBoolean(String key) {
        if (preferences == null) {
            LOG.error("getBoolean(key=" + key + ") preferences == null");
            throw new IllegalStateException("getBoolean(key="+key+") failed, preferences:SharedPreferences is null");
        }
        return preferences.getBoolean(key, false);
    }

    public void setBoolean(String key, boolean value) {
        try {
            setBoolean(editor, key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setBoolean(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public void resetToDefaults() {
        // TODO: remove this when editor variable is gone
        if (editor == null) {
            throw new IllegalStateException("Shared preferences editor can't be null at this moment");
        }
        resetToDefaults(editor, defaults.getDefaultValues());
        editor.apply();
    }

    private void resetToDefault(String key) {
        if (defaults != null) {
            Map<String, Object> defaultValues = defaults.getDefaultValues();
            if (defaultValues != null && defaultValues.containsKey(key)) {
                Object defaultValue = defaultValues.get(key);
                initPreference(preferences, editor, key, defaultValue, true);
            }
        }
    }

    public String getUUIDString() {
        return getString(Constants.PREF_KEY_CORE_UUID);
    }

    public int getLastMediaTypeFilter() {
        return getInt(Constants.PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER);
    }

    public void setLastMediaTypeFilter(int mediaTypeId) {
        setInt(Constants.PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER, mediaTypeId);
    }

    public boolean vibrateOnFinishedDownload() {
        return getBoolean(Constants.PREF_KEY_GUI_VIBRATE_ON_FINISHED_DOWNLOAD);
    }

    public String[] getStringArray(String key) {
        String s = getString(key);
        return s != null ? JsonUtils.toObject(s, String[].class) : null;
    }

    public void setStringArray(String key, String[] values) {
        try {
            setStringArray(editor, key, values).apply();
        } catch (Throwable ignore) {
            LOG.warn("setStringArray(key=" + key + ", value=" + values + ") failed", ignore);
        }
    }

    public boolean showTransfersOnDownloadStart() {
        return getBoolean(Constants.PREF_KEY_GUI_SHOW_TRANSFERS_ON_DOWNLOAD_START);
    }

    public void registerOnPreferenceChange(OnSharedPreferenceChangeListener listener) {
        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public void unregisterOnPreferenceChange(OnSharedPreferenceChangeListener listener) {
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    public String getStoragePath() {
        return getString(Constants.PREF_KEY_STORAGE_PATH);
    }

    public void setStoragePath(String path) {
        if (path != null && path.length() > 0) { // minor verifications
            setString(Constants.PREF_KEY_STORAGE_PATH, path);
        }
    }

    public boolean isSeedFinishedTorrents() {
        return getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
    }

    public void setSeedFinishedTorrents(boolean value) {
        setBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS, value);
    }

    public boolean isSeedingEnabledOnlyForWifi() {
        return getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
    }

    private void initPreferences(@NonNull SharedPreferences preferences, @NonNull Editor editor) {
        for (Entry<String, Object> entry : defaults.getDefaultValues().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            initPreference(preferences, editor, key, value, false);
        }

        //there are some configuration values that need to be reset every time to a desired value
        resetToDefaults(editor, defaults.getResetValues());
    }

    private void initPreference(@NonNull SharedPreferences preferences, @NonNull Editor editor,
                                String key, Object value, boolean force) {
        if (preferences.contains(key) && !force) {
            return; // quick return
        }
        setPreference(editor, key, value);
    }

    private void resetToDefaults(@NonNull Editor editor, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            setPreference(editor, entry.getKey(), entry.getValue());
        }
    }

    private void setPreference(@NonNull Editor editor, String key, Object value) {
        if (value instanceof String) {
            setString(editor, key, (String) value);
        } else if (value instanceof Integer) {
            setInt(editor, key, (Integer) value);
        } else if (value instanceof Long) {
            setLong(editor, key, (Long) value);
        } else if (value instanceof Boolean) {
            setBoolean(editor, key, (Boolean) value);
        } else if (value instanceof String[]) {
            setStringArray(editor, key, (String[]) value);
        } else {
            throw new RuntimeException("Unsupported data type for setting: " +
                    "key = " + key + ", value = " + (value != null ? value.getClass() : "null"));
        }
    }

    private Editor setString(Editor editor, String key, String value) {
        return editor.putString(key, value);
    }

    private Editor setInt(Editor editor, String key, int value) {
        return editor.putInt(key, value);
    }

    private Editor setLong(Editor editor, String key, long value) {
        return editor.putLong(key, value);
    }

    private Editor setBoolean(Editor editor, String key, boolean value) {
        return editor.putBoolean(key, value);
    }

    private Editor setStringArray(Editor editor, String key, String[] values) {
        return setString(editor, key, JsonUtils.toJson(values));
    }
}
