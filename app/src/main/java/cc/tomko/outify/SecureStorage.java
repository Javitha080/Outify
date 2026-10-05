package cc.tomko.outify;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple modular secure storage backed by Tink AEAD + SharedPreferences.
 * <p>
 * - Stores encrypted Base64 blobs under a user-chosen key.
 * - You can store raw bytes, strings, or arbitrary objects (via Gson).
 */
public final class SecureStorage {

    public enum Keys {
        ACCESS_TOKEN("access_token"),
        REFRESH_TOKEN("refresh_token"),
        ACCESS_TOKEN_EXPIRATION("access_token_expiration"),
        CLIENT_ID("client_id"),
        CLIENT_SECRET("client_secret");

        private final String internalName;

        Keys(String internalName) {
            this.internalName = internalName;
        }

        public String key() {
            return internalName;
        }
    }

    private static final String DEFAULT_PREFS = "tink_secure_storage";
    private static final String DEFAULT_KEYSET_PREF = "tink_keyset";
    private static final String DEFAULT_MASTER_KEY_URI = "android-keystore://tink_master_key";
    private static final String PREF_KEY_PREFIX = "secure:";

    private final SharedPreferences prefs;
    private final Aead aead;
    private final Gson gson;

    /**
     * Create storage with default Tink keyset location and master key URI.
     */
    public SecureStorage(Context context) throws GeneralSecurityException, IOException {
        this(context, DEFAULT_PREFS, DEFAULT_KEYSET_PREF, DEFAULT_MASTER_KEY_URI);
    }

    /**
     * Create storage with explicit configuration.
     *
     * @param prefsName    SharedPreferences name for storage.
     * @param keysetPref   SharedPreferences key where Tink keyset is stored.
     * @param masterKeyUri Master key in Android Keystore.
     */
    public SecureStorage(Context context, String prefsName, String keysetPref, String masterKeyUri)
            throws GeneralSecurityException, IOException {
        this.prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        KeysetHandle handle = new AndroidKeysetManager.Builder()
                .withSharedPref(context, prefsName, keysetPref)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri(masterKeyUri)
                .build()
                .getKeysetHandle();

        this.aead = handle.getPrimitive(Aead.class);
        this.gson = new Gson();
    }

    private String prefKey(Keys key) {
        return PREF_KEY_PREFIX + key.key();
    }

    private byte[] aadFor(Keys key) {
        return key.key().getBytes(StandardCharsets.UTF_8);
    }

    /* ---------- basic bytes/string operations ---------- */

    public synchronized void putBytes(Keys key, byte[] plaintext) throws GeneralSecurityException {
        byte[] ciphertext = aead.encrypt(plaintext, aadFor(key));
        String b64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP);
        prefs.edit().putString(prefKey(key), b64).apply();
    }

    public synchronized byte[] getBytes(Keys key) throws GeneralSecurityException {
        String b64 = prefs.getString(prefKey(key), null);
        if (b64 == null) return null;
        byte[] ciphertext = Base64.decode(b64, Base64.NO_WRAP);
        return aead.decrypt(ciphertext, aadFor(key));
    }

    public synchronized void putString(Keys key, String value) throws GeneralSecurityException {
        putBytes(key, value.getBytes(StandardCharsets.UTF_8));
    }

    public synchronized String getString(Keys key) throws GeneralSecurityException {
        byte[] bytes = getBytes(key);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /* ---------- object serialization (Gson) ---------- */

    public synchronized <T> void putObject(Keys key, T object) throws GeneralSecurityException {
        String json = gson.toJson(object);
        putString(key, json);
    }

    public synchronized <T> T getObject(Keys key, Class<T> clazz) throws GeneralSecurityException {
        String json = getString(key);
        if (json == null) return null;
        return gson.fromJson(json, clazz);
    }

    /* ---------- utility ---------- */

    public synchronized boolean contains(Keys key) {
        return prefs.contains(prefKey(key));
    }

    public synchronized void remove(Keys key) {
        prefs.edit().remove(prefKey(key)).apply();
    }

    public synchronized void clear() {
        // careful: only clear keys with our prefix
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> all = prefs.getAll();
        for (String k : all.keySet()) {
            if (k.startsWith(PREF_KEY_PREFIX)) editor.remove(k);
        }
        editor.apply();
    }

    public synchronized List<String> listKeys() {
        Map<String, ?> all = prefs.getAll();
        List<String> keys = new ArrayList<>();
        Set<String> names = all.keySet();
        for (String storedKey : names) {
            if (storedKey.startsWith(PREF_KEY_PREFIX)) {
                keys.add(storedKey.substring(PREF_KEY_PREFIX.length()));
            }
        }
        return keys;
    }
}

