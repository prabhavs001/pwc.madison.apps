package com.pwc.madison.core.util;

import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HashGeneratorUtils.
 */
public class HashGeneratorUtils {
    
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HashGeneratorUtils.class);
    
    /**
     * The Enum TypeOfHash.
     */
    public enum TypeOfHash {
        /** The md5. */
        MD5,
        /** The sha1. */
        SHA1,
        /** The sha256. */
        SHA256
    };
    
    /**
     * Instantiates a new hash generator utils.
     */
    private HashGeneratorUtils() {
    }
    
    
    /**
     * Generate checkum based on hash algorithim.
     *
     * @param message the value to be hashed
     * @param typeOfHash the type of hash algorthim
     * @return the hashed value
     */
    public static String generateCheckum(String message,TypeOfHash typeOfHash) {
        String hashedValue = message;
        if(Objects.nonNull(typeOfHash) && StringUtils.isNotEmpty(message)) {
            hashedValue = getStringChecksum(message,typeOfHash);
        }
        return hashedValue;
    }


    /**
     * Gets the string checksum.
     *
     * @param value the value
     * @param algorithim the algorithim
     * @return the string checksum
     */
    private static String getStringChecksum(final String value, TypeOfHash algorithim) {
        String hashedValue = StringUtils.EMPTY;
        LOGGER.debug("Fetching checksum for {} with alorithim {}",value,algorithim);
        switch (algorithim) {
            case MD5:
                hashedValue = DigestUtils.md5Hex(value);
                break;

            case SHA1:
                hashedValue = DigestUtils.sha1Hex(value);
                break;

            case SHA256:
                hashedValue = DigestUtils.sha256Hex(value);
                break;

            default:
                break;
        }
        LOGGER.debug("Hashed value for {} is {}",value,hashedValue);
        return hashedValue;
    }
}
