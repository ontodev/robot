package org.obolibrary.robot.reason;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * Created by edouglass on 5/5/17.
 *
 * <p>Enum of the three different equivalent class reasoning strategies. ALL means all equivalent
 * classes detected are allowed. NONE means any equivalent class should fail, and ASSERTED_ONLY
 * means that only if the inferred equivalence class has been previously asserted directly in the
 * ontology is the equivalence class allowed.
 *
 * <p>This is also how the command line options are detected. For backwards compatibility, the old
 * "true" is a synonym for ALL, and "false" is a synonym for NONE. The #validates(String) method
 * detects if the given String is a synonym or the uncapitalized name of the enum instance. For
 * example, either "all" or "true" would validate to the instance ALL.
 *
 * <p>Lastly, one of the enum instances can easily be returned from a string with the static
 * #from(String) method. This is like a constructor. If the given string validates to any of the
 * enum instances, then that instance is returned. By default ALL is returned. This is how a Mode
 * instance should be retrieved from a string (say from the command line).
 */
public enum EquivalentClassReasoningMode {

  /** Allows all equivalent classes in the reason step. This is the default */
  ALL("All equivalent class axioms are allowed", "true"),

  /** Does not allow any detected equivalent classes. */
  NONE("No equivalent class axioms are allowed", "false"),

  /**
   * Only allows directly asserted equivalent classes. If there are equivalent classes inferred that
   * do are not already asserted in the ontology, then this is considered an error.
   */
  ASSERTED_ONLY(
      "Only equivalent classes that have been asserted are allowed."
          + " Inferred equivalencies are forbidden.");

  /** ALL by default. */
  private static EquivalentClassReasoningMode DEFAULT = ALL;

  /** List of synonyms. */
  private List<String> synonyms;

  /** Explanation String. */
  private String explanation;

  /**
   * @param explanation String
   * @param synonyms String...
   */
  EquivalentClassReasoningMode(String explanation, String... synonyms) {
    this.synonyms = Lists.newArrayList(synonyms);
    this.explanation = explanation;
  }

  /**
   * Get the explanation.
   *
   * @return explanation
   */
  public String getExplanation() {
    return explanation;
  }

  /**
   * Change mode to command line format.
   *
   * @return command-line String
   */
  public String written() {
    return name().replace('_', '-').toLowerCase();
  }

  /**
   * Checks if the given string counts as this mode. A string validates to a mode if s is the
   * written form or one of the synonyms of this Mode.
   *
   * @param s String to validate
   * @return True if the given string s is valid, False if not.
   */
  public boolean validates(String s) {
    return s.equals(this.written()) || synonyms.contains(s);
  }

  /**
   * Canonical way to find make a Mode instance from a String (say from a command line argument).
   * Tries to validate the string against one of the instances and if a mode instance is found, it's
   * returned. If no match is found, ALL is returned by default.
   *
   * @param s String to match to a Mode instance.
   * @return Mode enum instance that matches the String, or ALL by default.
   */
  public static EquivalentClassReasoningMode from(String s) {
    for (EquivalentClassReasoningMode mode : EquivalentClassReasoningMode.values()) {
      if (mode.validates(s)) {
        return mode;
      }
    }
    return DEFAULT;
  }
}
