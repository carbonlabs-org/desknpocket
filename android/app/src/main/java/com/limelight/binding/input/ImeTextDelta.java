/* SPDX-License-Identifier: GPL-3.0-or-later
 * Custom Moonlight IME fork, 2026-09-23.
 */
package com.limelight.binding.input;

/** A remote editor has no readable document. Reconcile only our locally known suffix. */
public final class ImeTextDelta {
    public final int backspaces;
    public final String text;

    private ImeTextDelta(int backspaces, String text) {
        this.backspaces = backspaces;
        this.text = text;
    }

    public static ImeTextDelta between(String before, String after) {
        int prefix = 0;
        while (prefix < before.length() && prefix < after.length()) {
            int a = before.codePointAt(prefix);
            int b = after.codePointAt(prefix);
            if (a != b) break;
            prefix += Character.charCount(a);
        }
        return new ImeTextDelta(before.codePointCount(prefix, before.length()), after.substring(prefix));
    }
}
