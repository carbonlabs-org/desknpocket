// Callback format adapted from Tailscale Android App.kt.
// Copyright (c) Tailscale Inc & AUTHORS. SPDX-License-Identifier: BSD-3-Clause
package com.limelight.network;

import android.content.Context;
import android.net.Network;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;

final class MeshAppContext implements libtailscale.AppContext {
    private final Context context;
    private final EncryptedState store;
    final PhysicalNetwork networks;
    MeshAppContext(Context context) {
        this.context = context;
        store = new EncryptedState(context);
        networks = new PhysicalNetwork(context);
    }
    @Override public void encryptToPref(String key, String value) throws Exception { store.put(key, value); }
    @Override public String decryptFromPref(String key) throws Exception { return store.get(key); }
    @Override public String getStateStoreKeysJSON() { return store.keys(); }
    @Override public String getOSVersion() { return Build.VERSION.RELEASE; }
    @Override public long getSDKInt() { return Build.VERSION.SDK_INT; }
    @Override public String getDeviceName() { return Build.MANUFACTURER + " " + Build.MODEL; }
    @Override public String getInstallSource() { return "desknpocket-local-debug"; }
    @Override public boolean shouldUseGoogleDNSFallback() { return false; }
    @Override public boolean isChromeOS() { return context.getPackageManager().hasSystemFeature("android.hardware.type.pc"); }
    @Override public boolean isClientLoggingEnabled() { return false; }
    // Do not expose backend logs, which can contain device/network identifiers or auth URLs.
    @Override public void log(String tag, String line) { }
    @Override public String getPlatformDNSConfig() { return networks.dns; }
    @Override public String getInterfacesAsJson() throws Exception {
        JSONArray result = new JSONArray();
        java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) return "[]";
        for (NetworkInterface item : Collections.list(interfaces)) {
            try {
                JSONArray addresses = new JSONArray();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (address.getAddress() != null) addresses.put(new JSONObject()
                            .put("ip", address.getAddress().getHostAddress()).put("prefixLen", address.getNetworkPrefixLength()));
                }
                result.put(new JSONObject().put("name", item.getName()).put("index", item.getIndex())
                        .put("mtu", item.getMTU()).put("up", item.isUp()).put("broadcast", item.supportsMulticast())
                        .put("loopback", item.isLoopback()).put("pointToPoint", item.isPointToPoint())
                        .put("multicast", item.supportsMulticast()).put("addrs", addresses));
            } catch (java.net.SocketException ignored) { /* interface disappeared during snapshot */ }
        }
        return result.toString();
    }
    @Override public boolean bindSocketToNetwork(int fd) {
        Network network = networks.selected;
        if (network == null) return false;
        try (ParcelFileDescriptor copy = ParcelFileDescriptor.fromFd(fd)) {
            network.bindSocket(copy.getFileDescriptor()); return true;
        } catch (Exception ignored) { return false; }
    }
    @Override public byte[] getUserCACertsPEM() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidCAStore"); ks.load(null);
        StringBuilder pem = new StringBuilder();
        for (String alias : Collections.list(ks.aliases())) {
            if (!alias.startsWith("user:") || ks.getCertificate(alias) == null) continue;
            pem.append("-----BEGIN CERTIFICATE-----\n")
                    .append(Base64.encodeToString(ks.getCertificate(alias).getEncoded(), Base64.NO_WRAP))
                    .append("\n-----END CERTIFICATE-----\n");
        }
        return pem.toString().getBytes(StandardCharsets.UTF_8);
    }
    // No managed policy is configured for this local spike; preserve upstream's absence semantics.
    @Override public boolean getSyspolicyBooleanValue(String key) throws Exception { throw new Exception("no such key"); }
    @Override public String getSyspolicyStringValue(String key) throws Exception { throw new Exception("no such key"); }
    @Override public String getSyspolicyStringArrayJSONValue(String key) throws Exception { throw new Exception("no such key"); }
    // Hardware attestation is explicitly disabled when starting this prototype backend.
    @Override public boolean hardwareAttestationKeySupported() { return false; }
    @Override public String hardwareAttestationKeyCreate() { throw new UnsupportedOperationException(); }
    @Override public void hardwareAttestationKeyLoad(String id) { throw new UnsupportedOperationException(); }
    @Override public byte[] hardwareAttestationKeyPublic(String id) { throw new UnsupportedOperationException(); }
    @Override public void hardwareAttestationKeyRelease(String id) { throw new UnsupportedOperationException(); }
    @Override public byte[] hardwareAttestationKeySign(String id, byte[] data) { throw new UnsupportedOperationException(); }
}
