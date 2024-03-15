package com.pwc.madison.core.userreg.utils;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.EncryptedDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Utils Class which provide methods to encrypt the given String.
 */
final public class SecurityUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtils.class);

    private static final byte[] SALT = { (byte) 0x20, (byte) 0x21, (byte) 0xF0, (byte) 0x55, (byte) 0xC3, (byte) 0x9F,
            (byte) 0x5A, (byte) 0x75 };
    private final static int ITERATION_COUNT = 31;
    private static final String UNICODE_FORMAT = "UTF8";

    private static final String SECRET_KEY = "PBEWithMD5AndDES";

    /**
     * Returns {@link EncryptedDocumentException} value of the given input string. Returns null if given input is null
     * or encryption fails.
     * 
     * @param input
     *            {@link String}
     * @return {@link String}
     */
    public static String encode(String input) {
        if (null != input) {
            try {
                KeySpec keySpec = new PBEKeySpec(null, SALT, ITERATION_COUNT);
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);
                SecretKey key = SecretKeyFactory.getInstance(SECRET_KEY).generateSecret(keySpec);
                Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                byte[] enc = ecipher.doFinal(input.getBytes(UNICODE_FORMAT));
                String res = new String(Base64.encodeBase64(enc));
                res = res.replace('+', '-').replace('/', '_').replace("%", "%25").replace("\n", "%0A");
                return res;
            } catch (Exception exception) {
                LOGGER.error("SecutityUtils encode() : Exception occured while encoding : {}", exception);
            }
        }
        return null;

    }

    /**
     * Returns encrypted {@link List} for given String
     * 
     * @param {@link
     *            String} value
     * @return {@link String} of encrypted given value
     */
    public static String encrypt(final String value, final String fdKey) {
        String encryptedString = null;
        if (null != value) {
            try {
                IvParameterSpec iv = new IvParameterSpec(fdKey.getBytes("UTF-8"));
                SecretKeySpec skeySpec = new SecretKeySpec(fdKey.getBytes("UTF-8"), "AES");
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
                try {
                    encryptedString = java.util.Base64.getEncoder()
                            .encodeToString(cipher.doFinal(value.getBytes("UTF-8")));
                } catch (Exception exception) {
                    LOGGER.error("SecurityUtils encrypt() : Exception occured while encrypting value {} : {}",
                            exception, value);
                }
            } catch (Exception exception) {
                LOGGER.error("SecurityUtils encrypt() : Exception occured while setting algorithm : {}", exception);
            }
        }
        return encryptedString;
    }

}
