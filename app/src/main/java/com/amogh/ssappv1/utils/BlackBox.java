package com.amogh.ssappv1.utils;

import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.amogh.ssappv1.SSApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class BlackBox {

    private static final String TAG = BlackBox.class.getSimpleName();

    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final String TYPE_RSA = "RSA";

    private final String alias;


    public BlackBox(String alias) {
        this.alias = alias;
    }

    /**
     * Creates a public and private key and stores it using the AndroidKeyStore,
     * so that only this application will be able to access the keys.
     */
//    @SuppressWarnings("deprecation")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void createKeys() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (keyStore.containsAlias(alias)) {
            Log.d(TAG, "[containsAlias]");
            return;
        }

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(SSApp.getContext())
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        KeyPairGenerator generator = KeyPairGenerator.getInstance(TYPE_RSA, ANDROID_KEY_STORE);
        generator.initialize(spec);
        KeyPair keyPair = generator.generateKeyPair();
        Log.d(TAG, "Public Key is: " + keyPair.getPublic().toString());
    }


    /**
     * Encrypt the secret with RSA.
     *
     * @param secret the secret.
     * @return the encrypted secret.
     * @throws Exception
     */
    public String encrypt(String secret) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry =
                (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);

        Cipher inputCipher = Cipher.getInstance(RSA_ALGORITHM);
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret.getBytes());
        cipherOutputStream.close();

        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }


    /**
     * Decrypt the encrypted secret.
     *
     * @param encrypted the encrypted secret.
     * @return the decrypted secret.
     * @throws Exception
     */
    public String decrypt(String encrypted) throws Exception {
        byte[] encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT);
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry =
                (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
        Cipher output = Cipher.getInstance(RSA_ALGORITHM);
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encryptedBytes), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i);
        }
        return new String(bytes);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void test() {
        try {
            String alias = "amogh";
            String plainTxt = "Ojas$1234";
            BlackBox bb = new BlackBox(alias);
            bb.createKeys();

            String encrTxt = bb.encrypt(plainTxt);
            System.out.println("Encrypted text :---------> " + encrTxt);
            System.out.println("decrypted text :---------> " + bb.decrypt(encrTxt));

            String storedPass = "fsi5wOtg4K1Qa7VkYtH4Wa1g+UpWza8NMHSmYi6+bgcp3+kGGhclbNhJMLanh/Gpy5ueqEHzqjbbgCFlWvhIbKBaUhu00CLOz6VbO7pZSuEWiCttJ+ybpodOrUhWovdI4frFaVQKPnp62AT3/bDb9fy00e12hZrxm87hylPkKIaAdwi/lT871jRjZpp90yXwT9kNMuwvAznFmjLlBXzcNnw0EKL1lGyc9Y46AAEgVQmO1jb6dgCi4uQhaOO0qk3qp4vUwmYhWFiD5vKu7c7FVqEzki4xMoZ+oI/RhYvUSqg/9vRLZVnCgOPUPCR3az+uTSOI8YPIWzOuUIDSVI4+/Q==";
            System.out.println("decrypted stored text :---------> " + bb.decrypt(storedPass));
            storedPass = "BcgI/5wjGMXUGnQ0DNylpRAWhf7rYI5lxeiH5+IQ9G74z+MOw7oQil2OEKz6qtAcaaRNb8WkKvxbYBOOlQnSrrlTrUn/kfNOP9tbf3C/1M/TLxGm4ZY3S3Zv8H0vQNlHU4mpm6YudB6lwVkU/yy4aIxJuBbgtSQyaQhO3Hlls37pCUjn9aJD3XxzuDZD2dGXUp+rDA/srmCOicyjF63JEWCWPaDnV9p9AA5y0OLLIwkHEi+BG7vL9P0/jFyubZ/wqinkIxyoLvcNrNzZyKWG87bOJGgm5JZtjQAxB88Ob721fkdWdLFYPVnxVdDxNxF7V8HHmjDD60XrFykaWr7BNg==";
            System.out.println("decrypted stored text :---------> " + bb.decrypt(storedPass));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
