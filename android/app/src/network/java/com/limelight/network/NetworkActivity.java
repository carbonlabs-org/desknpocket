package com.limelight.network;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import com.limelight.PcView;

/** User-driven enrollment and lifecycle checks for the embedded network backend. */
public final class NetworkActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private MeshRuntime runtime;
    private TextView state;
    private Button browser;
    private boolean pendingSignIn;
    private final Runnable redraw = new Runnable() {
        @Override public void run() {
            state.setText(runtime.displayStatus());
            browser.setEnabled(!runtime.loginUrl().isEmpty());
            runtime.inspect();
            main.postDelayed(this, 2000);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        runtime = MeshRuntime.get(this);
        pendingSignIn = saved != null && saved.getBoolean("pendingSignIn");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this); title.setText("Desknpocket Network Lab"); title.setTextSize(24); layout.addView(title);
        TextView note = new TextView(this);
        note.setText("Sign in using the test-network account your PC uses. Android permits one VPN at a time; approving this app may disconnect your current VPN. Complete sign-in in your browser, then return here.");
        layout.addView(note);
        state = new TextView(this); layout.addView(state);
        button(layout, "Check backend", runtime::inspect);
        button(layout, "Sign in", () -> prepareVpn(true));
        browser = button(layout, "Continue sign-in in browser", () -> {
            String url = runtime.loginUrl();
            if (!LoginUrl.allowed(url)) return;
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)); }
            catch (android.content.ActivityNotFoundException error) { runtime.status = "No browser is available for sign-in."; }
        });
        browser.setEnabled(false);
        button(layout, "Start VPN service", () -> prepareVpn(false));
        button(layout, "Stop VPN service", () -> {
            runtime.cancelLogin();
            if (stopService(new Intent(this, MeshVpnService.class))) {
                runtime.status = "VPN stop requested";
            } else {
                runtime.serviceStatus = "stopped";
                runtime.status = "VPN service is already stopped";
            }
        });
        button(layout, "Open desktops", () -> startActivity(new Intent(this, PcView.class)));
        ScrollView scroll = new ScrollView(this); scroll.addView(layout);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
    }
    private Button button(LinearLayout layout, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setOnClickListener(v -> action.run()); layout.addView(button);
        return button;
    }
    private void prepareVpn(boolean signIn) {
        pendingSignIn = signIn;
        Intent consent = VpnService.prepare(this);
        if (consent != null) startActivityForResult(consent, 61); else startVpn();
    }
    private void startVpn() {
        try { startForegroundService(new Intent(this, MeshVpnService.class)
                .setAction(pendingSignIn ? MeshVpnService.SIGN_IN : MeshVpnService.START)); }
        catch (RuntimeException error) { runtime.failed(error); }
        finally { pendingSignIn = false; }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 61) {
            if (result == RESULT_OK) startVpn();
            else { pendingSignIn = false; runtime.status = "VPN permission denied; service not started"; }
        }
    }
    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putBoolean("pendingSignIn", pendingSignIn); super.onSaveInstanceState(saved);
    }
    @Override protected void onResume() { super.onResume(); main.post(redraw); }
    @Override protected void onPause() { main.removeCallbacks(redraw); super.onPause(); }
}
