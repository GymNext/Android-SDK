package com.gymnext.gymnextsdk.btle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.gymnext.gymnextsdk.CentralDeviceManager;
import com.gymnext.gymnextsdk.CommunicationMethod;
import com.gymnext.gymnextsdk.Device;
import com.gymnext.gymnextsdk.DeviceState;
import com.gymnext.gymnextsdk.Service;
import com.gymnext.gymnextsdk.timer.TimerService;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BluetoothLEDevice extends Device
{
    public static BluetoothLEDevice fromJson(JSONObject deviceJson)
    {
        return new BluetoothLEDevice(deviceJson);
    }

    private BluetoothGatt _btGatt;
    private BluetoothDevice _btDevice;
    private Map<String, BluetoothLEService> _services = new HashMap<String, BluetoothLEService>();
    private boolean _inRange = false;

    public BluetoothLEDevice(String deviceId, String deviceName, String deviceAlias, String manufacturerName, String modelName, BluetoothDevice btDevice)
    {
        super(deviceId, deviceName, deviceAlias, manufacturerName, modelName);
        _btDevice = btDevice;
    }

    protected BluetoothLEDevice(JSONObject deviceJson)
    {
        super(deviceJson);
    }

    @Override
    public CommunicationMethod getCommunicationMethod()
    {
        return CommunicationMethod.BluetoothLE;
    }

    public Service getService(String serviceId)
    {
        return _services.get(serviceId);
    }

    public void setBtDevice(BluetoothDevice btDevice) {
        _btDevice = btDevice;
    }

    public void connect(Context context)
    {
        if (_deviceState == DeviceState.Disconnected || _deviceState == DeviceState.OutOfRange)
        {
            Log.i("BluetoothLEDevice", "Connect");

            _deviceState = DeviceState.Connecting;

            _btGatt = _btDevice.connectGatt(context, false, new BluetoothGattCallback()
            {
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
                {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        boolean sendingInitialCommunicationBefore = _services.get(TimerService.SERVICE_ID).isSendingInitialCommunication();
                        _services.get(TimerService.SERVICE_ID).didReadValueForCharacteristic(characteristic);
                        boolean sendingInitialCommunicationAfter = _services.get(TimerService.SERVICE_ID).isSendingInitialCommunication();

                        if (sendingInitialCommunicationBefore && !sendingInitialCommunicationAfter && _delegate != null) {
                            _delegate.establishedCommunicationChannel(BluetoothLEDevice.this);
                        }

                        if (_delegate != null)
                        {
                            _delegate.receivedDataFromDevice(BluetoothLEDevice.this);
                        }
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
                {
                    _services.get(TimerService.SERVICE_ID).didWriteValueForCharacteristic(characteristic);

                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {

                        if (_delegate != null)
                        {
                            _delegate.sentDataToDevice(BluetoothLEDevice.this);
                        }
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    _services.get(TimerService.SERVICE_ID).didWriteValueForDescriptor(descriptor);

                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        if (_delegate != null)
                        {
                            _delegate.sentDataToDevice(BluetoothLEDevice.this);
                        }
                    }
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    _services.get(TimerService.SERVICE_ID).didReadValueForDescriptor(descriptor);

                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        if (_delegate != null)
                        {
                            _delegate.receivedDataFromDevice(BluetoothLEDevice.this);
                        }
                    }
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
                {
                    Log.i("BluetoothLEDevice", "onConnectionStateChange Status: "  + status);
                    Log.i("BluetoothLEDevice", "onConnectionStateChange New State: "  + newState);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            BluetoothLEDeviceManager.getInstance().deviceDidConnect(BluetoothLEDevice.this);

                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            BluetoothLEDeviceManager.getInstance().deviceDidDisconnect(BluetoothLEDevice.this);
                        }
                    }
                    else {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            BluetoothLEDeviceManager.getInstance().deviceDidFailToConnect(BluetoothLEDevice.this);

                        }
                    }

                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status)
                {
                    Log.i("BluetoothLEDevice", "onServicesDiscovered: "  + gatt);
                    final BluetoothGattService service = gatt.getService(BluetoothLETimerService.UART_UUID);
                    if (service != null)
                    {
                        _services.get(TimerService.SERVICE_ID).didDiscoverServiceAndCharacteristics(service);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
                {
                    boolean sendingInitialCommunicationBefore = _services.get(TimerService.SERVICE_ID).isSendingInitialCommunication();
                    _services.get(TimerService.SERVICE_ID).didReceiveUpdateValueForCharacteristic(characteristic);
                    boolean sendingInitialCommunicationAfter = _services.get(TimerService.SERVICE_ID).isSendingInitialCommunication();

                    if (sendingInitialCommunicationBefore && !sendingInitialCommunicationAfter && _delegate != null) {
                        _delegate.establishedCommunicationChannel(BluetoothLEDevice.this);
                    }

                    if (_delegate != null)
                    {
                        _delegate.receivedDataFromDevice(BluetoothLEDevice.this);
                    }
                }

            });

            // Assumes one service
            final String secureCode = CentralDeviceManager.getInstance().getDeviceSecureCode(this);
            _services.put(TimerService.SERVICE_ID, new BluetoothLETimerService(secureCode, _btGatt));

            BluetoothLEDeviceManager.getInstance().connect(this);

        }
    }

    public void didConnect()
    {
        // println("Did Connect")

        if (_deviceState == DeviceState.Connecting)
        {
            _deviceState = DeviceState.Connected;
            _inRange = true;

            _btGatt.discoverServices();
        }
    }

    public void disconnect()
    {
        // println("Disconnect")

        if (_deviceState == DeviceState.Connecting || _deviceState == DeviceState.Connected)
        {
            BluetoothLEDeviceManager.getInstance().disconnect(this);

            if (_btGatt != null) {
                _btGatt.disconnect();
                _btGatt.close();
                _btGatt = null;
            }
            BluetoothLEDeviceManager.getInstance().deviceDidDisconnect(BluetoothLEDevice.this);
        }
    }

    public void didDisconnect()
    {
        // println("Did Disconnect")
        if (_inRange)
        {
            _deviceState = DeviceState.Disconnected;
        }
        else
        {
            _deviceState = DeviceState.OutOfRange;
        }
    }

    public void didFailToConnect()
    {
        if (_btGatt != null) {
            _btGatt.disconnect();
            _btGatt.close();
            _btGatt = null;
        }

        // println("Did Fail To Connect")
        if (_inRange)
        {
            _deviceState = DeviceState.Disconnected;
        }
        else
        {
            _deviceState = DeviceState.OutOfRange;
        }
    }

    public void didMoveIntoRange()
    {
        // println("Did Move Into Range")
        if (_deviceState == DeviceState.OutOfRange)
        {
            _deviceState = DeviceState.Disconnected;
        }
        _inRange = true;
    }

    public void didMoveOutOfRange()
    {
        // println("Did Move Out Of Range")
        _deviceState = DeviceState.OutOfRange;
        _inRange = false;
    }

}
