package com.gymnext.gymnextsdk.btle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.gymnext.gymnextsdk.Device;
import com.gymnext.gymnextsdk.DeviceState;
import com.gymnext.gymnextsdk.timer.TimerService;
import com.gymnext.gymnextsdk.base.DeviceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BluetoothLEDeviceManager implements DeviceManager {
    public interface BluetoothLEDeviceManagerDelegate {
        public void deviceDidMoveInRange(BluetoothLEDeviceManager deviceManager, BluetoothLEDevice device);

        public void deviceDidMoveOutOfRange(BluetoothLEDeviceManager deviceManager, BluetoothLEDevice device);

        public void deviceDidConnect(BluetoothLEDeviceManager deviceManager, BluetoothLEDevice device);

        public void deviceDidFailToConnect(BluetoothLEDeviceManager deviceManager, BluetoothLEDevice device);

        public void deviceDidDisconnect(BluetoothLEDeviceManager deviceManager, BluetoothLEDevice device);
    }

    /**
     * singleton instance
     */
    private static BluetoothLEDeviceManager mInstance = new BluetoothLEDeviceManager();

    private static final int CONNECTION_TIMEOUT = 5;

    /**
     * singleton accessor
     */
    public static BluetoothLEDeviceManager getInstance() {
        return mInstance;
    }

    private Context _context;

    private BluetoothLEDeviceManagerDelegate _delegate;

    private boolean _scanning = false;
    private Map<String, BluetoothLEDevice> _devices = new HashMap<String, BluetoothLEDevice>();

    private BluetoothAdapter _adapter;

    private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
    private Map<String, ScheduledFuture<?>> _connectionTimeoutTimers = new HashMap<String, ScheduledFuture<?>>();


    /**
     * hidden constructor
     */
    private BluetoothLEDeviceManager() {
    }

    public void initialize(Context context) {
        if (_context != null) {
            return;
        }

        _context = context;

        BluetoothManager bluetoothManager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
        _adapter = bluetoothManager.getAdapter();

        _loadDevices();
    }

    public void setDelegate(BluetoothLEDeviceManagerDelegate delegate) {
        _delegate = delegate;
    }

    public BluetoothLEDeviceManagerDelegate getDelegate() {
        return _delegate;
    }

    public boolean isAvailable() {
        return _adapter.isEnabled();
    }

    public boolean isScanning() {
        return _scanning;
    }

    public boolean startScanning() {
        if (!isAvailable()) {
            return false;
        }

        Log.i("BluetoothLEDeviceManager", "Start Scanning");
        _scanning = true;
        _adapter.startLeScan(mScanCallback);

        return true;
    }

    public void stopScanning() {
        if (!isAvailable()) {
            return;
        }

        Log.i("BluetoothLEDeviceManager", "Stop Scanning");
        _adapter.stopLeScan(mScanCallback);
        _scanning = false;
    }

    private LeScanCallback mScanCallback = new LeScanCallback() {



        @Override
        public void onLeScan(BluetoothDevice btDevice, int rssi, byte[] scanRecord) {

            Log.i("Duane", "Device Discovered: " + btDevice.getAddress());
            Log.i("Duane", "Device Discovered: " + btDevice.getName());
            if (btDevice.getName() == null) {
                return;
            }

            Log.i("Duane", "Device Discovered: " + btDevice.getName());

            String deviceId = btDevice.getAddress();
            String deviceName = btDevice.getName();

            // TODO: Advertisement Data
            Log.i("Duane", "Service ids " + btDevice.getUuids());
//            printScanRecord(scanRecord);

            boolean acceptable = false;
            List<UUID> serviceUuids = _parseUUIDs(scanRecord);
            Log.i("Duane", "Service ids " + serviceUuids);
            if (serviceUuids.contains(BluetoothLETimerService.UART_UUID)) {
                Log.i("BluetoothLEDeviceManager", "Acceptable Device");
                acceptable = true;
            } else {
                Log.i("BluetoothLEDeviceManager", "Not Acceptable Device: " + serviceUuids);
            }

            if (acceptable) {
                String modelName = "Unknown";
                String manufacturerName = "GymNext";
                Set<String> serviceIds = new HashSet<String>();
                serviceIds.add(TimerService.SERVICE_ID);

                if (!hasDevice(deviceId)) {
                    Log.i("BluetoothLEDeviceManager", "New device " + deviceId);

                    final BluetoothLEDevice device = new BluetoothLEDevice(deviceId, deviceName, null, manufacturerName, modelName, btDevice);
                    device.setServiceIds(serviceIds);

                    device.didMoveIntoRange();
                    _devices.put(device.getDeviceId(), device);
                    _saveDevices();

                    if (_delegate != null) {
                        _delegate.deviceDidMoveInRange(BluetoothLEDeviceManager.this, device);
                    }
                } else {
                    Log.i("BluetoothLEDeviceManager", "Existing device " + deviceId);

                    final BluetoothLEDevice device = _devices.get(deviceId);
                    device.setBtDevice(btDevice);

                    if (!device.getDeviceName().equals(deviceName)) {
                        device.setDeviceName(deviceName);
                        _saveDevices();

                        // Trigger an update
                        if (_delegate != null) {
                            _delegate.deviceDidMoveInRange(BluetoothLEDeviceManager.this, device);
                        }
                    }

                    if (device.getDeviceState() == DeviceState.OutOfRange) {
                        device.didMoveIntoRange();
                        if (_delegate != null) {
                            _delegate.deviceDidMoveInRange(BluetoothLEDeviceManager.this, device);
                        }
                    }
                }
            }

        }
    };

    public boolean hasDevice(String deviceId) {
        return _devices.containsKey(deviceId);
    }

    public void forgetDevice(String deviceId) {
        _devices.remove(deviceId);
        _saveDevices();
    }

    public Device getDevice(String deviceId) {
        return _devices.get(deviceId);
    }

    public List<Device> getDevices(String serviceId) {
        List<Device> result = new ArrayList<Device>();
        for (Device device : _devices.values()) {
            if (serviceId == null || device.hasService(serviceId)) {
                result.add(device);
            }
        }
        return result;
    }

    public void connect(final BluetoothLEDevice device) {
        // Does nothing

        ScheduledFuture<?> future = _executor.schedule(new Runnable() {
            @Override
            public void run() {

                Log.i("BluetoothLEDeviceManager", "Timeout occurred");

                device.didFailToConnect();

                if (_delegate != null) {
                    _delegate.deviceDidFailToConnect(BluetoothLEDeviceManager.this, device);
                }
            }
        }, CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        _connectionTimeoutTimers.put(device.getDeviceId(), future);
    }

    public void disconnect(BluetoothLEDevice device) {
        // Does nothing
    }

    private void _loadDevices() {
        SharedPreferences settings = _context.getSharedPreferences("gymnext_devices.pref", Context.MODE_PRIVATE);
        boolean hasDevices = settings.getString("bluetoothLEDevicesVersion", null) != null;

        if (hasDevices) {
            String s = settings.getString("devices", null);
            if (s != null) {
                try {
                    JSONArray jsonArray = new JSONArray(s);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        BluetoothLEDevice device = BluetoothLEDevice.fromJson(jsonArray.getJSONObject(i));
                        BluetoothDevice btDevice = _adapter.getRemoteDevice(device.getDeviceId());
                        if (btDevice != null) {
                            device.setBtDevice(btDevice);
                            _devices.put(device.getDeviceId(), device);
                        }
                    }
                } catch (JSONException e) {
                    // Swallow
                }
            }
        }

        Log.i("BluetoothLEDeviceManager", "Loaded devices: " + _devices.size());

    }

    private void _saveDevices() {
        SharedPreferences settings = _context.getSharedPreferences("gymnext_devices.pref", Context.MODE_PRIVATE);
        final Editor editor = settings.edit();
        editor.putString("bluetoothLEDevicesVersion", "1.0");

        final JSONArray jsonArray = new JSONArray();
        for (Device device : _devices.values()) {
            jsonArray.put(device.toJson());
        }

        editor.putString("devices", jsonArray.toString());
        editor.commit();

        Log.i("BluetoothLEDeviceManager", "Saved devices: " + _devices.size());
    }

    public void deviceDidConnect(BluetoothLEDevice device) {
        if (!hasDevice(device.getDeviceId())) {
            return;
        }

        if (device.isConnected()) {
            return;
        }

        device.didConnect();

        // Kill connection timeout trackers
        ScheduledFuture<?> future = _connectionTimeoutTimers.get(device.getDeviceId());
        if (future != null) {
            future.cancel(false);
        }

        if (_delegate != null) {
            _delegate.deviceDidConnect(this, device);
        }
    }

    public void deviceDidDisconnect(BluetoothLEDevice device) {
        if (!hasDevice(device.getDeviceId())) {
            return;
        }

        if (!device.isConnected()) {
            return;
        }

        device.didDisconnect();

        // Kill connection timeout trackers
        ScheduledFuture<?> future = _connectionTimeoutTimers.get(device.getDeviceId());
        if (future != null) {
            future.cancel(false);
        }

        if (_delegate != null) {
            _delegate.deviceDidDisconnect(this, device);
        }

    }

    public void deviceDidFailToConnect(BluetoothLEDevice device) {
        if (!hasDevice(device.getDeviceId())) {
            return;
        }

        device.didFailToConnect();

        // Kill connection timeout trackers
        ScheduledFuture<?> future = _connectionTimeoutTimers.get(device.getDeviceId());
        if (future != null) {
            future.cancel(false);
        }

        if (_delegate != null) {
            _delegate.deviceDidFailToConnect(this, device);
        }
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    // http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> _parseUUIDs(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        Log.i("Duane", "Advertised Data " + bytesToHex(advertisedData));

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        Log.i("BluetoothLEDeviceManager", "Advertised Data " + buffer.remaining());
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    Log.i("BluetoothLEDeviceManager", "02 or 03");
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    Log.i("BluetoothLEDeviceManager", "06 or 07");
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        Log.i("BluetoothLEDeviceManager", lsb + "");
                        Log.i("BluetoothLEDeviceManager", msb + "");
                        Log.i("BluetoothLEDeviceManager", new UUID(msb, lsb).toString());

                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    Log.i("BluetoothLEDeviceManager", "No Match " + type + " - "  + length);
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }

    private void printScanRecord (byte[] scanRecord) {

        // Simply print all raw bytes
        try {
            String decodedRecord = new String(scanRecord,"UTF-8");
            Log.d("DEBUG","decoded String : " + ByteArrayToString(scanRecord));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Parse data bytes into individual records
        List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);


        // Print individual records
        if (records.size() == 0) {
            Log.i("DEBUG", "Scan Record Empty");
        } else {
            Log.i("DEBUG", "Scan Record: " + TextUtils.join(",", records));
        }

    }


    public static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }


    protected static class AdRecord {

        protected AdRecord(int length, int type, byte[] data) {
            String decodedRecord = "";
            try {
                decodedRecord = new String(data,"UTF-8");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Log.d("DEBUG", "Length: " + length + " Type : " + type + " Data : " + ByteArrayToString(data));
        }

        // ...

        protected static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }

            return records;
        }

        // ...
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
