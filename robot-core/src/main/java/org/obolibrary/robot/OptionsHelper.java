package org.obolibrary.robot;

import java.util.Map;

/** Provides convenience methods for getting options. */
public class OptionsHelper {

  /**
   * Given a map of options and a key name, return the value, or null if it is not specified.
   *
   * @param options a map of options
   * @param key the name of the option to get
   * @return the value, if set, otherwise null
   */
  public static String getOption(Map<String, String> options, String key) {
    return getOption(options, key, null);
  }

  /**
   * Given a map of options, a key name, and a default value, if the map contains the key, return
   * its value, otherwise return the default value.
   *
   * @param options a map of options
   * @param key the name of the option to get
   * @param defaultValue the value to return if the key is not set
   * @return the value, if set, otherwise the default value
   */
  public static String getOption(Map<String, String> options, String key, String defaultValue) {
    if (options == null) {
      return defaultValue;
    }
    if (!options.containsKey(key)) {
      return defaultValue;
    }
    return options.get(key);
  }

  /**
   * Given a map of options and a key name, return true if the value is "true" or "yes", otherwise
   * return false.
   *
   * @param options a map of options
   * @param key the name of the option to get
   * @return true if the value is "true" or "yes", false otherwise
   */
  static boolean optionIsTrue(Map<String, String> options, String key) {
    String value = getOption(options, key);
    if (value == null) {
      return false;
    }

    value = value.trim().toLowerCase();
    return value.equals("true") || value.equals("yes");
  }
}
