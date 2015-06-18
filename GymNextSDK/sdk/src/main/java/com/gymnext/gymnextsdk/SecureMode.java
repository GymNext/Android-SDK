package com.gymnext.gymnextsdk;

/**
 * Controls how much security is enabled on the device
 */
public enum SecureMode {
    /**
     * No security and anyone can connect
     */
    None,
    /**
     * All administrative functions require a secure code
     */
    Admin,
    /**
     * All functions require a secure code
     */
    All
}
