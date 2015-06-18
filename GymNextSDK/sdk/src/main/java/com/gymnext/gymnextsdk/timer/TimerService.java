package com.gymnext.gymnextsdk.timer;

import com.gymnext.gymnextsdk.SecureMode;

import java.util.List;

/**
 * Provides access to the internal state of the timer and actions that it can perform
 */
public interface TimerService
{
    public static final String SERVICE_ID = "TimerService";

    // Information

    /**
     * Retrieve the hardware version of the timer.
     *
     * 0x01 - Timer 2,4
     *
     * @return
     * the hardware version of the timer
     */
    public int getHardwareVersion();

    /**
     * Retrieve the software version running on the timer.
     *
     * 0x01 - v1.0
     *
     * @return
     * the software version of the timer
     */
    public int getSoftwareVersion();

    // Startup/Power

    /**
     * Asynchronous Request - Seed the clock with the current time in seconds
     *
     * @param seed
     * The current UTC time in seconds for the day
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean seedClock(int seed);

    /**
     * Asynchronous Request - Seed the timer with the current elapsed time in seconds
     * @param seed
     * The current elapsed time (including prelude) in seconds
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean seedTimer(int seed);

    /**
     * If the timer is powered on
     * @return
     * If the timer is powered on
     */
    public boolean isPowerOn();

    /**
     * Asynchronous Request - Flip the power on/off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean togglePower();

    /**
     * Asynchronous Request - Turn the power on/off based on flag
     * @param power
     *  to turn the power on or off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setPower(boolean power);

    /**
     * Asynchronous Request - Turn the power on
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean powerOn();

    /**
     * Asynchronous Request - Turn the power off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean powerOff();

    // Administration

    /**
     * The secure mode the timer is currently running in
     * @return
     * The secure mode
     */
    public SecureMode getSecureMode();

    /**
     * Asynchronous Request - Turn secure mode to off
     *
     * After this request, all requests will succeed without securing the connection.
     *
     * Privileges Required: Admin
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSecureModeNone();

    /**
     * Asynchronous Request - Turn secure mode to admin
     *
     * After this request, all requests that require admin privileges will first require that the connection has been secured.
     *
     * Privileges Required: Admin

     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSecureModeAdmin();

    /**
     * Asynchronous Request - Turn secure mode to all
     *
     * After this request, all requests will first require that the connection has been secured.
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSecureModeAll();


    /**
     * Asynchronous Request - Change the secure code
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSecureCode(String secureCode);

    /**
     * Check if the connection has been secured
     *
     * @return
     * if the connection has been secured
     */
    public boolean isSecured();

    /**
     * Asynchronous Request - Verify the secure code and secure the connection
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean verifySecureCode(String secureCode);
    /**
     * Retrieve the name of the device
     * @return
     * the name of the device
     */
    public String getDeviceName();

    /**
     * Asynchronous Request - Change the name of the device (7 character max)
     *
     * Note:  After this request completes, the timer will reset itself and your connection will be lost.
     *
     * @param deviceName
     * The name of the device
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setDeviceName(String deviceName);

    /**
     * Retrieve if the clock is set for 12 hour or 24 hour display
     * @return
     * twelve hour display enabled
     */
    public boolean isTwelveHourClockOn();

    /**
     * Asynchronous Request - Toggle if 12h/24h clock display should be used
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean toggleTwelveHourClock();

    /**
     * Asynchronous Request - Set twelve hour clock based on flag
     * @param twelveHourClock
     * if twelve hour clock display should be used
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setTwelveHourClock(boolean twelveHourClock);

    /**
     * Asynchronous Request - Turn twelve hour clock on
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean twelveHourClockOn();

    /**
     * Asynchronous Request - Turn twelve hour clock off
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean twelveHourClockOff();


    /**
     * Retrieve if the clock should be showing seconds or not.  If the clock is to show seconds, then it will display time
     * in the format HH:MM:SS.  If not, it will display time in the format HH:MM.
     *
     * @return
     * If the clock is showing seconds
     */
    public boolean isShowClockSecondsOn();

    /**
     * Asynchronous Request - Flip the toggle seconds options
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean toggleShowClockSeconds();

    /**
     * Asynchronous Request - Set the show clock seconds option based on flag
     * @param showClockSeconds
     * Whether or not to show clock seconds
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setShowClockSeconds(boolean showClockSeconds);

    /**
     * Asynchronous Request - Turn show clock seconds on
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean showClockSecondsOn();

    /**
     * Asynchronous Request - Turn show clock seconds off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean showClockSecondsOff();

    /**
     * Retrieve the current timezone offset in minutes
     * @return
     * The current time zone offset
     */
    public int getTimeZoneOffset();

    /**
     * Asynchronous Request - Set the timezone offset to use
     * @param offset
     * The offset in minutes from UTC
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setTimeZoneOffset(int offset);


    // General

    /**
     * Retrieve the current display mode the timer is in
     * @return
     * The current display mode
     */
    public DisplayMode getDisplayMode();

    /**
     * Asynchronous Request - Set the display mode to show clock
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setDisplayModeClock();

    /**
     * Asynchronous Request - Set the display mode to show timer
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setDisplayModeTimer();

    /**
     * Asynchronous Request - Set the display mode to show the message
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setDisplayModeMessage();

    /**
     * The status mode controls what is displayed in the status digits.  On a 6 digit timer, the two left most digits are used for
     * status and the 4 right most digits show the time elapsed/remaining.  Refer to the status mode enum for an explanation of each
     * status mode.
     * @return
     * The current status mode
     */
    public StatusMode getStatusMode();

    /**
     * Asynchronous Request - Controls the status mode that is used when the timer is running.
     *
     * By setting the status mode to none, there will be no data shown in the status digits.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setStatusModeNone();

    /**
     * Asynchronous Request - Controls the status mode that is used when the timer is running.
     *
     * By setting the status mode to interval, the status digits will show the interval number.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setStatusModeInterval();

    /**
     * Asynchronous Request - Controls the status mode that is used when the timer is running.
     *
     * By setting the status mode to repetition, the status digits will show the repetition number.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setStatusModeRepetition();

    /**
     * Asynchronous Request - Controls the status mode that is used when the timer is running.
     *
     * By setting the status mode to custom, the status digits will show the values specified via setCustomStatus()
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setStatusModeCustom();

    /**
     * Retrieve if the timer is currently muted.  If the timer is muted it will not make any beeps.
     * @return
     * if the timer is muted
     */
    public boolean isMuteOn();

    /**
     * Asynchronous Request - Toggle if the timer is muted
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean toggleMute();

    /**
     * Asynchronous Request - Set the mute option based on a flag
     * @param mute
     * Whether or not to turn on mute
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setMute(boolean mute);

    /**
     * Asynchronous Request - Turn mute on
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean muteOn();

    /**
     * Asynchronous Request - Turn mute off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean muteOff();

    /**
     * Retrieve if the segue is on.  If the segue is on, then at the end of each interval and at the end of the program, the last three seconds will feature a beep.
     * @return
     * If the segue is on
     */
    public boolean isSegueOn();

    /**
     * Asynchronous Request - Toggle the segue
     *
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean toggleSegue();

    /**
     * Asynchronous Request - Set segue based on flag
     *
     * @param segue
     * Whether or not to turn on the segue
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSegue(boolean segue);

    /**
     * Asynchronous Request - Turn segue on
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean segueOn();

    /**
     * Asynchronous Request - Turn segue off
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean segueOff();


    /**
     * Retrieve if the timer is counting up (showing elapsed time) or counting down (showing remaining time)
     * @return
     * If the timer direction is up
     */
    public boolean isDirectionUp();

    /**
     * Asynchronous Request - Flip the direction of the timer
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean toggleDirection();

    /**
     * Asynchronous Request - Set the direction of the timer based on a flag
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setDirection(boolean directionUp);

    /**
     * Asynchronous Request - Set the direction of the timer to up (shows elapsed)
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean directionUp();

    /**
     * Asynchronous Request - Set the direction of the timer to down (shows remaining)
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean directionDown();

    /**
     * Retrieve the prelude in seconds 0-99).  The prelude is an amount of time that runs before the timer program to allow
     * everyone to prepare and get in the proper position.
     *
     * @return
     * The prelude in seconds
     */
    public int getPrelude();

    /**
     * Asynchronous Request - Set the prelude to a specific duration.
     * @param prelude
     * the prelude duration in seconds
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setPrelude(int prelude);

    /**
     * Check if the timer is actively running
     *
     * @return
     * If the timer is actively running
     */
    public boolean isRunning();

    /**
     * Check if the timer has been started (more than 0 seconds has elapsed)
     * @return
     * If the timer has been started
     */
    public boolean isStarted();

    /**
     * Check if the timer has completed its schedule
     * @return
     * If the timer has completed its schedule
     */
    public boolean isFinished();


    /**
     * Asynchronous Request - Start the timers counting sequence.  This will begin with the prelude and then move through the schedules to completion.
     *
     * If the timer has been paused, use start to continue.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean start();

    /**
     * Asynchronous Request - Pause the timer from counting.  This will wait at the current timer duration until a start of reset command.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean pause();

    /**
     * Asynchronous Request - Reset timer back to 0 if its not actively running.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean reset();

    /**
     * Asynchronous Request - Reset timer back to 0 regardless of its current state.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean clear();

    /**
     * Asynchronous Request - Make the timer buzz
     * @param longBuzz
     * if true, a long buzz will occur.  If false, a short buzz.
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean buzz(boolean longBuzz);

    /**
     * Asynchronous Request - Make the timer buzz for a specified duration
     * @param duration
     * the duration in milliseconds to make the timer buzz for
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean buzzRaw(int duration);

    /**
     * Asynchronous Request - Set the message to be displayed when the device is in message mode.  The message
     * cannot be more than 32 characters and can only use these characters [A-Za-z0-9.-_ and space.
     * @param message
     * the message to display
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setMessage(String message);

    /**
     * Asynchronous Request - Set the message to be displayed when the device is in message mode by specifying the raw value to control the seven segment display.  A seven segment
     * display is made of 7 separate line segments and a decimal point.  Each is given a numeric value.  To control more than one segment at a time,
     * simply add the values.
     *
     * top (a) = 4
     * top-right (b) = 8
     * bottom-right (c) = 32
     * bottom (d) = 64
     * bottom-left (e) = 128
     * top-left (f) = 2
     * middle (g) = 1
     * decimal point (dp) = 16
     *
     *
     * @param message
     * the message to display
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setMessageRaw(int[] message);

    /**
     * Asynchronous Request - A flash message appears for 3 seconds (more if it needs to scroll) and then the timer returns back to its previously display setting.
     *
     * The message cannot be more than 32 characters and can only use these characters [A-Za-z0-9.-_ and space.
     *
     * @param duration
     * the duration in seconds to show the message
     * @param message
     * the message to flash
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean flashMessage(int duration, String message);

    /**
     * Asynchronous Request - A flash message appears for 3 seconds (more if it needs to scroll) and then the timer returns back to its previously display setting.
     *
     * Set the flash message to be displayed by specifying the raw value to control the seven segment display.  A seven segment
     * display is made of 7 separate line segments and a decimal point.  Each is given a numeric value.  To control more than one segment at a time,
     * simply add the values.
     *
     * top (a) = 4
     * top-right (b) = 8
     * bottom-right (c) = 32
     * bottom (d) = 64
     * bottom-left (e) = 128
     * top-left (f) = 2
     * middle (g) = 1
     * decimal point (dp) = 16
     *
     * @param duration
     * the duration in seconds to show the message
     * @param message
     * the message to flash
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean flashMessageRaw(int duration, int[] message);

    /**
     * Asynchronous Request - A custom status appears in the status digits when the status mode is set to custom.
     *
     * The status cannot be more than 2 characters and can only use these characters [A-Za-z0-9.-_ and space.
     *
     * @param status
     * the status to display
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setCustomStatus(String status);

    /**
     * Asynchronous Request - A custom status appears in the status digits when the status mode is set to custom.
     *
     * Set the custom status by specifying the raw value to control the seven segment display.  A seven segment
     * display is made of 7 separate line segments and a decimal point.  Each is given a numeric value.  To control more than one segment at a time,
     * simply add the values.
     *
     * top (a) = 4
     * top-right (b) = 8
     * bottom-right (c) = 32
     * bottom (d) = 64
     * bottom-left (e) = 128
     * top-left (f) = 2
     * middle (g) = 1
     * decimal point (dp) = 16
     *
     * @param status
     * the status to display
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setCustomStatusRaw(int[] status);

    /**
     * Asynchronous Request - Set the schedule the clock should track.  If the clock is currently running, this will be rejected.
     *
     * Each schedule is made up of one or more intervals.  These intervals have a specific duration on which work or rest may be performed.
     * You can repeat a group of intervals multiple times using the repetition value.  You can use the restAfterInterval and restAfterRepetition values to
     * specify a rest period which is common to all intervals or repetitions.
     *
     * @param reset
     * should we reset the clock before applying this schedule.  If the clock has been started, setting the schedule without a reset will fail.
     * @param prelude
     * the prelude duration to use with the schedule
     * @param segue
     * if segue should be enabled for this schedule
     * @param continuous
     * if the total elapsed/remaining time should be shown, or if the elapsed/remaining time in the current interval should be shown
     * @param statusMode
     * the type of status to display
     * @param schedule
     * The actual work durations and rest durations along with repetition and interval schemes
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSchedule(boolean reset, int prelude, boolean segue, boolean continuous, StatusMode statusMode, TimerSchedule schedule);

    /**
     * Asynchronous Request - Set the schedule the clock should track.  If the clock is currently running, this will be rejected.
     *
     * Each schedule is made up of one or more intervals.  These intervals have a specific duration on which work or rest may be performed.
     * You can repeat a group of intervals multiple times using the repetition value.  You can use the restAfterInterval and restAfterRepetition values to
     * specify a rest period which is common to all intervals or repetitions.
     *
     * With this method, you can specify more than one schedule if necessary.
     *
     * @param reset
     * should we reset the clock before applying this schedule.  If the clock has been started, setting the schedule without a reset will fail.
     * @param prelude
     * the prelude duration to use with the schedule
     * @param segue
     * if segue should be enabled for this schedule
     * @param continuous
     * if the total elapsed/remaining time should be shown, or if the elapsed/remaining time in the current interval should be shown
     * @param statusMode
     * the type of status to display
     * @param schedules
     * The actual work durations and rest durations along with repetition and interval schemes
     * @return
     * True/false based on if the command was accepted.  This does not indicate success/failure of the command.
     */
    public boolean setSchedules(boolean reset, int prelude, boolean segue, boolean continuous, StatusMode statusMode, List<TimerSchedule> schedules);
}
