package org.obolibrary.robot.reason;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.obolibrary.robot.reason.EquivalentClassReasoningMode.*;

import org.junit.Test;

/** Created by edouglass on 5/8/17. Tests for EquivalentClassReasoningMode */
public class EquivalentClassReasoningModeTest {

  /** Test transforming mode to command line format. Pass if all strings are the same. */
  @Test
  public void testWritten() {
    assertEquals("all", ALL.written());
    assertEquals("none", NONE.written());
    assertEquals("asserted-only", ASSERTED_ONLY.written());
  }

  /** Test to verify that the command line strings are valid modes. */
  @Test
  public void testValidateWrittenName() {
    assertTrue(ALL.validates("all"));
    assertTrue(NONE.validates("none"));
    assertTrue(ASSERTED_ONLY.validates("asserted-only"));
    assertFalse(ASSERTED_ONLY.validates("asserted_only"));
  }

  /** Test to verify backwards compatibility. */
  @Test
  public void testValidatesSynonyms() {
    assertTrue(ALL.validates("true"));
    assertTrue(NONE.validates("false"));
  }

  /** Test to verify that any other string will set to default (ALL). */
  @Test
  public void testFromDefault() {
    assertEquals(ALL, from("foo"));
  }

  /** Test to verify that the command line format modes will correctly implement the mode. */
  @Test
  public void testFrom() {
    assertEquals(ALL, from("all"));
    assertEquals(NONE, from("false"));
    assertEquals(ASSERTED_ONLY, from("asserted-only"));
  }
}
