package com.gymnext.gymnextsdk.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.gymnext.gymnextsdk.timer.DisplayMode;
import com.gymnext.gymnextsdk.SecureMode;
import com.gymnext.gymnextsdk.timer.StatusMode;
import com.gymnext.gymnextsdk.timer.TimerSchedule;
import com.gymnext.gymnextsdk.timer.TimerService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;

public class BluetoothLETimerService implements BluetoothLEService, TimerService {
    private static class BluetoothLEOperation {

        private BluetoothGattDescriptor _descriptor;
        private BluetoothGattCharacteristic _characteristic;
        private byte[] _value;

        private BluetoothLEOperation(BluetoothGattDescriptor descriptor, byte[] value) {
            _descriptor = descriptor;
            _value = value;
        }

        private BluetoothLEOperation(BluetoothGattCharacteristic characteristic, byte[] value) {
            _characteristic = characteristic;
            _value = value;
        }

        public BluetoothGattCharacteristic getCharacteristic() {
            return _characteristic;
        }

        public BluetoothGattDescriptor getDescriptor() {
            return _descriptor;
        }

        public byte[] getValue() {
            return _value;
        }
    }

    // UUIDs for UART service and associated characteristics.
//    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
//    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
//    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    public static UUID UART_UUID = UUID.fromString("028c8db0-fb17-11e4-a322-1697f925ec7b");
    public static UUID TX_UUID = UUID.fromString("028c8db2-fb17-11e4-a322-1697f925ec7b");
    public static UUID RX_UUID = UUID.fromString("028c8db1-fb17-11e4-a322-1697f925ec7b");

    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean _sendingInitialCommunication = false;

    // provided
    private String _secureCode = null;
    private BluetoothGatt _gatt;
    private String _deviceName;

    // Info
    private int _hardwareVersion;
    private int _softwareVersion;

    // On/Off
    private boolean _power = true;

    // Admin
    private SecureMode _secureMode = SecureMode.None;
    private boolean _secured = false;

    private boolean _12h = true;
    private boolean _showClockSeconds = false;
    private int _timeZoneOffset = -60 * 5;
    private boolean _timeZonePositive = false;

    private DisplayMode _displayMode = DisplayMode.Clock;
    private StatusMode _statusMode = StatusMode.None;
    private boolean _mute = false;
    private boolean _continuity = false;
    private boolean _direction = false;
    private boolean _segue = false;
    private int _prelude = 10;

    // Timer
    private boolean _running = false;
    private boolean _started = false;
    private boolean _finished = false;

    private BluetoothGattService _service;
    private BluetoothGattCharacteristic _rxCharacteristic;
    private BluetoothGattCharacteristic _txCharacteristic;

    private Queue<BluetoothLEOperation> _btleCommandQueue = new LinkedList<BluetoothLEOperation>();

    // Constructor
    public BluetoothLETimerService(String secureCode, BluetoothGatt gatt) {
        Log.i("BluetoothLETimerService", "Timer Service Constructed");
        _gatt = gatt;
        _secureCode = secureCode;
        _deviceName = gatt.getDevice().getName();
    }

    public String getId() {
        return TimerService.SERVICE_ID;
    }

    @Override
    public boolean isSendingInitialCommunication() {
        return _sendingInitialCommunication;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //
    // INFORMATION
    //
    ////////////////////////////////////////////////////////////////////////////////////
    public int getHardwareVersion() {
        return _hardwareVersion;
    }

    public int getSoftwareVersion() {
        return _softwareVersion;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //
    // STARTUP/POWER
    //
    ////////////////////////////////////////////////////////////////////////////////////

    public boolean seedClock(int seed) {
        return _doCommand("XC?" + seed);
    }

    public boolean seedTimer(int seed) {
        return _doCommand("XT?" + seed);
    }

    public boolean isPowerOn() {
        return _power;
    }

    public boolean setPower(boolean b) {
        return b ? powerOn() : powerOff();
    }

    public boolean togglePower() {
        return _doCommand("@P");
    }

    public boolean powerOn() {
        return _doCommand("P1");
    }

    public boolean powerOff() {
        return _doCommand("P0");
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //
    // ADMINISTRATION
    //
    ////////////////////////////////////////////////////////////////////////////////////

    public String getDeviceName() {
        return _deviceName;
    }

    public boolean setDeviceName(String deviceName) {
        return _doCommand("NM?" + deviceName);
    }

    public boolean isSecured() {
        return _secured;
    }

    public boolean verifySecureCode(String secureCode) {
        return _doCommand("VS?" + secureCode);
    }

    public boolean setSecureCode(String secureCode) {
        return _doCommand("SS?" + secureCode);
    }

    public SecureMode getSecureMode() {
        return _secureMode;
    }

    public boolean setSecureModeNone() {
        return _doCommand("SN");
    }

    public boolean setSecureModeAdmin() {
        return _doCommand("SA");
    }
    public boolean setSecureModeAll() {
        return _doCommand("SL");
    }

    public boolean isTwelveHourClockOn() {
        return _12h;
    }

    public boolean toggleTwelveHourClock() {
        return _doCommand("@H");
    }

    public boolean setTwelveHourClock(boolean b) {
        return b ? twelveHourClockOn() : twelveHourClockOff();
    }

    public boolean twelveHourClockOn() {
        return _doCommand("H1");
    }

    public boolean twelveHourClockOff() {
        return _doCommand("H0");
    }

    public boolean isShowClockSecondsOn() {
        return _showClockSeconds;
    }

    public boolean toggleShowClockSeconds() {
        return _doCommand("@E");
    }

    public boolean setShowClockSeconds(boolean b) {
        return b ? showClockSecondsOn() : showClockSecondsOff();
    }

    public boolean showClockSecondsOn() {
        return _doCommand("E1");
    }

    public boolean showClockSecondsOff() {
        return _doCommand("E0");
    }

    public boolean setTimeZoneOffset(int offset) {
        int h = offset / 60;
        int m = offset % 60;

        return _doCommand("TZ?" + h + "," + m);
    }

    public int getTimeZoneOffset() {
        return _timeZoneOffset;
    }


    ////////////////////////////////////////////////////////////////////////////////////
    //
    // GENERAL
    //
    ////////////////////////////////////////////////////////////////////////////////////

    public DisplayMode getDisplayMode() {
        return _displayMode;
    }

    public boolean setDisplayModeClock() {
        return _doCommand("CL");
    }

    public boolean setDisplayModeTimer() {
        return _doCommand("TI");
    }
    public boolean setDisplayModeMessage() {
        return _doCommand("ME");
    }

    public StatusMode getStatusMode() {
        return _statusMode;
    }

    public boolean setStatusModeNone() {
        return _doCommand("NO");
    }

    public boolean setStatusModeInterval() {
        return _doCommand("IN");
    }

    public boolean setStatusModeRepetition() {
        return _doCommand("IL");
    }

    public boolean setStatusModeCustom() {
        return _doCommand("CU");
    }

    public boolean isMuteOn() {
        return _mute;
    }

    public boolean toggleMute() {
        return _doCommand("@M");
    }

    public boolean setMute(boolean b) {
        return b ? muteOn() : muteOff();
    }

    public boolean muteOn() {
        return _doCommand("M1");
    }

    public boolean muteOff() {
        return _doCommand("M0");
    }

    public boolean isSegueOn() {
        return _segue;
    }
    public boolean setSegue(boolean b) {
        return b ? segueOn() : segueOff();
    }

    public boolean toggleSegue() {
        return _doCommand("@S");
    }

    public boolean segueOn() {
        return _doCommand("S1");
    }

    public boolean segueOff() {
        return _doCommand("S0");
    }


    public boolean isDirectionUp() {
        return _direction;
    }

    public boolean setDirection(boolean b) {
        return b ? directionUp() : directionDown();
    }

    public boolean toggleDirection() {
        return _doCommand("@D");
    }

    public boolean directionUp() {
        return _doCommand("D1");
    }

    public boolean directionDown() {
        return _doCommand("D0");
    }
    public boolean setPrelude(int prelude) {
        return _doCommand("PR?" + prelude);
    }

    public int getPrelude() {
        return _prelude;
    }





    public boolean isRunning() {
        return _running;
    }

    public boolean isStarted() {
        return _started;
    }

    public boolean isFinished() {
        return _finished;
    }


    public boolean start() {
        return _doCommand("_S");
    }

    public boolean pause() {
        return _doCommand("_P");
    }

    public boolean reset() {
        return _doCommand("_R");
    }

    public boolean clear() {
        return _doCommand("_C");
    }

    public boolean buzz(boolean longBuzz) {
        return _doCommand("ZZ?" + (longBuzz? "1" : "0"));
    }

    public boolean buzzRaw(int durationMilliseconds) {
        return _doCommand("ZR?" + durationMilliseconds);
    }

    public boolean setMessage(String message) {
        return _doCommand("XM?" + message);
    }

    public boolean setMessageRaw(int[] message) {
        StringBuilder sb = new StringBuilder();
        sb.append("XR?");

        boolean first = true;
        for (int i : message) {
            if (!first) {
                sb.append(",");
            }

            sb.append(i);
            first = false;
        }
        return _doCommand(sb.toString());
    }


    public boolean flashMessage(int duration, String message) {
        return _doCommand("FM?" + duration + "," + message);
    }

    public boolean flashMessageRaw(int duration, int[] message) {
        StringBuilder sb = new StringBuilder();
        sb.append("FR?");
        sb.append(duration);

        for (int i : message) {
            sb.append(",");
            sb.append(i);
        }
        return _doCommand(sb.toString());
    }

    public boolean setCustomStatus(String customStatus) {
        return _doCommand("ST?" + customStatus);
    }

    public boolean setCustomStatusRaw(int[] customStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("SR?");

        boolean first = true;
        for (int i : customStatus) {
            if (!first) {
                sb.append(",");
            }

            sb.append(i);
            first = false;
        }
        return _doCommand(sb.toString());
    }

    public boolean setSchedule(boolean reset, int prelude, boolean segue, boolean continuous, StatusMode statusMode, TimerSchedule schedule)
    {
        List<TimerSchedule> schedules = new ArrayList<TimerSchedule>();
        schedules.add(schedule);
        return setSchedules(reset, prelude, segue, continuous, statusMode, schedules);
    }

    public boolean setSchedules(boolean reset, int prelude, boolean segue, boolean continuous, StatusMode statusMode, List<TimerSchedule> schedules)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(reset ? "_R;" : "");
        sb.append("PR?").append(prelude).append(";");
        sb.append(segue ? "S1;" : "S0;");
        sb.append(continuous ? "C1;" : "C0;");
        if (statusMode == StatusMode.None) {
            sb.append("NO;");
        }
            else if (statusMode == StatusMode.Interval) {
            sb.append("IN;");
        }
            else if (statusMode == StatusMode.Repetition) {
            sb.append("IL;");
        }
            else if (statusMode == StatusMode.Custom) {
            sb.append("CU;");
        }
        sb.append("SC?");

        boolean first = true;
        for (TimerSchedule schedule : schedules) {
        if (!first) {
            sb.append("|");
        }

        sb.append(schedule.intervals.size()).append(",");
        sb.append(schedule.restBetweenIntervals).append(",");
        sb.append(schedule.numberOfRepetitions).append(",");
        sb.append(schedule.restBetweenRepetitions).append(",");

        boolean first2 = true;
        for (TimerSchedule.TimerInterval interval : schedule.intervals) {
            if (!first2) {
                sb.append(",");
            }

            if (interval.intervalType == TimerSchedule.TimerIntervalType.Work) {
                sb.append(interval.duration);
            }
            else if (interval.intervalType == TimerSchedule.TimerIntervalType.Rest) {
                sb.append("R").append(interval.duration);
            }
            first2 = false;
        }

        first = false;
    }
        return _doCommand(sb.toString());
    }

    private boolean _doCommand(String command) {
        return _writeString(command + ";");
    }

    public void didDiscoverServiceAndCharacteristics(BluetoothGattService service) {
        Log.i("LETimerService", "didDiscoverServiceAndCharacteristics");
        if (service.getUuid().equals(UART_UUID)) {
            Log.i("LETimerService", "Get service, set notification, send initial");

            _service = service;
            _txCharacteristic = _service.getCharacteristic(TX_UUID);
            _rxCharacteristic = _service.getCharacteristic(RX_UUID);

            Log.i("LETimerService", "TX: " + _txCharacteristic.getUuid());
            Log.i("LETimerService", "RX: " + _rxCharacteristic.getUuid());
            Log.i("LETimerService", "Service: " + _service.getUuid());

            // Setup notifications on RX characteristic changes (i.e. data received).
            if (!_gatt.setCharacteristicNotification(_rxCharacteristic, true)) {
                Log.w("LETimerService", "Characteristic notification setup failed");
            }

            // Next update the RX characteristic's client descriptor to enable notifications.
            BluetoothGattDescriptor desc = _rxCharacteristic.getDescriptor(CLIENT_UUID);
            if (desc == null) {
                Log.w("LETimerService", "Failed to get descriptor");
            } else {
                _addDescriptorWriteToQueue(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }

            sendInitialCommunication();
        }
    }

    public void didReceiveUpdateValueForCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.i("LETimerService", "Update to " + characteristic.toString());
        if (characteristic == _rxCharacteristic) {
            if (characteristic.getStringValue(0).length() >= 10) {

                int[] data = new int[10];
                for (int i = 0; i < 10; i++) {
                    data[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i);
                }

                if (data[0] != 0) {
                    // error
                    return;
                }

                // Byte 0 - request result
                // Byte 1 - timer settings
                // Byte 2 - timer settings
                // Byte 3 - prelude
                // Byte 4 - timezone H
                // Byte 5 - timezone M
                // Byte 6 - timer mode

                int state = data[1];
                int state2 = data[2];

                // bit 0 - success
                // bit 1 - display mode
                // bit 2 - mute
                // bit 3 - 12h/24h
                // bit 4 - up/down
                // bit 5 - continuous/interval
                // bit 6 - started
                // bit 7 - running

                if ((state & 1) == 1) {
                    _displayMode = DisplayMode.Timer;
                }
                else if ((state & 2) == 2) {
                    _displayMode = DisplayMode.Message;
                }
                else {
                    _displayMode = DisplayMode.Clock;
                }

                _mute = (state & 4) == 4;
                _direction = (state & 8) == 8;
                _continuity = (state & 16) == 16;
                _started = (state & 32) == 32;
                _running = (state & 64) == 64;
                _finished = (state & 128) == 128;

                _timeZonePositive = (state2 & 1) == 1;
                _segue = (state2 & 2) == 2;
                _power = (state2 & 4) == 4;
                _showClockSeconds = (state2 & 8) == 8;
                _secured = (state2 & 16) == 16;
                _12h = (state2 & 32) == 32;

                if (_timeZonePositive) {
                    _timeZoneOffset = (data[4]) * 60 + (data[5]);
                } else {
                    _timeZoneOffset = ((data[4]) * 60 + (data[5])) * -1;
                }

                _prelude = data[3];

                if (data[6] == 0) {
                    _secureMode = SecureMode.None;
                }
                else if (data[6] == 1) {
                    _secureMode = SecureMode.Admin;
                }
                else if (data[6] == 2) {
                    _secureMode = SecureMode.All;
                }

                if (data[7] == 0) {
                    _statusMode = StatusMode.None;
                }
                else if (data[7] == 1) {
                    _statusMode = StatusMode.Interval;
                }
                else if (data[7] == 2) {
                    _statusMode = StatusMode.Repetition;
                }
                else if (data[7] == 3) {
                    _statusMode = StatusMode.Custom;
                }

                _hardwareVersion = data[8];
                _softwareVersion = data[9];

                _sendingInitialCommunication = false;
            }
        }
    }

    public void didWriteValueForCharacteristic(BluetoothGattCharacteristic characteristic) {
        _popQueue();
    }

    public void didWriteValueForDescriptor(BluetoothGattDescriptor descriptor) {
        _popQueue();
    }

    public void didReadValueForCharacteristic(BluetoothGattCharacteristic characteristic) {
        // Ignored
    }

    public void didReadValueForDescriptor(BluetoothGattDescriptor descriptor) {
        // Ignored
    }

    public boolean _writeString(String string) {
            Log.i("LETimerService", "Writing String: " + string + " to " + _txCharacteristic.getUuid());

            int len = string.length();
            int pos = 0;
            while (len != 0) {
                if (len >= 20) {
                    byte[] data = string.substring(pos, pos + 20).getBytes(Charset.forName("UTF-8"));
                    _addCharacteristicWriteToQueue(_txCharacteristic, data);
                    len -= 20;
                    pos += 20;
                } else {
                    byte[] data = string.substring(pos, pos + len).getBytes(Charset.forName("UTF-8"));
                    _addCharacteristicWriteToQueue(_txCharacteristic, data);
                    len = 0;
                }
            }
        return true;
    }

    public void sendInitialCommunication() {

        _sendingInitialCommunication = true;

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long now = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long passed = now - c.getTimeInMillis();
        long secondsPassed = passed / 1000;

        StringBuilder sb = new StringBuilder();
        if (_secureCode != null) {
            sb.append("VS?").append(_secureCode).append(";");
        }
        else {
            sb.append("VS?0000;");
        }
        sb.append("XC?").append(secondsPassed).append(";");
        sb.append("P1;");
        sb.append("XX"); // Last command must be status request since it can be run in secure and non-secure modes

        _doCommand(sb.toString());
    }



    private synchronized void _addCharacteristicWriteToQueue(BluetoothGattCharacteristic characteristic, byte[] value) {
        _btleCommandQueue.add(new BluetoothLEOperation(characteristic, value));

        if (_btleCommandQueue.size() == 1) {
            Log.i("BluetoothLETimerService", "Write Charactertistic " + value.length);
            characteristic.setValue(value);
            _gatt.writeCharacteristic(characteristic);
        }
    }

    private synchronized void _addDescriptorWriteToQueue(BluetoothGattDescriptor descriptor, byte[] value) {
        _btleCommandQueue.add(new BluetoothLEOperation(descriptor, value));

        if (_btleCommandQueue.size() == 1) {
            descriptor.setValue(value);
            _gatt.writeDescriptor(descriptor);
        }
    }

    private synchronized void _popQueue() {
        _btleCommandQueue.remove();

        if (_btleCommandQueue.size() > 0) {
            BluetoothLEOperation operation = _btleCommandQueue.element();
            if (operation.getDescriptor() != null) {
                operation.getDescriptor().setValue(operation.getValue());
                _gatt.writeDescriptor(operation.getDescriptor());
            } else if (operation.getCharacteristic() != null) {
                operation.getCharacteristic().setValue(operation.getValue());
                Log.i("BluetoothLETimerService", "Write Charactertistic " + operation.getValue().length);
                _gatt.writeCharacteristic(operation.getCharacteristic());
            }
        }
    }

}
