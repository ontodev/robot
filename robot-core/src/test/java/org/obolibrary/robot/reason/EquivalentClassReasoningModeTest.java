package org.obolibrary.robot.reason;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.obolibrary.robot.reason.EquivalentClassReasoningMode.*;

/**
 * Created by edouglass on 5/8/17.
 * Tests for EquivalentClassReasoningMode
 */
public class EquivalentClassReasoningModeTest {

    @Test
    public void testWritten() {
        assertEquals("all", ALL.written());
        assertEquals("none", NONE.written());
        assertEquals("asserted-only", ASSERTED_ONLY.written());
    }

    @Test
    public void testValidateWrittenName() {
        assertTrue(ALL.validates("all"));
        assertTrue(NONE.validates("none"));
        assertTrue(ASSERTED_ONLY.validates("asserted-only"));
        assertFalse(ASSERTED_ONLY.validates("asserted_only"));
    }

    @Test
    public void testValidatesSynonyms() {
        assertTrue(ALL.validates("true"));
        assertTrue(NONE.validates("false"));
    }

    @Test
    public void testFromDefault() {
        assertEquals(ALL, from("foo"));
    }

    @Test
    public void testFrom() {
        assertEquals(ALL, from("all"));
        assertEquals(NONE, from("false"));
        assertEquals(ASSERTED_ONLY, from("asserted-only"));
    }
}
