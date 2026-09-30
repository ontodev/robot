package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Tests for {@link LanguagePreference}. */
public class LanguagePreferenceTest {

  /** Build a candidate list from alternating (lang, value) arguments. */
  private static List<LanguagePreference.Candidate> candidates(String... langValuePairs) {
    if (langValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("expected pairs of (lang, value)");
    }
    LanguagePreference.Candidate[] cs = new LanguagePreference.Candidate[langValuePairs.length / 2];
    for (int i = 0; i < cs.length; i++) {
      cs[i] = new LanguagePreference.Candidate(langValuePairs[2 * i], langValuePairs[2 * i + 1]);
    }
    return Arrays.asList(cs);
  }

  /** Parsing splits, trims, drops blanks, and maps the no-lang token to the empty string. */
  @Test
  public void testParse() {
    assertEquals(Arrays.asList("en-GB", "en", "fr"), LanguagePreference.parse("en-GB, en ,fr"));
    // The no-lang token becomes the empty string; case-insensitive.
    assertEquals(Arrays.asList("en", ""), LanguagePreference.parse("en,NONE"));
    // Blank entries (e.g. from a trailing comma) are dropped.
    assertEquals(Arrays.asList("en", "fr"), LanguagePreference.parse("en,,fr,"));
    // Null or empty means "no preference".
    assertEquals(Collections.emptyList(), LanguagePreference.parse(null));
    assertEquals(Collections.emptyList(), LanguagePreference.parse("  "));
  }

  /** An exact language match is preferred over other languages. */
  @Test
  public void testExactMatch() {
    List<LanguagePreference.Candidate> cs = candidates("de", "Hund", "en", "dog", "fr", "chien");
    assertEquals("dog", LanguagePreference.selectValue(cs, Arrays.asList("en", "fr")));
    assertEquals("chien", LanguagePreference.selectValue(cs, Arrays.asList("fr", "en")));
  }

  /** A broad preference cascades to a more specific tag (en matches en-GB). */
  @Test
  public void testCascadeMatch() {
    List<LanguagePreference.Candidate> cs = candidates("en-GB", "colour", "de", "Farbe");
    assertEquals("colour", LanguagePreference.selectValue(cs, Collections.singletonList("en")));
  }

  /** Within a single preference entry, an exact match beats a cascade match. */
  @Test
  public void testExactBeatsCascade() {
    // Preferring just "en": the bare-"en" label matches exactly, while the en-US label only
    // cascades from "en". Both attach to the same (only) entry, so the exact match wins.
    List<LanguagePreference.Candidate> cs = candidates("en", "generic", "en-US", "regional");
    assertEquals("generic", LanguagePreference.selectValue(cs, Collections.singletonList("en")));
    // With "en,en-GB" and only an en-GB label present, it is matched by its own entry.
    List<LanguagePreference.Candidate> gbOnly = candidates("en-GB", "colour");
    assertEquals("colour", LanguagePreference.selectValue(gbOnly, Arrays.asList("en", "en-GB")));
  }

  // A GB/US pair whose values are chosen so the en-GB label ("aluminium") sorts alphabetically
  // *before* the en-US label ("aluminum"). This makes the tie-break discriminating: a broken
  // implementation that merged both regions and fell back to alpha order would return the GB label,
  // so asserting the US label proves the region logic (not the alpha accident) is doing the work.
  private static List<LanguagePreference.Candidate> gbUsCandidates() {
    return candidates("en-GB", "aluminium", "en-US", "aluminum");
  }

  /** An exact regional preference selects that region only, never the sibling region. */
  @Test
  public void testExactRegionalPreference() {
    assertEquals(
        "aluminum",
        LanguagePreference.selectValue(gbUsCandidates(), Collections.singletonList("en-US")));
    assertEquals(
        "aluminium",
        LanguagePreference.selectValue(gbUsCandidates(), Collections.singletonList("en-GB")));
  }

  /**
   * A broad entry cascades only to the region that is <em>not</em> itself listed; a listed region
   * stays bound to its own (lower-priority) entry.
   */
  @Test
  public void testBroadPreferenceSkipsListedRegion() {
    // "en" (index 0) cascades to en-US (which is not listed), while en-GB is reserved for its own
    // entry at index 1. Because the first matching entry wins, the en-US label is chosen even
    // though it sorts after the en-GB label.
    assertEquals(
        "aluminum", LanguagePreference.selectValue(gbUsCandidates(), Arrays.asList("en", "en-GB")));
  }

  /** With only a broad entry and no listed region, both regions cascade and alpha order decides. */
  @Test
  public void testBroadPreferenceAmongRegionsUsesAlphaTieBreak() {
    assertEquals(
        "aluminium",
        LanguagePreference.selectValue(gbUsCandidates(), Collections.singletonList("en")));
  }

  /**
   * A tag binds to its most specific matching entry even when that tag is not itself listed. With
   * {@code en,en-GB}, a tag of {@code en-GB-scouse} binds (by cascade) to the more specific {@code
   * en-GB} entry rather than to the earlier {@code en}, so an {@code en-US} label — which can only
   * bind to {@code en} — is preferred.
   */
  @Test
  public void testMostSpecificCascadeBinding() {
    // Values are chosen so that a naive "earliest matching entry, then alphabetical" rule would
    // wrongly return "aaa" (the en-GB-scouse label).
    List<LanguagePreference.Candidate> cs = candidates("en-GB-scouse", "aaa", "en-US", "zzz");
    assertEquals("zzz", LanguagePreference.selectValue(cs, Arrays.asList("en", "en-GB")));
  }

  /** The "*" wildcard (RFC 4647 basic filtering) matches any tag as a catch-all. */
  @Test
  public void testWildcardCatchAll() {
    List<LanguagePreference.Candidate> cs = candidates("de", "Zebra", "fr", "Apfel");
    // No en label exists, but "*" matches both; among them the alphanumerically-first wins.
    assertEquals("Apfel", LanguagePreference.selectPreferred(cs, Arrays.asList("en", "*")));
    // Without the wildcard there is no preferred match at all.
    assertNull(LanguagePreference.selectPreferred(cs, Collections.singletonList("en")));
  }

  /** A concrete tag is more specific than the wildcard, so a listed language is preferred. */
  @Test
  public void testWildcardIsLeastSpecific() {
    List<LanguagePreference.Candidate> cs = candidates("en", "dog", "de", "Hund");
    // "en" binds the English label (exact, index 0); the German label only matches "*" (index 1).
    assertEquals("dog", LanguagePreference.selectValue(cs, Arrays.asList("en", "*")));
  }

  /** The wildcard also matches an untagged literal. */
  @Test
  public void testWildcardMatchesUntagged() {
    List<LanguagePreference.Candidate> cs = candidates((String) null, "plain");
    assertEquals("plain", LanguagePreference.selectValue(cs, Collections.singletonList("*")));
  }

  /** The no-lang token matches an untagged literal. */
  @Test
  public void testNoLangToken() {
    List<LanguagePreference.Candidate> cs = candidates("en", "tagged", null, "untagged");
    List<String> prefs = LanguagePreference.parse("none,en");
    assertEquals("untagged", LanguagePreference.selectValue(cs, prefs));
    // An empty preference entry must not cascade-match everything.
    List<LanguagePreference.Candidate> onlyTagged = candidates("de", "Hund");
    assertNull(LanguagePreference.selectPreferred(onlyTagged, LanguagePreference.parse("none")));
  }

  /** Multiple values in the same (preferred) language are broken alphanumerically. */
  @Test
  public void testSameLanguageAlphaTieBreak() {
    List<LanguagePreference.Candidate> cs = candidates("en", "zebra", "en", "apple", "en", "mango");
    assertEquals("apple", LanguagePreference.selectValue(cs, Collections.singletonList("en")));
  }

  /** When no candidate matches a preferred language, fall back to the alphanumerically-first. */
  @Test
  public void testFallbackWhenNoPreferredMatch() {
    List<LanguagePreference.Candidate> cs = candidates("de", "Zebra", "de", "Apfel");
    // No German preference given: selectPreferred finds nothing...
    assertNull(LanguagePreference.selectPreferred(cs, Arrays.asList("en", "fr")));
    // ...but selectValue still returns a deterministic, non-null label.
    assertEquals("Apfel", LanguagePreference.selectValue(cs, Arrays.asList("en", "fr")));
  }

  /**
   * An empty preference list yields no preferred match; selectValue falls back deterministically.
   */
  @Test
  public void testEmptyPreferenceList() {
    List<LanguagePreference.Candidate> cs = candidates("en", "beta", "fr", "alpha");
    assertNull(LanguagePreference.selectPreferred(cs, Collections.emptyList()));
    assertEquals("alpha", LanguagePreference.selectValue(cs, Collections.emptyList()));
  }

  /** Language-tag matching is case-insensitive (the OWL API reports tags in lower case). */
  @Test
  public void testCaseInsensitive() {
    // Candidate tag as the OWL API would report it (lower case), preference as a user might type
    // it.
    List<LanguagePreference.Candidate> cs = candidates("en-gb", "colour", "de", "Farbe");
    assertEquals("colour", LanguagePreference.selectValue(cs, Collections.singletonList("en-GB")));
    // And the reverse casing.
    List<LanguagePreference.Candidate> cs2 = candidates("EN", "hi", "fr", "salut");
    assertEquals("hi", LanguagePreference.selectValue(cs2, Collections.singletonList("en")));
  }

  /** No candidates yields null (caller falls back to the IRI/CURIE short form). */
  @Test
  public void testNoCandidates() {
    assertNull(LanguagePreference.selectValue(Collections.emptyList(), Arrays.asList("en")));
    assertNull(LanguagePreference.selectFallback(Collections.emptyList()));
  }
}
