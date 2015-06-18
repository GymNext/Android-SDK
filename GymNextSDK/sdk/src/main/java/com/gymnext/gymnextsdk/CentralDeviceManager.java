package com.gymnext.gymnextsdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.gymnext.gymnextsdk.btle.BluetoothLEDevice;
import com.gymnext.gymnextsdk.btle.BluetoothLEDeviceManager;
import com.gymnext.gymnextsdk.btle.BluetoothLEDeviceManager.BluetoothLEDeviceManagerDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the main access point for all device management.  This is where you will discover new devices,
 * activate and deactivate devices, and reconnect to devices.  You can also give devices aliases,
 * store secure codes so that users don't need to be prompted every time, and can receive updates
 * on device state by registering yourself as the delegate.
 *
 * To talk to a device, you must first activate it.  Once connected, you can use the device object
 * to retrieve a specific service it supports and call methods on that service.
 */
public class CentralDeviceManager implements BluetoothLEDeviceManagerDelegate
{
    /**
     * Main listener for all events related to device management
     */
    public interface CentralDeviceManagerDelegate
    {
        // Device Discovery

        /**
         * The device moved into range
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidMoveInRange(CentralDeviceManager centralDeviceManager, Device device);

        /**
         * The device moved out of range
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidMoveOutOfRange(CentralDeviceManager centralDeviceManager, Device device);

        // Device Activation

        /**
         * The device was activated
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidActivate(CentralDeviceManager centralDeviceManager, Device device);

        /**
         * The device was deactivated
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidDeactivate(CentralDeviceManager centralDeviceManager, Device device);

        // Device Connection

        /**
         * The device did connect
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidConnect(CentralDeviceManager centralDeviceManager, Device device);

        /**
         * The device did fail to connect
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidFailToConnect(CentralDeviceManager centralDeviceManager, Device device);

        /**
         * The device did disconnect
         * @param centralDeviceManager
         * the device manager
         * @param device
         * the device
         */
        public void deviceDidDisconnect(CentralDeviceManager centralDeviceManager, Device device);

        /**
         * The device manager finished reconnecting
         * @param centralDeviceManager
         * the device manager
         */
        public void deviceManagerDidFinishReconnectingDevices(CentralDeviceManager centralDeviceManager);

        /**
         * The device manager finished disconnecting from all devices
         * @param centralDeviceManager
         * The device manager
         */
        public void deviceManagerDidFinishDisconnectingDevices(CentralDeviceManager centralDeviceManager);
    }

    /**
     * singleton instance
     */
    private static CentralDeviceManager mInstance = new CentralDeviceManager();

    /**
     * singleton accessor
     */
    public static CentralDeviceManager getInstance()
    {
        return mInstance;
    }

    // Context
    private Context _context;

    // Reconnection State
    private boolean _reconnected = false;
    private Map<String, Device> _reconnectingDevices = new HashMap<String, Device>();
    private Map<String, Device> _disconnectingDevices = new HashMap<String, Device>();

    // Scanning State
    private boolean _scanning = false;

    private CentralDeviceManagerDelegate _delegate;
    private Map<CommunicationMethod, Boolean> _communicationMethods = new HashMap<CommunicationMethod, Boolean>();

    private Set<String> _activeDeviceIds = new HashSet<String>();
    private Set<String> _transientInactiveDeviceIds = new HashSet<String>();
    private Map<String, String> _deviceAliases = new HashMap<String, String>();
    private Map<String, String> _deviceSecureCodes = new HashMap<String, String>();

    /**
     * hidden constructor
     */
    private CentralDeviceManager()
    {
        _communicationMethods.put(CommunicationMethod.Bluetooth, false); // not fully implemented
        _communicationMethods.put(CommunicationMethod.Wifi, false); // not fully implemented
        _communicationMethods.put(CommunicationMethod.BluetoothLE, true);

    }

    public void initialize(Context context) {
        if (_context != null) {
            return;
        }
        _context = context;
        _loadSettings();

        BluetoothLEDeviceManager.getInstance().setDelegate(this);
        BluetoothLEDeviceManager.getInstance().initialize(context);
    }

    /**
     * Set the delegate for the manager
     * @param delegate
     * The delegate listener
     */
    public void setDelegate(CentralDeviceManagerDelegate delegate)
    {
        _delegate = delegate;
    }

    /**
     * Retrieve the delegate for the manager
     * @return
     * The delegate
     */
    public CentralDeviceManagerDelegate getDelegate()
    {
        return _delegate;
    }

    // COMMUNICATION METHODS

    /**
     * Check if a specific communication method is enabled
     * @param communicationMethod
     * The communication method to check
     * @return
     * If the communication method is enabled
     */
    public boolean isEnabled(CommunicationMethod communicationMethod)
    {
        return _communicationMethods.get(communicationMethod);
    }

    /**
     * Enable a communication method
     * @param communicationMethod
     * The communication method to enable
     */
    public void enable(CommunicationMethod communicationMethod)
    {
        _communicationMethods.put(communicationMethod, true);
    }

    /**
     * Disable a communication method
     *
     * @param communicationMethod
     * The communication method to disable
     *
     */
    public void disable(CommunicationMethod communicationMethod)
    {
        _communicationMethods.put(communicationMethod, false);
    }

    /**
     * Check if a communication method is even supported by the user's device (aka. phone/tablet)
     * @param communicationMethod
     * The communication method to check
     * @return
     * If the communication method is available and enabled
     */
    public boolean isAvailable(CommunicationMethod communicationMethod)
    {
        if (communicationMethod == CommunicationMethod.Bluetooth)
        {
            return false;
        }
        else if (communicationMethod == CommunicationMethod.Wifi)
        {
            return false;
        }
        else if (communicationMethod == CommunicationMethod.BluetoothLE)
        {
            return BluetoothLEDeviceManager.getInstance().isAvailable();
        }
        else
        {
            return false;
        }
    }

    // RECONNECT DEVICES

    /**
     * Reconnect to all active devices only if not already trying to reconnect.
     *
     * @param forceActive
     * Forces a retry of all active devices even if a previous reconnect attempt failed
     * @return
     * If a reconnect attempt can be made.
     */
    public boolean reconnectAll(boolean forceActive)
    {

        if (forceActive)
        {
            _transientInactiveDeviceIds.clear();
        }

        if (_reconnected)
        {
            // println("skipping reconnection due to already reconnected")
            return false;
        }

        if (_activeDeviceIds.size() == 0)
        {
            // println("skipping reconnect - there are no active devices")
            _reconnected = true;
            return false;
        }

        // println("reconnecting to active devices")

        _reconnected = true;
        _reconnectingDevices.clear();

        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            for (Device device : BluetoothLEDeviceManager.getInstance().getDevices(null))
            {
                if (isActive(device))
                {
                    _reconnectingDevices.put(device.getDeviceId(), device);
                }
            }
        }

        // MARKER: Add additional communication methods here

        if (_reconnectingDevices.size() == 0)
        {
            // println("skipping reconnect - there are no actual active devices")
            return false;
        }

        for (Device device : _reconnectingDevices.values())
        {
            _connect(device);
        }

        return true;
    }

    /**
     * Disconnect from all devices
     */
    public void disconnectAll()
    {

        _disconnectingDevices.clear();

        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            for (Device device : BluetoothLEDeviceManager.getInstance().getDevices(null))
            {
                if (isActive(device))
                {
                    _disconnectingDevices.put(device.getDeviceId(), device);
                }
            }
        }

        // MARKER: Add additional communication methods here

        if (_disconnectingDevices.size() == 0)
        {
            if (_delegate != null)
            {
                _delegate.deviceManagerDidFinishDisconnectingDevices(this);
            }
        }

        for (Device device : _disconnectingDevices.values())
        {
            _disconnect(device);
        }

        _reconnected = false;
    }

    // DISCOVERY FOR NEW DEVICES

    /**
     * Start scanning for devices to be discovered.
     */
    public void startScanning()
    {

        // mark reconnection as done if we start scanning
        _reconnected = true;

        if (_scanning)
        {
            Log.i("CentralDeviceManager", "skipping scanning due to already scanning");
            return;
        }

        _scanning = true;
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            BluetoothLEDeviceManager.getInstance().startScanning();
        }

        // MARKER: Add communication methods here
    }

    /**
     * Stop scanning for devices
     */
    public void stopScanning()
    {

        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            BluetoothLEDeviceManager.getInstance().stopScanning();
        }

        // MARKER: Add communication methods here
        _scanning = false;
    }

    /**
     * Check if we are actively scanning
     * @return
     * If we are scanning
     */
    public boolean isScanning()
    {
        return _scanning;
    }

    // RENAME/REMOVE DEVICES

    /**
     * Set an alias on a device.  An alias allows a user to locally override the name of a device without actually changing the
     * name of the device
     * @param device
     * The device to alias
     * @param deviceAlias
     * The alias to use (null to remove aliases)
     */
    public void setDeviceAlias(Device device, String deviceAlias)
    {
        if (deviceAlias == null) {
            device.setDeviceAlias(null);
            _deviceAliases.remove(device.getDeviceId());
        }
        else {
            device.setDeviceAlias(deviceAlias);
            _deviceAliases.put(device.getDeviceId(), deviceAlias);
        }
        _saveSettings();
    }


    /**
     * Retrieve the stored value for the secure code that is used to secure this device connection.
     * @param device
     * The device the secure code is for
     * @return
     * The secure code
     */
    public String getDeviceSecureCode(Device device) {
        return _deviceSecureCodes.get(device.getDeviceId());
    }

    /**
     * Store the secure code to use when attempting to secure the device connection.  This way we
     * don't have to prompt the user for it every time.
     *
     * @param device
     * The device the secure code is for
     * @param secureCode
     * The secure code
     */
    public void setDeviceSecureCode(Device device, String secureCode)
    {
        _deviceSecureCodes.put(device.getDeviceId(), secureCode);
        _saveSettings();
    }

    /**
     * Remove a device from the history
     *
     * @param device
     * the device to remove
     */
    public void forgetDevice(Device device)
    {

        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            if (BluetoothLEDeviceManager.getInstance().hasDevice(device.getDeviceId()))
            {
                BluetoothLEDeviceManager.getInstance().forgetDevice(device.getDeviceId());
            }
        }

        // MARKER: Add additional communication methods here

        _deviceAliases.remove(device.getDeviceId());
        _deviceSecureCodes.remove(device.getDeviceId());
        _saveSettings();
    }

    // LIST DEVICES

    /**
     * Retrieve a specific device
     * @param deviceId
     * the id of the device to retrieve
     * @return
     * the device or null if not found
     */
    public Device getDevice(String deviceId)
    {

        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            if (BluetoothLEDeviceManager.getInstance().hasDevice(deviceId))
            {
                return _flushOut(BluetoothLEDeviceManager.getInstance().getDevice(deviceId));
            }
        }

        // MARKER: Add additional communication methods here

        return null;
    }

    /**
     * Retrieve all devices. Optionally filter by service id.
     * @param serviceId
     * the service id to filter
     * @return
     * The devices that match the filter
     */
    public List<Device> getDevices(String serviceId)
    {

        List<Device> result = new ArrayList<Device>();
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            result.addAll(BluetoothLEDeviceManager.getInstance().getDevices(serviceId));
        }

        // MARKER: Add additional communication methods here
        return _filterDevices(result, null, null, null);
    }

    /**
     * Retrieve all the devices that are connected.  Optionally filter by service id.
     * @param serviceId
     * the service id to filter
     * @return
     * the devices that are connected and match the filter
     */
    public List<Device> getConnectedDevices(String serviceId)
    {
        List<Device> result = new ArrayList<Device>();
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            result.addAll(BluetoothLEDeviceManager.getInstance().getDevices(serviceId));
        }

        // MARKER: Add additional communication methods here
        return _filterDevices(result, true, null, null);
    }

    /**
     * Retrieve all active devices.  Optionally filter by service id.
     * @param serviceId
     * the service id to filter
     * @return
     * the devices that are active and match the filter
     */
    public List<Device> getActiveDevices(String serviceId)
    {

        List<Device> result = new ArrayList<Device>();
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            result.addAll(BluetoothLEDeviceManager.getInstance().getDevices(serviceId));
        }

        // MARKER: Add additional communication methods here
        return _filterDevices(result, null, null, true);
    }

    /**
     * Retrieve all inactive devices.  Optionally filter by service id.
     * @param serviceId
     * the service id to filter
     * @return
     * the devices that are inactive and match the filter
     */
    public List<Device> getInactiveDevices(String serviceId)
    {

        List<Device> result = new ArrayList<Device>();
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            result.addAll(BluetoothLEDeviceManager.getInstance().getDevices(serviceId));
        }

        // MARKER: Add additional communication methods here
        return _filterDevices(result, null, true, false);
    }

    /**
     * Retrieve all out of range devices.  Optionally filter by service id.
     * @param serviceId
     * the service id to filter
     * @return
     * the devices that are out of range and match the filter
     */
    public List<Device> getOutOfRangeDevices(String serviceId)
    {
        List<Device> result = new ArrayList<Device>();
        if (isEnabled(CommunicationMethod.BluetoothLE))
        {
            result.addAll(BluetoothLEDeviceManager.getInstance().getDevices(serviceId));
        }

        // MARKER: Add additional communication methods here
        return _filterDevices(result, null, false, false);
    }

    // ACTIVATE AND CONNECT TO DEVICES

    /**
     * Make a device active and connect to it.  This will allow you to automatically reconnect to the device
     * using the reconnect() method.
     *
     * @param device
     * the device to make active
     * @param deactivateOthers
     * if we should deactivate (and possibly disconnect) from all other devices
     */
    public void activateDevice(Device device, boolean deactivateOthers)
    {

        if (deactivateOthers)
        {
            for (String deviceId : _activeDeviceIds)
            {
                Device oldDevice = getDevice(deviceId);
                if (oldDevice != null)
                {
                    deactivateDevice(oldDevice);
                }
            }

            _activeDeviceIds.clear();
        }

        _activeDeviceIds.add(device.getDeviceId());
        _saveSettings();

        // TODO: Background thread
        if (_delegate != null)
        {
            _delegate.deviceDidActivate(this, device);
        }

        _connect(device);
    }

    /**
     * Make a device inactive and disconnect from it.  This will remove the device from the list of devices to
     * automatically reconnect to using the reconnect() method.
     *
     * @param device
     * the device to make inactive
     */
    public void deactivateDevice(Device device)
    {
        Log.i("CentralDeviceManager", "Deactivate Device");
        _activeDeviceIds.remove(device.getDeviceId());
        _disconnect(device);

        // TODO: Background thread
        if (_delegate != null)
        {
            _delegate.deviceDidDeactivate(this, device);
        }

        _saveSettings();
    }

    /**
     * Check if a device is marked as active
     *
     * @param device
     * the device to check for active
     *
     * @return
     * if the device is active
     */
    public boolean isActive(Device device)
    {
        boolean active = _activeDeviceIds.contains(device.getDeviceId());

        // override
        if (_transientInactiveDeviceIds.contains(device.getDeviceId()))
        {
            active = false;
        }

        return active;
    }

    /**
     * Check if we have any active devices.  Optionally filter by service id.
     *
     * @param serviceId
     * the service to filter
     *
     * @return
     * if we have active devices
     */
    public boolean hasActiveDevices(String serviceId)
    {
        return getActiveDevices(serviceId).size() > 0;
    }

    /**
     * Check if we have any connected devices.  Optionally filter by service id.
     *
     * @param serviceId
     * the service to filter
     *
     * @return
     * if we have connected devices
     */
    public boolean hasConnectedDevices(String serviceId)
    {
        return getConnectedDevices(serviceId).size() > 0;
    }

    // PRIVATE METHODS

    private void _disconnect(Device device)
    {
        _transientInactiveDeviceIds.add(device.getDeviceId());
        device.disconnect();
    }

    private void _connect(Device device)
    {
        _transientInactiveDeviceIds.remove(device.getDeviceId());
        device.connect(_context);
    }

    private Device _flushOut(Device device)
    {

        if (device == null)
        {
            return null;
        }

        String deviceAlias = _deviceAliases.get(device.getDeviceId());
        if (deviceAlias != null)
        {
            device.setDeviceAlias(deviceAlias);
        }

        return device;
    }

    private List<Device> _filterDevices(Collection<Device> devices, Boolean connected, Boolean inRange, Boolean active)
    {
        List<Device> result = new ArrayList<Device>();
        for (Device device : devices)
        {
            Device d = _flushOut(device);
            if (d != null)
            {
                boolean match = true;
                if (connected != null)
                {
                    boolean r = d.isConnected();
                    if ((!r && connected) || (r && !connected))
                    {
                        match = false;
                    }
                }
                if (inRange != null)
                {
                    boolean r = d.isInRange();
                    if ((!r && inRange) || (r && !inRange))
                    {
                        match = false;
                    }
                }
                if (active != null)
                {
                    boolean a = isActive(d);
                    if ((!a && active) || (a && !active))
                    {
                        match = false;
                    }
                }

                if (match)
                {
                    result.add(d);
                }
            }
        }

        return result;
    }

    private void _loadSettings()
    {
        SharedPreferences settings = _context.getSharedPreferences("gymnext_device_aliases.pref", Context.MODE_PRIVATE);
        Map<String, ?> values = settings.getAll();
        for (String key : values.keySet())
        {
            String value = (String) values.get(key);
            _deviceAliases.put(key, value);
        }

        settings = _context.getSharedPreferences("gymnext_device_secure_codes.pref", Context.MODE_PRIVATE);
        values = settings.getAll();
        for (String key : values.keySet())
        {
            String value = (String) values.get(key);
            _deviceSecureCodes.put(key, value);
        }

        settings = _context.getSharedPreferences("gymnext_active_device_ids.pref", Context.MODE_PRIVATE);
        _activeDeviceIds = settings.getStringSet("active_devices", new HashSet<String>());
        Log.i("CentralDeviceManager", "Load Active Devices: " + _activeDeviceIds.size());
    }

    private void _saveSettings()
    {
        SharedPreferences settings = _context.getSharedPreferences("gymnext_device_aliases.pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        for (String key : _deviceAliases.keySet())
        {
            String value = _deviceAliases.get(key);
            editor.putString(key, value);
        }
        editor.commit();

        settings = _context.getSharedPreferences("gymnext_device_secure_codes.pref", Context.MODE_PRIVATE);
        editor = settings.edit();

        for (String key : _deviceSecureCodes.keySet())
        {
            String value = _deviceSecureCodes.get(key);
            editor.putString(key, value);
        }
        editor.commit();

        settings = _context.getSharedPreferences("gymnext_active_device_ids.pref", Context.MODE_PRIVATE);
        editor = settings.edit();
        if (_activeDeviceIds.size() == 0) {
            editor.remove("active_devices");
        }
        else {
            editor.putStringSet("active_devices", _activeDeviceIds);
        }
        boolean b = editor.commit();

        Log.i("CentralDeviceManager","Save Active Devices: " + b + " - " + _activeDeviceIds.size());

        settings = _context.getSharedPreferences("gymnext_active_device_ids.pref", Context.MODE_PRIVATE);
        _activeDeviceIds = settings.getStringSet("active_devices", new HashSet<String>());
        Log.i("CentralDeviceManager", "Load Active Devices: " + _activeDeviceIds.size());
    }

    private void _checkForFinishedReconnecting(Device device)
    {
        if (_reconnectingDevices.get(device.getDeviceId()) != null)
        {
            _reconnectingDevices.remove(device.getDeviceId());

            if (_reconnectingDevices.size() == 0)
            {
                if (_delegate != null)
                {
                    _delegate.deviceManagerDidFinishReconnectingDevices(this);
                }
            }
        }
    }

    private void _checkForFinishedDisconnecting(Device device)
    {
        if (_disconnectingDevices.get(device.getDeviceId()) != null)
        {
            _disconnectingDevices.remove(device.getDeviceId());

            if (_disconnectingDevices.size() == 0)
            {
                if (_delegate != null)
                {
                    _delegate.deviceManagerDidFinishDisconnectingDevices(this);
                }
            }
        }
    }

    // Device Manager Delegate

    /**
     * Internal method
     * @param manager
     * @param device
     */
    public final void deviceDidMoveOutOfRange(BluetoothLEDeviceManager manager, BluetoothLEDevice device)
    {
        if (_delegate != null)
        {
            _delegate.deviceDidMoveOutOfRange(this, device);
        }
    }

    /**
     * Internal method
     * @param manager
     * @param device
     */
    public final void deviceDidMoveInRange(BluetoothLEDeviceManager manager, BluetoothLEDevice device)
    {
        if (_delegate != null)
        {
            _delegate.deviceDidMoveInRange(this, device);
        }
    }

    /**
     * Internal method
     * @param manager
     * @param device
     */
    public final void deviceDidConnect(BluetoothLEDeviceManager manager, BluetoothLEDevice device)
    {
        _checkForFinishedReconnecting(device);

        if (_delegate != null)
        {
            _delegate.deviceDidConnect(this, device);
        }
    }

    /**
     * Internal method
     * @param manager
     * @param device
     */
    public final void deviceDidFailToConnect(BluetoothLEDeviceManager manager, BluetoothLEDevice device)
    {
        _checkForFinishedReconnecting(device);

        if (_delegate != null)
        {
            _delegate.deviceDidFailToConnect(this, device);
        }
    }

    /**
     * Internal method
     * @param manager
     * @param device
     */
    public final void deviceDidDisconnect(BluetoothLEDeviceManager manager, BluetoothLEDevice device)
    {
        _checkForFinishedDisconnecting(device);

        if (_delegate != null)
        {
            _delegate.deviceDidDisconnect(this, device);
        }
    }

}
