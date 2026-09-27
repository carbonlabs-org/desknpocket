/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.limelight.binding.input;

import android.view.KeyEvent;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35}, manifest = Config.NONE)
public class StreamInputConnectionTest {
    private StreamInputConnection input;
    private final StringBuilder remote = new StringBuilder();
    private final List<Integer> keys = new ArrayList<>();
    private boolean active;
    @Before public void setup() {
        active = true;
        input = new StreamInputConnection(new View(RuntimeEnvironment.getApplication()), new StreamInputConnection.Sink() {
            public boolean isActive() { return active; }
            public void text(String text) { remote.append(text); }
            public void key(int vk) {
                keys.add(vk);
                if (vk == 8 && remote.length() > 0) remote.delete(remote.offsetByCodePoints(remote.length(), -1), remote.length());
            }
        });
    }
    @Test public void ordinaryTypingAndCommitDoNotDuplicate() {
        input.setComposingText("h", 1);
        assertEquals("h", remote.toString());
        input.setComposingText("hello", 1);
        input.commitText("hello", 1);
        input.commitText(" ", 1);
        input.commitText("WORLD 123!? @#$%", 1);
        assertEquals("hello WORLD 123!? @#$%", remote.toString());
    }
    @Test public void suggestionReplacesComposition() {
        input.setComposingText("teh", 1);
        input.commitText("the ", 1);
        assertEquals("the ", remote.toString());
    }
    @Test public void finishCompositionKeepsLastWordExactlyOnce() {
        input.setComposingText("word", 1);
        input.finishComposingText();
        input.finishComposingText();
        assertEquals("word", remote.toString());
    }
    @Test public void swipeAndVoicePhrases() {
        input.commitText("A swiped word ", 1);
        input.setComposingText("voice", 1);
        input.setComposingText("voice dictation works", 1);
        input.commitText("Voice dictation works.", 1);
        assertEquals("A swiped word Voice dictation works.", remote.toString());
    }
    @Test public void backspaceInsideCompositionAndOutsideContext() {
        remote.append("existing ");
        input.setComposingText("cat", 1);
        input.deleteSurroundingText(1, 0);
        assertEquals("existing ca", remote.toString());
        input.setComposingText("car", 1);
        input.commitText("car", 1);
        input.deleteSurroundingText(4, 0);
        assertEquals("existing", remote.toString());
    }
    @Test public void codePointDeletionNeverSplitsSurrogate() {
        input.commitText("A😀", 1);
        input.deleteSurroundingTextInCodePoints(1, 0);
        assertEquals("A", remote.toString());
    }
    @Test public void utf16DeletionNeverSplitsSurrogate() {
        input.commitText("A😀", 1);
        input.deleteSurroundingText(1, 0);
        assertEquals("A", remote.toString());
    }
    @Test public void batchCorrectionProducesFinalResult() {
        input.commitText("bad", 1);
        input.beginBatchEdit();
        input.deleteSurroundingText(3, 0);
        input.commitText("good", 1);
        input.endBatchEdit();
        assertEquals("good", remote.toString());
    }
    @Test public void composingRegionSupportsPreviousWordCorrection() {
        input.commitText("hello wurld", 1);
        assertTrue(input.setComposingRegion(6, 11));
        input.commitText("world", 1);
        assertEquals("hello world", remote.toString());
    }
    @Test public void emptyCompositionRemovesOldComposition() {
        input.setComposingText("gone", 1);
        input.setComposingText("", 1);
        assertEquals("", remote.toString());
    }
    @Test public void staleConnectionCannotTypeOrDelete() {
        input.setComposingText("keep", 1);
        input.retire();
        assertFalse(input.commitText("late", 1));
        assertFalse(input.deleteSurroundingText(4, 0));
        assertEquals("keep", remote.toString());
    }
    @Test public void disconnectedInputIsIgnored() {
        active = false;
        assertFalse(input.commitText("ignored", 1));
        assertTrue(remote.length() == 0);
    }
    @Test public void keyEventsAndEditorActionProduceSingleKey() {
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A));
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A));
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        assertEquals("a", remote.toString());
        assertEquals(1, keys.size());
        assertEquals(Integer.valueOf(13), keys.get(0));
    }
    @Test public void newlineCommitDoesNotDuplicateComposingWord() {
        input.setComposingText("hello", 1);
        input.commitText("hello\n", 1);
        assertEquals("hello\n", remote.toString());
        assertEquals(0, input.getEditable().length());
    }
    @Test public void unknownRemoteSelectionIsNotInvented() {
        input.commitText("hello", 1);
        assertFalse(input.setSelection(1, 2));
        assertEquals("hello", remote.toString());
    }
    @Test public void unicodeDeltaHandlesSharedSurrogatePrefix() {
        ImeTextDelta delta = ImeTextDelta.between("A😀", "A😁");
        assertEquals(1, delta.backspaces);
        assertEquals("😁", delta.text);
    }
    @Test public void imeArrowUsesSinglePairAndDropsOldContext() {
        input.commitText("word", 1);
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
        input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals(1, keys.size());
        assertEquals(Integer.valueOf(0x25), keys.get(0));
        assertEquals(0, input.getEditable().length());
    }
    @Test public void modernImeOverloadsReachSameImplementation() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            input.setComposingText("test", 1, null);
            input.commitText("tested", 1, null);
            assertEquals("tested", remote.toString());
        }
    }
}
