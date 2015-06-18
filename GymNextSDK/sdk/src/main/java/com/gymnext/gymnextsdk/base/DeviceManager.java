package com.gymnext.gymnextsdk.base;

import com.gymnext.gymnextsdk.Device;

import java.util.List;

public interface DeviceManager
{
    public boolean isAvailable();

    public boolean isScanning();

    public boolean startScanning();

    public void stopScanning();

    public boolean hasDevice(String deviceId);

    public Device getDevice(String deviceId);

    public List<Device> getDevices(String serviceId);

    public void forgetDevice(String deviceId);
}
