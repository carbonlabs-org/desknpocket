// Adapted from Tailscale Android NetworkChangeCallback.kt.
// Copyright (c) Tailscale Inc & AUTHORS. SPDX-License-Identifier: BSD-3-Clause
package com.limelight.network;

import android.content.Context;
import android.net.*;
import java.util.LinkedHashMap;
import java.util.Map;
import libtailscale.Libtailscale;

final class PhysicalNetwork extends ConnectivityManager.NetworkCallback {
    private final ConnectivityManager manager;
    private final Map<Network, NetworkCapabilities> capabilities = new LinkedHashMap<>();
    private final Map<Network, LinkProperties> links = new LinkedHashMap<>();
    volatile Network selected;
    volatile String dns = "";
    volatile MeshVpnService service;
    private boolean backendReady;
    PhysicalNetwork(Context context) {
        manager = context.getSystemService(ConnectivityManager.class);
        manager.registerNetworkCallback(new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(), this);
    }
    static int rank(boolean internet, boolean notVpn, boolean validated, boolean hasDns, boolean unmetered) {
        if (!internet || !notVpn) return -1;
        return (validated ? 4 : 0) + (hasDns ? 2 : 0) + (unmetered ? 1 : 0);
    }
    @Override public synchronized void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
        capabilities.put(network, caps); recompute();
    }
    @Override public synchronized void onLinkPropertiesChanged(Network network, LinkProperties props) {
        links.put(network, props); recompute();
    }
    @Override public synchronized void onLost(Network network) {
        capabilities.remove(network); links.remove(network); recompute();
    }
    synchronized void ready() { backendReady = true; recompute(); }
    private void recompute() {
        Network best = null;
        int bestRank = -1;
        for (Map.Entry<Network, NetworkCapabilities> entry : capabilities.entrySet()) {
            NetworkCapabilities c = entry.getValue();
            LinkProperties lp = links.get(entry.getKey());
            int score = rank(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN),
                    c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    lp != null && !lp.getDnsServers().isEmpty(),
                    c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED));
            if (score > bestRank) { best = entry.getKey(); bestRank = score; }
        }
        selected = best;
        LinkProperties lp = links.get(best);
        StringBuilder config = new StringBuilder();
        String gateway = "";
        if (lp != null) {
            for (java.net.InetAddress server : lp.getDnsServers()) config.append(server.getHostAddress()).append(' ');
            if (lp.getDomains() != null) config.append('\n').append(lp.getDomains());
            for (RouteInfo route : lp.getRoutes()) {
                if (route.isDefaultRoute() && route.getGateway() != null) {
                    gateway = route.getGateway().getHostAddress(); break;
                }
            }
        }
        dns = config.toString();
        MeshVpnService current = service;
        if (current != null) current.setUnderlyingNetworks(best == null ? new Network[0] : new Network[]{best});
        if (backendReady) {
            Libtailscale.onGatewayChanged(gateway);
            Libtailscale.onDNSConfigChanged(lp == null || lp.getInterfaceName() == null ? "" : lp.getInterfaceName());
        }
    }
}
