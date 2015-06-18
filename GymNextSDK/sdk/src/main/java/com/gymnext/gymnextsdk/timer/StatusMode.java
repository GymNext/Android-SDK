package com.gymnext.gymnextsdk.timer;

/**
 * The left most digits of the timer display the status.  The status can vary depending on what type of a schedule
 * you are running.
 */
public enum StatusMode {
    /**
     *  No status displayed
     */
    None,
    /**
     * Display the current interval number for the status
     */
    Interval,
    /**
     * Display the current repetition (aka. round) number for the status
     */
    Repetition,
    /**
     * Display a completely custom status that you control
     */
    Custom
}
