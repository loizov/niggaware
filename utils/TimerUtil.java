package ru.niggaware.utils;

public class TimerUtil {
    private long lastMS = 0L;
    
    public TimerUtil() {
        reset();
    }
    
    public void reset() {
        lastMS = System.currentTimeMillis();
    }
    
    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS > time) {
            if (reset) reset();
            return true;
        }
        return false;
    }
    
    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }
    
    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }
    
    public void setTime(long time) {
        lastMS = time;
    }
}