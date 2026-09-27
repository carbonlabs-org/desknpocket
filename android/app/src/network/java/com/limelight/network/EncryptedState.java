package com.limelight.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Separate, non-backed-up network state; authentication failures never become empty state. */
final class EncryptedState {
    private final SharedPreferences prefs;
    private final String alias;
    EncryptedState(Context context) {
        prefs = context.getSharedPreferences("mesh-encrypted-state", Context.MODE_PRIVATE);
        alias = context.getPackageName() + ".mesh-state";
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(alias)) {
            if (!prefs.getAll().isEmpty()) throw new IllegalStateException("State key is unavailable");
            KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
            return gen.generateKey();
        }
        return (SecretKey) store.getKey(alias, null);
    }
    synchronized void put(String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        if (!prefs.edit().putString(name, encoded).commit()) throw new java.io.IOException("State write failed");
    }
    synchronized String get(String name) throws Exception {
        String value = prefs.getString(name, null);
        if (value == null) return "";
        String[] parts = value.split(":", -1);
        if (parts.length != 2) throw new java.io.IOException("Invalid encrypted state");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    synchronized String keys() {
        JSONArray result = new JSONArray();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("statestore-")) result.put(key.substring(11));
        }
        return result.toString();
    }
}
