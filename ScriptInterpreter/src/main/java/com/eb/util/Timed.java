package com.eb.util;


import com.eb.util.string.UtilString;

/**
 * Utility class for timing execution speeds and measuring elapsed time.
 * <p>
 * This class provides a simple stopwatch-like timer that can be started, stopped,
 * and continued. It tracks both the total elapsed time and the time since the last
 * continuation point.
 * </p>
 *
 * @author Earl
 */
public final class Timed {

    private boolean running;
    private long timerStart;
    private long timerStop;
    private long timerPrevStop;

    /**
     * Constructs a new timer instance in a stopped state.
     * The timer is initialized to the current time but not started.
     */
    public Timed() {
        timerReset();
        running = false;
    }

    /**
     * Resets the timer to the current time without starting it.
     * All time markers (start, stop, and previous stop) are set to the current time.
     */
    public void timerReset() {
        long currentTime = System.currentTimeMillis();
        timerStart = currentTime;
        timerStop = currentTime;
        timerPrevStop = currentTime;
    }

    /**
     * Starts (or restarts) the timer from the current time.
     * This resets all time markers to the current time and sets the timer to running.
     */
    public void timerStart() {
        running = true;
        timerReset();
    }

    /**
     * Continues the timer after a stop, marking a new continuation point.
     * This updates the previous stop marker and sets the timer to running.
     * The total elapsed time from the original start is preserved.
     */
    public void timerContinue() {
        timerPrevStop = timerStop;
        running = true;
    }

    /**
     * Stops the timer and returns the total elapsed time in milliseconds.
     *
     * @return the total time elapsed since the timer was started, in milliseconds
     */
    public long timerStop() {
        running = false;
        long currentTime = System.currentTimeMillis();
        timerPrevStop = timerStop;
        timerStop = currentTime;
        return timerStop - timerStart;
    }

    /**
     * Sets the timer start position manually.
     * <p>
     * <strong>Warning:</strong> This method should be used with caution as it can
     * break the internal consistency of the timer.
     * </p>
     *
     * @param pTimer the new start time in milliseconds since epoch
     */
    public void setTimerStart(long pTimer) {
        timerStart = pTimer;
    }

    /**
     * Gets the timer start position.
     *
     * @return the start time in milliseconds since epoch
     */
    public long getTimerStart() {
        return timerStart;
    }

    /**
     * Gets the timer stop position.
     *
     * @return the stop time in milliseconds since epoch
     */
    public long getTimerStop() {
        return timerStop;
    }

    /**
     * Checks if the timer is currently running.
     *
     * @return true if the timer is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the total elapsed time from the timer start.
     * If the timer is running, returns the time elapsed up to now.
     * If stopped, returns the time elapsed when it was stopped.
     *
     * @return the elapsed time in milliseconds
     */
    public long getTimerPeriod() {
        if (running) {
            long currentTime = System.currentTimeMillis();
            return currentTime - timerStart;
        } else {
            return timerStop - timerStart;
        }
    }

    /**
     * Gets the elapsed time since the last continuation point.
     * If the timer is running, returns the time elapsed since the last continue call.
     * If stopped, returns the time between the last continue and stop.
     *
     * @return the elapsed time since last continuation, in milliseconds
     */
    public long getContinuePeriod() {
        if (running) {
            long currentTime = System.currentTimeMillis();
            return currentTime - timerPrevStop;
        } else {
            return timerStop - timerPrevStop;
        }
    }

    /**
     * Gets the total elapsed time as a string in milliseconds.
     *
     * @return the elapsed time in milliseconds as a string
     */
    public String getTimerString_milliseconds() {
        return String.valueOf(getTimerPeriod());
    }

    /**
     * Gets the time since last continuation as a string in milliseconds.
     *
     * @return the elapsed time since continuation in milliseconds as a string
     */
    public String getContinueString_milliseconds() {
        return String.valueOf(getContinuePeriod());
    }

    /**
     * Gets the total elapsed time as a string in seconds with milliseconds.
     * Format: "seconds.milliseconds" (e.g., "5.123")
     *
     * @return the elapsed time in seconds as a string
     */
    public String getTimerString_Seconds() {
        long period = getTimerPeriod();
        return String.format("%d.%03d", period / 1000, period % 1000);
    }

    /**
     * Gets the time since last continuation as a string in seconds with milliseconds.
     * Format: "seconds.milliseconds" (e.g., "5.123")
     *
     * @return the elapsed time since continuation in seconds as a string
     */
    public String getContinueString_Seconds() {
        long period = getContinuePeriod();
        return String.format("%d.%03d", period / 1000, period % 1000);
    }

    /**
     * Gets the total elapsed time as a string in seconds with specified decimal precision.
     * Format: "seconds.xxx" where xxx has pDecimals digits
     *
     * @param pDecimals number of decimal places (0-3)
     * @return the elapsed time in seconds as a string with specified precision
     * @throws IllegalArgumentException if pDecimals is negative or greater than 3
     */
    public String getTimerString_Seconds(int pDecimals) {
        if (pDecimals < 0 || pDecimals > 3) {
            throw new IllegalArgumentException("pDecimals must be between 0 and 3, got: " + pDecimals);
        }
        long period = getTimerPeriod();
        if (pDecimals == 0) {
            return String.valueOf(period / 1000);
        }
        String millisPart = UtilString.lpad(String.valueOf(period % 1000), 3, '0');
        return period / 1000 + "." + millisPart.substring(0, pDecimals);
    }

    /**
     * Gets the time since last continuation as a string in seconds with specified decimal precision.
     * Format: "seconds.xxx" where xxx has pDecimals digits
     *
     * @param pDecimals number of decimal places (0-3)
     * @return the elapsed time since continuation in seconds as a string with specified precision
     * @throws IllegalArgumentException if pDecimals is negative or greater than 3
     */
    public String getContinueString_Seconds(int pDecimals) {
        if (pDecimals < 0 || pDecimals > 3) {
            throw new IllegalArgumentException("pDecimals must be between 0 and 3, got: " + pDecimals);
        }
        long period = getContinuePeriod();
        if (pDecimals == 0) {
            return String.valueOf(period / 1000);
        }
        String millisPart = UtilString.lpad(String.valueOf(period % 1000), 3, '0');
        return period / 1000 + "." + millisPart.substring(0, pDecimals);
    }

}
