package com.gymnext.gymnextsdk;

/**
 * The various states of a device connection
 */
public enum DeviceState
{
    /**
     * can't communicate with device at all or device is in use
     */
    OutOfRange,
    /**
     * device is in range, but not connected
     */
    Disconnected,
    /**
     * we are establishing a connection
     */
    Connecting,
    /**
     * we are actively connected
     */
    Connected
}
