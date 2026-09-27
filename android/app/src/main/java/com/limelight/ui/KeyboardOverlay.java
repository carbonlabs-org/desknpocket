/* SPDX-License-Identifier: GPL-3.0-or-later
 * Custom Moonlight IME fork, 2026-09-23.
 */
package com.limelight.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.StreamInputConnection;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

import java.util.ArrayList;
import java.util.List;

/** In-activity controls only: no system overlay permission or accessibility service. */
public final class KeyboardOverlay implements StreamInputConnection.Sink {
    private final Activity activity;
    private final StreamView stream;
    private final LinearLayout panel;
    private final HorizontalScrollView toolbar;
    private final List<Button> modifiers = new ArrayList<>();
    private NvConnection connection;
    private boolean active;
    private boolean keyboardRequested;
    private boolean imeVisible;
    private int armed;

    public KeyboardOverlay(Activity activity, StreamView stream) {
        this.activity = activity;
        this.stream = stream;
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.END);
        LinearLayout toggles = new LinearLayout(activity);
        Button keyboard = button("⌨", activity.getString(R.string.ime_toggle));
        keyboard.setTextSize(24);
        keyboard.setOnClickListener(v -> toggleKeyboard());
        toggles.addView(keyboard, new LinearLayout.LayoutParams(dp(56), dp(48)));
        Button keys = button("⋯", activity.getString(R.string.ime_toolbar));
        toggles.addView(keys, new LinearLayout.LayoutParams(dp(48), dp(48)));
        panel.addView(toggles);
        toolbar = new HorizontalScrollView(activity);
        toolbar.setFillViewport(false);
        toolbar.setHorizontalScrollBarEnabled(true);
        toolbar.setVisibility(View.GONE);
        LinearLayout row = new LinearLayout(activity);
        toolbar.addView(row);
        panel.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        keys.setOnClickListener(v -> {
            boolean show = toolbar.getVisibility() != View.VISIBLE;
            toolbar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) clearModifiers();
        });
        addKey(row, "Esc", 0x1B);
        addModifier(row, "Ctrl", KeyboardPacket.MODIFIER_CTRL);
        addModifier(row, "Alt", KeyboardPacket.MODIFIER_ALT);
        addModifier(row, "Win", KeyboardPacket.MODIFIER_META);
        addModifier(row, "Shift", KeyboardPacket.MODIFIER_SHIFT);
        addKey(row, "Tab", 0x09);
        addKey(row, "↑", 0x26);
        addKey(row, "↓", 0x28);
        addKey(row, "←", 0x25);
        addKey(row, "→", 0x27);
        addKey(row, "Del", 0x2E);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        params.setMargins(dp(8), dp(8), dp(8), 0);
        ((FrameLayout) activity.findViewById(android.R.id.content)).addView(panel, params);
        panel.setVisibility(View.GONE);
        // Keep controls above the IME, including landscape/floating-keyboard configurations.
        // Top placement avoids covering the bottom gamepad controls and survives adjustNothing.
        panel.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect frame = new Rect();
            stream.getWindowVisibleDisplayFrame(frame);
            boolean visible = stream.getRootView().getHeight() - frame.bottom > dp(100);
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsets insets = stream.getRootWindowInsets();
                if (insets != null) visible = insets.isVisible(WindowInsets.Type.ime());
            }
            if (imeVisible && !visible) {
                keyboardRequested = false;
                clearModifiers();
                stream.resetImeContext();
            }
            imeVisible = visible;
            int width = Math.max(dp(104), Math.min(dp(640), stream.getRootView().getWidth() - dp(16)));
            ViewGroup.LayoutParams lp = toolbar.getLayoutParams();
            if (lp.width != width) { lp.width = width; toolbar.setLayoutParams(lp); }
        });
        stream.setImeSink(this);
    }

    private int dp(int n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }

    private Button button(String label, String description) {
        Button b = new Button(activity);
        b.setText(label);
        b.setContentDescription(description);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setFocusable(false);
        b.setFocusableInTouchMode(false);
        style(b, false);
        return b;
    }

    private void style(Button b, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? 0xFF176B9B : 0xD9222630);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), selected ? 0xFF8CD5FF : 0xFF657080);
        b.setBackground(bg);
        b.setSelected(selected);
    }

    private void addKey(LinearLayout row, String label, int vk) {
        Button b = button(label, label);
        b.setOnClickListener(v -> {
            stream.resetImeContext();
            key(vk);
        });
        row.addView(b, new LinearLayout.LayoutParams(dp(48), dp(48)));
    }

    private void addModifier(LinearLayout row, String label, int flag) {
        Button b = button(label, label + " — " + activity.getString(R.string.ime_one_shot));
        b.setTag(flag);
        b.setOnClickListener(v -> {
            armed ^= flag;
            refreshModifiers();
            stream.setShortcutMode(armed != 0);
        });
        modifiers.add(b);
        row.addView(b, new LinearLayout.LayoutParams(dp(52), dp(48)));
    }

    private void refreshModifiers() {
        for (Button b : modifiers) style(b, (armed & (Integer) b.getTag()) != 0);
    }

    public void clearModifiers() {
        if (armed != 0) {
            armed = 0;
            refreshModifiers();
            stream.setShortcutMode(false);
        }
    }

    public void setConnection(NvConnection connection, boolean active) {
        this.connection = connection;
        this.active = active;
        panel.setVisibility(active ? View.VISIBLE : View.GONE);
        if (!active) suspend();
    }

    public void setHidden(boolean hidden) {
        panel.setVisibility(active && !hidden ? View.VISIBLE : View.GONE);
        if (hidden) suspend();
    }

    public void suspend() {
        clearModifiers();
        keyboardRequested = false;
        stream.resetImeContext();
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(stream.getWindowToken(), 0);
    }

    public void remoteFocusChanged() {
        if (keyboardRequested || imeVisible) stream.resetImeContext();
        clearModifiers();
    }

    public boolean handleBack(KeyEvent event) {
        if (!keyboardRequested && !imeVisible) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) suspend();
        return true;
    }

    public void toggleKeyboard() {
        if (!active) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imeVisible || keyboardRequested) {
            suspend();
        } else {
            stream.requestFocus();
            stream.resetImeContext();
            keyboardRequested = true;
            stream.post(() -> {
                if (!imm.showSoftInput(stream, InputMethodManager.SHOW_IMPLICIT)) keyboardRequested = false;
            });
        }
    }

    @Override public boolean isActive() { return active && connection != null && activity.hasWindowFocus(); }

    @Override public void text(String text) {
        if (!isActive()) return;
        if (armed == 0) {
            // Unicode protocol packets are for printable text. Enter/Tab need key packets.
            StringBuilder printable = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n' || c == '\r' || c == '\t') {
                    if (printable.length() > 0) {
                        connection.sendUtf8Text(printable.toString());
                        printable.setLength(0);
                    }
                    if (c != '\r' || i + 1 == text.length() || text.charAt(i + 1) != '\n')
                        key(c == '\t' ? 0x09 : 0x0D);
                } else printable.append(c);
            }
            if (printable.length() > 0) connection.sendUtf8Text(printable.toString());
            return;
        }
        // Shortcuts are physical key packets, never Unicode-with-Ctrl. Restrict to unambiguous keys.
        if (text.length() == 1) {
            char c = Character.toUpperCase(text.charAt(0));
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ') {
                key(c);
                stream.post(stream::resetImeContext);
                return;
            }
        }
        clearModifiers();
        Toast.makeText(activity, R.string.ime_shortcut_hint, Toast.LENGTH_SHORT).show();
        stream.post(stream::resetImeContext);
    }

    @Override public void key(int vk) {
        if (!isActive()) return;
        int flags = armed;
        // Arm locally; hold on the host only for the duration of a single key pair.
        int[] bits = {KeyboardPacket.MODIFIER_CTRL, KeyboardPacket.MODIFIER_ALT,
                KeyboardPacket.MODIFIER_META, KeyboardPacket.MODIFIER_SHIFT};
        int[] codes = {0xA2, 0xA4, 0x5B, 0xA0};
        int held = 0;
        try {
            for (int i = 0; i < bits.length; i++) if ((flags & bits[i]) != 0) {
                held |= bits[i];
                send(codes[i], true, held);
            }
            send(vk, true, flags);
            send(vk, false, flags);
        } finally {
            for (int i = bits.length - 1; i >= 0; i--) if ((held & bits[i]) != 0) {
                held &= ~bits[i];
                send(codes[i], false, held);
            }
            if (flags != 0) {
                // Do not retire the InputConnection from inside its mutation callback.
                armed = 0;
                refreshModifiers();
                stream.post(() -> stream.setShortcutMode(false));
            }
        }
    }

    private void send(int vk, boolean down, int flags) {
        connection.sendKeyboardInput((short) (0x8000 | vk),
                down ? KeyboardPacket.KEY_DOWN : KeyboardPacket.KEY_UP, (byte) flags, (byte) 0);
    }
}
