package com.gymnext.gymnextsdk;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for all devices that you can connect to.  Connecting to a device allows you
 * to access the service interfaces that device supports.  Then using these interfaces
 * you can request the device perform some action or provide some data.
 */
public abstract class Device
{

    /**
     * Main listener for this device
     */
    public interface DeviceDelegate
    {
        // Device connection

        /**
         * Callback for when communication channel has been established with the device
         * @param device
         * the device
         */
        public void establishedCommunicationChannel(Device device);

        // Device Communication
        /**
         * Callback for when data has been sent to the device
         * @param device
         * the device
         */
        public void sentDataToDevice(Device device);

        /**
         * Callback for when data has been received by the device
         * @param device
         * the device
         */
        public void receivedDataFromDevice(Device device);
    }

    private static final String KEY_DEVICE_ID = "deviceId";
    private static final String KEY_DEVICE_NAME = "deviceName";
    private static final String KEY_DEVICE_ALIAS = "deviceAlias";
    private static final String KEY_MANUFACTURER_NAME = "manufacturerName";
    private static final String KEY_MODEL_NAME = "modelName";
    private static final String KEY_SERVICE_IDS = "serviceIds";

    protected DeviceDelegate _delegate;
    protected String _deviceId;
    protected DeviceState _deviceState = DeviceState.OutOfRange;
    protected String _manufacturerName;
    protected String _modelName;
    protected String _deviceName;
    protected String _deviceAlias;
    protected Set<String> _serviceIds = new HashSet<String>();

    public Device(String deviceId, String deviceName, String deviceAlias, String manufacturerName, String modelName)
    {
        _deviceId = deviceId;
        _deviceName = deviceName;
        _deviceAlias = deviceAlias;
        _manufacturerName = manufacturerName;
        _modelName = modelName;
    }

    protected Device(JSONObject deviceJson)
    {
        _deviceId = deviceJson.optString(KEY_DEVICE_ID, "unknown");
        _deviceName = deviceJson.optString(KEY_DEVICE_NAME, "unknown");
        _deviceAlias = deviceJson.optString(KEY_DEVICE_ALIAS, null);
        _manufacturerName = deviceJson.optString(KEY_MANUFACTURER_NAME, "unknown");
        _modelName = deviceJson.optString(KEY_MODEL_NAME, "unknown");

        _serviceIds = new HashSet<String>();
        final JSONArray serviceIdsJson = deviceJson.optJSONArray(KEY_SERVICE_IDS);
        if (serviceIdsJson != null)
        {
            for (int i = 0; i < serviceIdsJson.length(); i++)
            {
                _serviceIds.add(serviceIdsJson.optString(i, "unknown"));
            }
        }
    }

    /**
     * Retrieve the device's delegate
     * @return
     * The device delegate
     */
    public DeviceDelegate getDelegate()
    {
        return _delegate;
    }

    /**
     * Set the device's delegate
     * @param delegate
     * The device delegate
     */
    public void setDelegate(DeviceDelegate delegate)
    {
        _delegate = delegate;
    }

    /**
     * Retrieve the device id
     * @return
     * unique device id
     */
    public String getDeviceId()
    {
        return _deviceId;
    }

    /**
     * Retrieve the name of the device to display to the user.  Uses alias if set, otherwise
     * device name.
     * @return
     * The display name of the device
     */
    public String getDisplayName() {
        if (_deviceAlias == null) {
            return _deviceName;
        }
        return _deviceAlias;
    }

    /**
     * Retrieve the device name
     * @return
     * The name of the device
     */
    public String getDeviceName()
    {
        return _deviceName;
    }

    /**
     * Set the device name
     * @param deviceName
     * the name of the device
     */
    public void setDeviceName(String deviceName) {
        _deviceName = deviceName;
    }

    /**
     * Retrieve the device alias
     * @return
     * the alias of the device
     */
    public String getDeviceAlias() {
        return _deviceAlias;
    }

    /**
     * Set the device alias
     * @param deviceAlias
     * the alias of the device
     */
    public void setDeviceAlias(String deviceAlias)
    {
        _deviceAlias = deviceAlias;
    }

    /**
     * Get the manufacturer name (if known)
     * @return
     * the name of the manufacturer
     */
    public String getManufacturerName()
    {
        return _manufacturerName;
    }

    /**
     * Ge the model name (if known)
     * @return
     * the name of the model
     */
    public String getModelName()
    {
        return _modelName;
    }

    /**
     * Retrieve the state of the device
     * @return
     * The device state
     */
    public DeviceState getDeviceState()
    {
        return _deviceState;
    }

    /**
     * Update the state of the device
     * @param deviceState
     * The device state
     */
    public void setDeviceState(DeviceState deviceState)
    {
        _deviceState = deviceState;
    }

    /**
     * Retrieve if the device is connected
     * @return
     * If the device is connected
     */
    public boolean isConnected()
    {
        return _deviceState == DeviceState.Connected;
    }

    /**
     * Retrieve if the device is in range
     * @return
     * If the device is in range
     */
    public boolean isInRange() {
        return _deviceState != DeviceState.OutOfRange;
    }

    /**
     * Retrieve the services supported by this device
     * @return
     * The services this device supports
     */
    public Set<String> getServiceIds()
    {
        return _serviceIds;
    }

    /**
     * Set the services this device supports
     * @param serviceIds
     * The services this device supports
     */
    public void setServiceIds(Set<String> serviceIds)
    {
        _serviceIds = serviceIds;
    }

    /**
     * Check if the device supports a given service
     * @param serviceId
     * The service to check
     * @return
     * true if the service is supported
     */
    public boolean hasService(String serviceId)
    {
        return _serviceIds.contains(serviceId);
    }

    /**
     * The communication method used by this device
     * @return
     * The communication method
     */
    public abstract CommunicationMethod getCommunicationMethod();

    /**
     * The implementation of the service interface for this device
     * @param serviceId
     * The service to retrieve
     * @return
     * The service to use
     */
    public abstract Service getService(String serviceId);

    /**
     * Connect to the device
     */
    public abstract void connect(Context context);

    /**
     * Disconnect from the device
     */
    public abstract void disconnect();

    /**
     * Internal callback for did connect
     */
    public abstract void didConnect();

    /**
     * Internal callback for did disconnect
     */
    public abstract void didDisconnect();

    /**
     * Internal callback for did fail to connect
     */
    public abstract void didFailToConnect();

    /**
     * Internal callback for did move into range
     */
    public abstract void didMoveIntoRange();

    /**
     * Internal callback for did move out of range
     */
    public abstract void didMoveOutOfRange();

    public JSONObject toJson()
    {
        try
        {
            final JSONObject result = new JSONObject();
            result.put(KEY_DEVICE_ID, _deviceId);
            result.put(KEY_DEVICE_NAME, _deviceName);
            result.put(KEY_DEVICE_ALIAS, _deviceAlias);
            result.put(KEY_MANUFACTURER_NAME, _manufacturerName);
            result.put(KEY_MODEL_NAME, _modelName);

            JSONArray array = new JSONArray();
            for (String serviceId : _serviceIds)
            {
                array.put(serviceId);
            }

            result.put(KEY_SERVICE_IDS, array);
            return result;
        }
        catch (JSONException e)
        {
            return new JSONObject();
        }
    }

}
