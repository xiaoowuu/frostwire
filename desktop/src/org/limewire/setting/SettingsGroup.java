package org.limewire.setting;

import org.limewire.setting.evt.SettingsGroupListener;

/**
 * Defines an abstract class to reload and save a value, revert to a 
 * default value and mark a value as always saving. 
 * <p>
 * If saving is turned off, then underlying settings will not be saved. If 
 * saving is turned on, then underlying settings still have the option not
 * to save settings to disk.
 */
public interface SettingsGroup {

    /**
     * Loads Settings from disk
     */
    void reload();

    /**
     * Saves the current Settings to disk
     */
    boolean save();

    /**
     * Reverts all Settings to their default values
     */
    boolean revertToDefault();

    /**
     * Adds the given {@link SettingsGroupListener}
     */
    void addSettingsGroupListener(SettingsGroupListener l);

    /**
     * Removes the given {@link SettingsGroupListener}
     */
    void removeSettingsGroupListener(SettingsGroupListener l);

    /**
     * Sets whether or not all Settings should be saved
     */
    void setShouldSave(boolean shouldSave);

    /** 
     * Access for shouldSave
     */
    boolean getShouldSave();

}