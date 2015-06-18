package com.gymnext.gymnextsdk.timer;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the definition of the schedule that the timer will track.  Depending on the type of
 * program you are tracking, whether it be a simple count-up/count-down or a more elaborate interval
 * based program, the timer schedule class is how you define them.  For many programs, there are
 * more than one way to define them, so use whatever is the least verbose.
 *
 * For example, if you have a scheme of 4 work intervals of 1:00 each followed by 0:30 of rest and
 * repeated 3 times, you could define that by specifying 15 intervals, but the better and less verbose
 * approach would be to have 4 intervals of 1:00, with 3 repetition and with 0:30 rest between repetitions.
 */
public class TimerSchedule {

    /**
     * Is this interval a rest or work interval
     */
    public static enum TimerIntervalType {
        Work,
        Rest
    }

    /**
     * The duration and type of interval
     */
    public static class TimerInterval {
        public int duration;
        public TimerIntervalType intervalType;

        public TimerInterval() {
            super();
        }

        public TimerInterval(int duration, TimerIntervalType intervalType) {
            this.duration = duration;
            this.intervalType = intervalType;
        }
    }

    /**
     * All the intervals of the schedule (Rest and Work)
     */
    public List<TimerInterval> intervals = new ArrayList<TimerInterval>();

    /**
     * Short cut for a common rest after each interval
     */
    public int restBetweenIntervals = 0;

    /**
     * Short cut for repeating the same interval/rest between interval schemes
     */
    public int numberOfRepetitions = 1;

    /**
     * Short cut for a common rest between repetitions
     */
    public int restBetweenRepetitions = 0;

    public TimerSchedule() {
        super();
    }

    public TimerSchedule(List<TimerInterval> intervals, int restBetweenIntervals, int numberOfRepetitions, int restBetweenRepetitions) {
        this.intervals = intervals;
        this.restBetweenIntervals = restBetweenIntervals;
        this.numberOfRepetitions = numberOfRepetitions;
        this.restBetweenRepetitions = restBetweenRepetitions;
    }

}

