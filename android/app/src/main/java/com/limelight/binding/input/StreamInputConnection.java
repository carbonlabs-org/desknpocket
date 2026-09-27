/* SPDX-License-Identifier: GPL-3.0-or-later
 * Custom Moonlight IME fork, 2026-09-23.
 */
package com.limelight.binding.input;

import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

/**
 * Full editor semantics for IMEs, backed by a bounded local suffix, never the host document.
 * Composing changes are mirrored immediately; committing the same word sends nothing twice.
 * Reset the context whenever a pointer/desktop key might move the remote caret.
 */
public final class StreamInputConnection extends BaseInputConnection {
    public interface Sink {
        boolean isActive();
        void text(String text);
        void key(int windowsVk);
    }

    private final View view;
    private final Sink sink;
    private String mirrored = "";
    private int batchDepth;
    private boolean closed;

    public StreamInputConnection(View view, Sink sink) {
        super(view, true);
        this.view = view;
        this.sink = sink;
        Selection.setSelection(getEditable(), 0);
    }

    private boolean active() { return !closed && sink.isActive(); }

    private void sync() {
        if (!active() || batchDepth != 0) return;
        Editable e = getEditable();
        // Relative editing is anchored at the end of this locally known suffix.
        Selection.setSelection(e, e.length());
        String next = e.toString();
        ImeTextDelta delta = ImeTextDelta.between(mirrored, next);
        for (int i = 0; i < delta.backspaces; i++) sink.key(0x08);
        if (!delta.text.isEmpty()) sink.text(delta.text);
        mirrored = next;
        if (delta.text.indexOf('\n') >= 0 || delta.text.indexOf('\r') >= 0 || delta.text.indexOf('\t') >= 0)
            clearContext();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.updateSelection(view, e.length(), e.length(), getComposingSpanStart(e), getComposingSpanEnd(e));
    }

    @Override public boolean beginBatchEdit() {
        if (!active()) return false;
        batchDepth++;
        return true;
    }

    @Override public boolean endBatchEdit() {
        if (!active()) return false;
        if (batchDepth > 0) batchDepth--;
        sync();
        return batchDepth > 0;
    }

    @Override public boolean setComposingText(CharSequence text, int position) {
        if (!active() || text == null) return false;
        super.setComposingText(text, 1);
        sync();
        return true;
    }

    @Override public boolean commitText(CharSequence text, int position) {
        if (!active() || text == null) return false;
        super.commitText(text, 1);
        sync();
        if (getEditable().length() > 2048 && batchDepth == 0) clearContext();
        return true;
    }

    @Override public boolean finishComposingText() {
        if (!active()) return false;
        super.finishComposingText();
        sync();
        return true;
    }

    @Override public boolean setComposingRegion(int start, int end) {
        if (!active() || start < 0 || end < start || end > getEditable().length()) return false;
        return super.setComposingRegion(start, end);
    }

    @Override public boolean setSelection(int start, int end) {
        // Host cursor/selection cannot be queried over GameStream. Do not pretend that it can.
        if (!active() || start != getEditable().length() || end != start) return false;
        return super.setSelection(start, end);
    }

    @Override public boolean deleteSurroundingText(int before, int after) {
        return delete(before, after, false);
    }

    @Override public boolean deleteSurroundingTextInCodePoints(int before, int after) {
        return delete(before, after, true);
    }

    private boolean delete(int before, int after, boolean codePoints) {
        if (!active() || before < 0 || after < 0) return false;
        flush();
        Editable e = getEditable();
        int available = codePoints ? Character.codePointCount(e, 0, e.length()) : e.length();
        int count = Math.min(before, available);
        int start = codePoints ? Character.offsetByCodePoints(e, e.length(), -count) : e.length() - count;
        if (start > 0 && start < e.length() && Character.isLowSurrogate(e.charAt(start))) start--;
        // Backspace must also work inside an in-progress word.
        e.delete(start, e.length());
        Selection.setSelection(e, e.length());
        flush();
        // Anything preceding our local context still belongs to the focused Windows app.
        for (int i = 0; i < Math.min(before - count, 2048); i++) sink.key(0x08);
        for (int i = 0; i < Math.min(after, 2048); i++) sink.key(0x2E);
        return true;
    }

    @Override public boolean performEditorAction(int action) {
        if (!active()) return false;
        finishComposingText();
        flush();
        sink.key(0x0D);
        clearContext();
        return true;
    }

    @Override public boolean sendKeyEvent(KeyEvent event) {
        if (!active()) return false;
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getCharacters() != null)
            return commitText(event.getCharacters(), 1);
        int vk = 0;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: vk = 0x25; break;
            case KeyEvent.KEYCODE_DPAD_UP: vk = 0x26; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: vk = 0x27; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: vk = 0x28; break;
            case KeyEvent.KEYCODE_ESCAPE: vk = 0x1B; break;
            case KeyEvent.KEYCODE_MOVE_HOME: vk = 0x24; break;
            case KeyEvent.KEYCODE_MOVE_END: vk = 0x23; break;
            case KeyEvent.KEYCODE_PAGE_UP: vk = 0x21; break;
            case KeyEvent.KEYCODE_PAGE_DOWN: vk = 0x22; break;
        }
        int unicode = event.getUnicodeChar();
        boolean printable = unicode > 0 && Character.isValidCodePoint(unicode);
        boolean handled = vk != 0 || printable || event.getKeyCode() == KeyEvent.KEYCODE_DEL
                || event.getKeyCode() == KeyEvent.KEYCODE_FORWARD_DEL
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_TAB;
        if (!handled) return super.sendKeyEvent(event); // Preserve both halves of unmapped key pairs.
        if (event.getAction() == KeyEvent.ACTION_UP) return true;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (vk != 0) {
            flush(); sink.key(vk); clearContext(); return true;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL: return deleteSurroundingText(1, 0);
            case KeyEvent.KEYCODE_FORWARD_DEL: return deleteSurroundingText(0, 1);
            case KeyEvent.KEYCODE_ENTER: return performEditorAction(EditorInfo.IME_ACTION_NONE);
            case KeyEvent.KEYCODE_TAB:
                flush(); sink.key(0x09); clearContext(); return true;
            default:
                if (printable)
                    return commitText(new String(Character.toChars(unicode)), 1);
                return super.sendKeyEvent(event);
        }
    }

    private void flush() {
        int saved = batchDepth;
        batchDepth = 0;
        sync();
        batchDepth = saved;
    }

    private void clearContext() {
        getEditable().clear();
        removeComposingSpans(getEditable());
        Selection.setSelection(getEditable(), 0);
        mirrored = "";
        if (active()) {
            ((InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .updateSelection(view, 0, 0, -1, -1);
        }
    }

    /** Retire stale IME callbacks before remote focus changes. Never erase host text. */
    public void retire() {
        closed = true;
        clearContext();
    }

    @Override public void closeConnection() {
        retire();
        super.closeConnection();
    }
}
