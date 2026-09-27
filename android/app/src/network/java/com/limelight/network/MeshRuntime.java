package com.limelight.network;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import libtailscale.Libtailscale;
import libtailscale.LocalAPIResponse;
import org.json.JSONObject;

/** One upstream backend per process. All blocking bridge calls run off the UI thread. */
final class MeshRuntime {
    private static MeshRuntime instance;
    static synchronized MeshRuntime get(Context context) {
        if (instance == null) instance = new MeshRuntime(context.getApplicationContext());
        return instance;
    }
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Context context;
    private libtailscale.Application backend;
    MeshAppContext callbacks;
    volatile String status = "Backend not started";
    volatile String serviceStatus = "not started";
    volatile boolean tunnelActive;
    private final AtomicBoolean inspecting = new AtomicBoolean();
    private volatile boolean loginRequested;
    private volatile String loginUrl = "";
    private volatile String loginMessage = "";
    private MeshRuntime(Context context) { this.context = context; }
    void initialize() throws Exception {
        if (backend != null) return;
        File directory = new File(context.getNoBackupFilesDir(), "mesh");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("Cannot create backend directory");
        callbacks = new MeshAppContext(context);
        go.Seq.setContext(context);
        backend = Libtailscale.start(directory.getAbsolutePath(), "", false, callbacks);
        callbacks.networks.ready();
        wantRunning(false);
        refresh();
    }
    private String request(String method, String path, String body) throws Exception {
        libtailscale.InputStream input = body == null ? null : new libtailscale.InputStream() {
            private boolean read;
            @Override public byte[] read() { if (read) return null; read = true; return body.getBytes(StandardCharsets.UTF_8); }
            @Override public void close() { }
        };
        LocalAPIResponse response = backend.callLocalAPI(5000, method, "/localapi/v0/" + path, input);
        byte[] bytes = response.bodyBytes();
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new java.io.IOException("Backend HTTP " + response.statusCode());
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }
    void wantRunning(boolean running) throws Exception {
        request("PATCH", "prefs", "{\"WantRunning\":" + running + ",\"WantRunningSet\":true}");
    }
    void refresh() throws Exception {
        JSONObject state = new JSONObject(request("GET", "status?peers=false", null));
        String backendState = state.optString("BackendState", "unknown");
        status = "Backend: " + backendState;
        if ("NeedsLogin".equals(backendState)) {
            status += "\nSign in to your PC's test network to establish a tunnel.";
        }
        if (loginRequested) {
            String url = state.optString("AuthURL", "");
            if (!url.isEmpty()) {
                loginUrl = LoginUrl.allowed(url) ? url : "";
                loginMessage = loginUrl.isEmpty() ? "Unrecognized sign-in address; browser launch blocked."
                        : "Tap Continue sign-in in browser, then return here.";
            } else {
                loginUrl = "";
                if ("Running".equals(backendState) || "Starting".equals(backendState)
                        || "NeedsMachineAuth".equals(backendState)) {
                    loginRequested = false;
                    loginMessage = "NeedsMachineAuth".equals(backendState)
                            ? "Sign-in received. This device needs approval from your network administrator."
                            : "Sign-in completed. Check tunnel status below.";
                }
            }
        }
    }
    void beginLogin() throws Exception {
        JSONObject current = new JSONObject(request("GET", "status?peers=false", null));
        String state = current.optString("BackendState", "");
        if (!"NeedsLogin".equals(state) && !"NoState".equals(state)) {
            loginMessage = "An account is already configured. Use Start VPN service to connect.";
            return;
        }
        if (loginRequested) return;
        loginRequested = true;
        loginUrl = "";
        loginMessage = "Requesting a sign-in link. If it does not appear, check Internet access and retry.";
        try {
            request("POST", "start", "{}");
            request("POST", "login-interactive", null);
            refresh();
        } catch (Exception error) {
            cancelLogin();
            loginMessage = "Could not start sign-in. Check Internet access and try again.";
            throw error;
        }
    }
    String loginUrl() { return loginRequested ? loginUrl : ""; }
    void cancelLogin() { loginRequested = false; loginUrl = ""; loginMessage = ""; }
    String displayStatus() {
        return "VPN service: " + serviceStatus + "\n" + status
                + "\nVPN tunnel: " + (tunnelActive ? "active" : "not established")
                + (loginMessage.isEmpty() ? "" : "\n" + loginMessage);
    }
    void inspect() {
        if (!inspecting.compareAndSet(false, true)) return;
        worker.execute(() -> {
            try { initialize(); refresh(); }
            catch (Exception | LinkageError error) { failed(error); }
            finally { inspecting.set(false); }
        });
    }
    void failed(Throwable error) {
        status = "Network operation failed (" + error.getClass().getSimpleName() + ").";
        // Exception messages may contain backend state, URLs or credentials.
    }
}
