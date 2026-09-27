// VPN bridge adapted from Tailscale Android IPNService.kt and VPNServiceBuilder.kt.
// Copyright (c) Tailscale Inc & AUTHORS. SPDX-License-Identifier: BSD-3-Clause
package com.limelight.network;

import android.app.*;
import android.content.Intent;
import android.net.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.system.OsConstants;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import libtailscale.Libtailscale;

public final class MeshVpnService extends VpnService implements libtailscale.IPNService {
    static final String START = "local.desknpocket.START_MESH";
    static final String SIGN_IN = "local.desknpocket.SIGN_IN";
    private final String serviceId = UUID.randomUUID().toString();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Handler main = new Handler(Looper.getMainLooper());
    private MeshRuntime runtime;
    private boolean attached; // accessed only by the runtime worker
    @Override public void onCreate() { super.onCreate(); runtime = MeshRuntime.get(this); }
    @Override public String id() { return serviceId; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || (!START.equals(intent.getAction()) && !SIGN_IN.equals(intent.getAction()))
                || prepare(this) != null || closed.get()) {
            close(); return START_NOT_STICKY;
        }
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("mesh", "Private network", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, NetworkActivity.class), PendingIntent.FLAG_IMMUTABLE);
        startForeground(9201, new Notification.Builder(this, "mesh")
                .setContentTitle("Desknpocket private network")
                .setContentText("Open to inspect or stop the connection")
                .setSmallIcon(android.R.drawable.stat_sys_upload).setContentIntent(open).setOngoing(true).build());
        runtime.serviceStatus = "running; attaching backend";
        runtime.worker.execute(() -> {
            try {
                if (closed.get()) return;
                runtime.initialize();
                if (closed.get()) return;
                if (!attached) {
                    runtime.callbacks.networks.service = this;
                    Libtailscale.requestVPN(this);
                    attached = true;
                }
                if (!closed.get()) runtime.serviceStatus = "running";
                if (!closed.get()) runtime.wantRunning(true);
                if (!closed.get() && SIGN_IN.equals(intent.getAction())) runtime.beginLogin();
                runtime.refresh();
            } catch (Exception | LinkageError error) {
                close();
                // Preserve the failure after queued teardown updates the normal stop status.
                runtime.worker.execute(() -> runtime.failed(error));
            }
        });
        return START_NOT_STICKY;
    }
    @Override public void updateVpnStatus(boolean active) { runtime.tunnelActive = active && !closed.get(); }
    @Override public void disconnectVPN() { close(); }
    @Override public void onRevoke() { close(); super.onRevoke(); }
    @Override public void onDestroy() { close(); super.onDestroy(); }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        runtime.serviceStatus = "stopping";
        runtime.cancelLogin();
        runtime.tunnelActive = false;
        runtime.worker.execute(() -> {
            if (attached) {
                // Always detach even if the preference update fails.
                try { runtime.wantRunning(false); }
                catch (Exception error) { runtime.failed(error); }
                finally { Libtailscale.serviceDisconnect(this); attached = false; }
            }
            if (runtime.callbacks != null && runtime.callbacks.networks.service == this)
                runtime.callbacks.networks.service = null;
            runtime.status = "VPN stopped";
            runtime.serviceStatus = "stopped";
        });
        main.post(() -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); });
    }
    @Override public libtailscale.VPNServiceBuilder newBuilder() {
        Builder builder = new Builder().setSession("Desknpocket")
                .allowFamily(OsConstants.AF_INET).allowFamily(OsConstants.AF_INET6);
        // Limit the spike's VPN routing to this package, including existing streaming JNI sockets.
        try { builder.addAllowedApplication(getPackageName()); }
        catch (android.content.pm.PackageManager.NameNotFoundException impossible) {
            throw new IllegalStateException("Own package missing", impossible);
        }
        Network underlying = runtime.callbacks.networks.selected;
        builder.setUnderlyingNetworks(underlying == null ? new Network[0] : new Network[]{underlying});
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(false);
        return new libtailscale.VPNServiceBuilder() {
            @Override public void setMTU(int mtu) { builder.setMtu(mtu); }
            @Override public void addDNSServer(String address) { builder.addDnsServer(address); }
            @Override public void addSearchDomain(String domain) { builder.addSearchDomain(domain); }
            @Override public void addRoute(String address, int prefix) { builder.addRoute(address, prefix); }
            @Override public void addAddress(String address, int prefix) { builder.addAddress(address, prefix); }
            @Override public void excludeRoute(String address, int prefix) throws Exception {
                if (Build.VERSION.SDK_INT >= 33) builder.excludeRoute(new IpPrefix(InetAddress.getByName(address), prefix));
            }
            @Override public libtailscale.ParcelFileDescriptor establish() throws Exception {
                if (closed.get()) throw new IllegalStateException("VPN service closed");
                android.os.ParcelFileDescriptor fd = builder.establish();
                if (fd == null) throw new IllegalStateException("VPN permission revoked");
                return fd::detachFd;
            }
        };
    }
}
