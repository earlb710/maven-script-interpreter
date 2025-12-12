package com.eb.util;


import com.eb.util.string.UtilString;

/**
 * Generic utility timer methods. Note : methods to time execution speeds.
 *
 * @author Earl
 *
 */

public final class Timed {

    private boolean running;
    private long timerStart;
    private long timerStop;
    private long timerPrevStop;

    /**
     * UtilTimer Constructor Starts a new timer
     */
    public Timed() {
        timerReset();
        running = false;
    }

    /**
     * Reset timer period to 0 and starts timer from current time
     *
     */
    public void timerReset() {
        timerStart = System.currentTimeMillis();
        timerStop = timerStart;
        timerPrevStop = timerStop;
    }

    /**
     * Start timer from current time
     *
     */
    public void timerStart() {
        running = true;
        timerReset();
    }

    public void timerContinue() {
        timerPrevStop = timerStop;
        timerStop = System.currentTimeMillis();
        running = true;
    }

    /**
     * Stop timer and return total period so far
     *
     * @return period
     */
    public long timerStop() {
        running = false;
        long newTimer = System.currentTimeMillis();
        timerPrevStop = timerStop;
        timerStop = newTimer;
        if (timerStart != 0) {
            return (timerStop - timerStart);
        } else {
            timerStop = 0L;
            return 0L;
        }
    }

    /**
     * Set timer start position
     *
     */
    public void setTimerStart(long pTimer) {
        timerStart = pTimer;
    }

    /**
     * Returns current timer start position
     *
     */
    public long getTimerStart() {
        return timerStart;
    }

    public long getTimerStop() {
        return timerStop;
    }

    public boolean isRunning() {
        return running;
    }

    public long getTimerPeriod() {
        if (running) {
            long newTimer = System.currentTimeMillis();
            return (newTimer - timerStart);
        } else {
            return (timerStop - timerStart);
        }
    }

    /**
     * Returns current timer period in milliseconds
     *
     */
    public long getContinuePeriod() {
        if (running) {
            long newTimer = System.currentTimeMillis();
            return (newTimer - timerPrevStop);
        } else {
            return (timerStop - timerPrevStop);
        }
    }

    /**
     * Returns current timer period in milliseconds as a String
     *
     */
    public String getTimerString_milliseconds() {
        return String.valueOf(getTimerPeriod());
    }

    public String getContinueString_milliseconds() {
        return String.valueOf(getContinuePeriod());
    }

    /**
     * Returns current timer period in seconds as a String
     *
     */
    public String getTimerString_Seconds() {
        return String.valueOf(getTimerPeriod() / 1000) + "." + String.valueOf(getTimerPeriod() % 1000);
    }

    public String getContinueString_Seconds() {
        return String.valueOf(getContinuePeriod() / 1000) + "." + String.valueOf(getContinuePeriod() % 1000);
    }

    /**
     * Returns current timer period in seconds as a String
     *
     * @param pDecimals Number of decimal spaces - max 6.
     */
    public String getTimerString_Seconds(int pDecimals) {
        return String.valueOf(getTimerPeriod() / 1000) + "." + UtilString.lpad(String.valueOf(getTimerPeriod() % 1000), 3, '0').substring(0, pDecimals);
    }

    public String getContinueString_Seconds(int pDecimals) {
        return String.valueOf(getContinuePeriod() / 1000) + "." + UtilString.lpad(String.valueOf(getContinuePeriod() % 1000), 3, '0').substring(0, pDecimals);
    }

}
