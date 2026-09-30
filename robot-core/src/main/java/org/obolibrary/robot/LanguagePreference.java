package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilities for choosing an annotation value (typically an rdfs:label) for an entity based on a
 * user-supplied, ordered list of preferred language tags.
 *
 * <p>Given a set of candidate values (each with a language tag), selection proceeds as:
 *
 * <ol>
 *   <li>Each value is <em>bound</em> to the preference entry its language tag matches most
 *       specifically. A match is either <em>exact</em> (equal tags) or a <em>cascade</em>, in which
 *       a broader entry matches a more specific tag (e.g. {@code en} matches {@code en-GB}); when a
 *       tag matches several entries, the longest (most specific) entry wins. So with {@code en,
 *       en-GB} a value tagged {@code en-GB} binds to the {@code en-GB} entry, while a value tagged
 *       {@code en-US} binds by cascade to {@code en}. The {@link #WILDCARD} ({@code *}) matches any
 *       tag but is less specific than every real tag.
 *   <li>The value bound to the <em>earliest</em> entry in the list wins. Listing a specific tag
 *       after a general one therefore deprioritizes it: with {@code en, en-GB}, {@code en-GB}
 *       labels are used only when nothing binds to {@code en}.
 *   <li>Among values bound to the same entry, an exact match beats a cascade match; if they still
 *       tie, the alphanumerically-first value is chosen (a deterministic tie-break, matching
 *       ROBOT's existing convention in {@code OntologyHelper.getAnnotationString}).
 *   <li>If no value matches any preferred language but at least one value exists, the
 *       alphanumerically-first value is chosen, so that a labelled entity never regresses to its
 *       IRI merely because its language is unlisted.
 * </ol>
 *
 * <p>The per-tag match rule (exact, or a prefix followed by {@code -}, plus the {@code *} wildcard)
 * is that of <a href="https://www.rfc-editor.org/rfc/rfc4647.html">RFC 4647</a> Basic Filtering.
 * This class then applies the list's priority order and the tie-breaks above to choose a single
 * value; that single-value selection is not RFC 4647 "Lookup" (Lookup would never let {@code en}
 * select an {@code en-GB} value), and the most-specific binding in rule 1 is a ROBOT extension.
 *
 * <p>The token {@link #NO_LANG_TOKEN} in a preference list refers to values that have no language
 * tag (the OWL API represents these with an empty language string).
 *
 * <p>Both the CLI token for "no language" and the command-line option name are defined here as
 * constants so that the spelling can be changed in one place.
 */
public class LanguagePreference {

  /** The token, used within a preference list, that matches values with no language tag. */
  public static final String NO_LANG_TOKEN = "none";

  /** The long name of the command-line option used to supply a language preference list. */
  public static final String OPTION_NAME = "label-langs-priority";

  /**
   * The RFC 4647 basic-filtering wildcard: as a preference entry it matches any language tag. It is
   * the least specific match, so any concrete tag match is preferred over it.
   */
  public static final String WILDCARD = "*";

  /** The OWL API's internal representation of "no language tag". */
  private static final String NO_LANG = "";

  /** A candidate annotation value together with its language tag. */
  public static class Candidate {

    /** The language tag, or the empty string for no language tag. Never null. */
    final String lang;

    /** The lexical value. */
    final String value;

    /**
     * Create a candidate value.
     *
     * @param lang the language tag (a null is treated as no language tag)
     * @param value the lexical value
     */
    public Candidate(String lang, String value) {
      this.lang = (lang == null) ? NO_LANG : lang;
      this.value = value;
    }
  }

  /** Prevent instantiation. */
  private LanguagePreference() {}

  /**
   * Parse a comma-separated preference string into an ordered list of language tags. The special
   * token {@link #NO_LANG_TOKEN} (case-insensitive) is mapped to the empty language tag. Blank
   * entries are ignored. A null or empty input yields an empty list, which means "no preference".
   *
   * @param csv comma-separated language tags, or null
   * @return an ordered list of language tags (the empty string represents "no language tag")
   */
  public static List<String> parse(String csv) {
    List<String> result = new ArrayList<>();
    if (csv == null) {
      return result;
    }
    for (String raw : csv.split(",")) {
      String token = raw.trim();
      if (token.isEmpty()) {
        continue;
      }
      if (token.equalsIgnoreCase(NO_LANG_TOKEN)) {
        result.add(NO_LANG);
      } else {
        result.add(token);
      }
    }
    return result;
  }

  /**
   * Choose the best value from the candidates according to the preferred languages, falling back to
   * the alphanumerically-first value if none match a preferred language.
   *
   * @param candidates the candidate values (may be empty)
   * @param preferredLangs the ordered list of preferred language tags (may be empty)
   * @return the chosen value, or null if there are no candidates
   */
  public static String selectValue(List<Candidate> candidates, List<String> preferredLangs) {
    String preferred = selectPreferred(candidates, preferredLangs);
    if (preferred != null) {
      return preferred;
    }
    return selectFallback(candidates);
  }

  /**
   * Choose the best value whose language tag matches one of the preferred languages. Returns null
   * if no candidate matches any preferred language (including when the preference list is empty).
   *
   * @param candidates the candidate values (may be empty)
   * @param preferredLangs the ordered list of preferred language tags (may be empty)
   * @return the best-matching value, or null if none matches a preferred language
   */
  public static String selectPreferred(List<Candidate> candidates, List<String> preferredLangs) {
    if (candidates == null || preferredLangs == null || preferredLangs.isEmpty()) {
      return null;
    }
    Candidate best = null;
    int[] bestKey = null;
    for (Candidate c : candidates) {
      int[] key = matchKey(c.lang, preferredLangs);
      if (key == null) {
        continue;
      }
      if (best == null || compare(key, c.value, bestKey, best.value) < 0) {
        best = c;
        bestKey = key;
      }
    }
    return (best == null) ? null : best.value;
  }

  /**
   * Choose the alphanumerically-first value, ignoring language. Used as a deterministic fallback
   * when no candidate matches a preferred language.
   *
   * @param candidates the candidate values (may be empty)
   * @return the alphanumerically-first value, or null if there are no candidates
   */
  public static String selectFallback(List<Candidate> candidates) {
    if (candidates == null) {
      return null;
    }
    Candidate best = null;
    for (Candidate c : candidates) {
      if (best == null || c.value.compareTo(best.value) < 0) {
        best = c;
      }
    }
    return (best == null) ? null : best.value;
  }

  /**
   * Compute a sortable ranking key describing how well a language tag matches a preference list, or
   * null if it does not match at all. Lower keys rank better.
   *
   * <p>The tag is first <em>bound</em> to the single preference entry it matches most specifically:
   * among all matching entries (exact, cascade, or the {@link #WILDCARD}), the one with the longest
   * tag is chosen, so a tag of {@code en-GB} binds to a listed {@code en-GB} rather than to a
   * broader {@code en}, and a tag of {@code en-GB-scouse} binds to a listed {@code en-GB} rather
   * than to {@code en}. This is what lets a specific tag be "deprioritized" by listing it after a
   * general one: with {@code en, en-GB} an {@code en-GB} label binds to the second entry, while an
   * {@code en-US} label binds (by cascade) to the first. The {@link #WILDCARD} matches any tag but
   * is less specific than every real tag, so a concrete match always binds in preference to it.
   *
   * <p>The returned key then ranks that binding for the cross-value comparison, components in
   * order:
   *
   * <ol>
   *   <li>the index of the bound entry, so that a label bound to an earlier entry wins;
   *   <li>match type: 0 for an exact match, 1 for a cascade (prefix) match, so that two labels
   *       bound to the same entry prefer the exact one.
   * </ol>
   *
   * @param lang the language tag to score (the empty string for no language tag)
   * @param preferredLangs the ordered list of preferred language tags
   * @return a ranking key, or null if the tag matches no preference entry
   */
  private static int[] matchKey(String lang, List<String> preferredLangs) {
    // Language tags are case-insensitive (BCP 47), and the OWL API reports them in lower case, so
    // compare case-insensitively.
    String lowerLang = lang.toLowerCase(Locale.ROOT);
    int bestIndex = -1;
    int bestType = 0;
    // Length of the matched preference tag; longer = more specific. The wildcard is less specific
    // than any real tag (including the empty "none" tag, whose length is 0), so it uses -1; the
    // initial "nothing matched yet" value must be lower still.
    int bestSpecificity = Integer.MIN_VALUE;
    for (int i = 0; i < preferredLangs.size(); i++) {
      String pref = preferredLangs.get(i).toLowerCase(Locale.ROOT);
      int type;
      int specificity;
      if (pref.equals(WILDCARD)) {
        // RFC 4647 basic filtering: "*" matches any tag. Least specific, and never an exact match.
        type = 1;
        specificity = -1;
      } else if (lowerLang.equals(pref)) {
        type = 0; // exact
        specificity = pref.length();
      } else if (!pref.isEmpty() && lowerLang.startsWith(pref + "-")) {
        type = 1; // cascade: a broader preference matches a more specific tag
        specificity = pref.length();
      } else {
        continue;
      }
      // Bind the tag to the most specific entry it matches. On a specificity tie prefer an exact
      // match; iterating in order keeps the earliest entry when everything else is equal.
      if (specificity > bestSpecificity || (specificity == bestSpecificity && type < bestType)) {
        bestSpecificity = specificity;
        bestType = type;
        bestIndex = i;
      }
    }
    if (bestIndex < 0) {
      return null;
    }
    return new int[] {bestIndex, bestType};
  }

  /**
   * Compare two ranking keys with the candidate value as a final tie-break.
   *
   * @param keyA first key
   * @param valueA value for the first key
   * @param keyB second key
   * @param valueB value for the second key
   * @return negative if A ranks before B, positive if after, zero if identical
   */
  private static int compare(int[] keyA, String valueA, int[] keyB, String valueB) {
    int cmp = compare(keyA, keyB);
    if (cmp != 0) {
      return cmp;
    }
    return valueA.compareTo(valueB);
  }

  /**
   * Compare two ranking keys component by component.
   *
   * @param keyA first key
   * @param keyB second key
   * @return negative if A ranks before B, positive if after, zero if identical
   */
  private static int compare(int[] keyA, int[] keyB) {
    for (int i = 0; i < keyA.length && i < keyB.length; i++) {
      int cmp = Integer.compare(keyA[i], keyB[i]);
      if (cmp != 0) {
        return cmp;
      }
    }
    return Integer.compare(keyA.length, keyB.length);
  }
}
