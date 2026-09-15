package com.bunbunka.programmernotebook;

import org.junit.Test;

import static org.junit.Assert.*;

public class NoteLogicTest {
    @Test
    public void blankNoteIsNotPersisted() {
        assertFalse(NoteLogic.shouldPersist("   ", "\n\t"));
        assertTrue(NoteLogic.shouldPersist("Java tips", ""));
        assertTrue(NoteLogic.shouldPersist("", "System.out.println();"));
    }

    @Test
    public void searchMatchesTitleOrContentIgnoringCase() {
        assertTrue(NoteLogic.matches("Kotlin Coroutines", "launch sample", "COROUTINES"));
        assertTrue(NoteLogic.matches("Kotlin Coroutines", "launch sample", "SAMPLE"));
        assertFalse(NoteLogic.matches("Kotlin Coroutines", "launch sample", "python"));
        assertTrue(NoteLogic.matches("Anything", "Anything", "   "));
    }

    @Test
    public void previewIsCompactAndBounded() {
        String text = "line one\nline two    with spaces";
        assertEquals("line one line two with spaces", NoteLogic.preview(text, 40));
        assertEquals("1234567…", NoteLogic.preview("123456789", 8));
    }
}
