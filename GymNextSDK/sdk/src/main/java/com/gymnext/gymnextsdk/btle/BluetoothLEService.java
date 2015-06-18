package com.gymnext.gymnextsdk.btle;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.gymnext.gymnextsdk.Service;

public interface BluetoothLEService extends Service
{
    public boolean isSendingInitialCommunication();

    public void didDiscoverServiceAndCharacteristics(BluetoothGattService service);

    public void didReceiveUpdateValueForCharacteristic(BluetoothGattCharacteristic characteristic);

    public void didReadValueForCharacteristic(BluetoothGattCharacteristic characteristic);

    public void didWriteValueForCharacteristic(BluetoothGattCharacteristic characteristic);

    public void didWriteValueForDescriptor(BluetoothGattDescriptor descriptor);

    public void didReadValueForDescriptor(BluetoothGattDescriptor descriptor);

}
