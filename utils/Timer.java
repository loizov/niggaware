package ru.niggaware.utils;

public class Timer {
    private long lastMS = 0L;
    
    /**
     * Сбрасывает таймер, устанавливая текущее время
     */
    public void reset() {
        lastMS = System.currentTimeMillis();
    }
    
    /**
     * Получает время, прошедшее с последнего сброса
     * 
     * @return Время в миллисекундах с момента последнего сброса
     */
    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }
    
    /**
     * Проверяет, прошло ли указанное время с момента последнего сброса
     * 
     * @param time Время для проверки в миллисекундах
     * @return true, если указанное время прошло
     */
    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS >= time;
    }
    
    /**
     * Проверяет, прошло ли указанное время с момента последнего сброса,
     * и сбрасывает таймер, если время прошло
     * 
     * @param time Время для проверки в миллисекундах
     * @param reset Сбросить таймер, если время прошло
     * @return true, если указанное время прошло
     */
    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS >= time) {
            if (reset) reset();
            return true;
        }
        return false;
    }
    
    /**
     * Задерживает выполнение на указанное количество миллисекунд
     * 
     * @param ms Время задержки в миллисекундах
     */
    public void delay(long ms) {
        lastMS = System.currentTimeMillis() + ms;
    }
    
    /**
     * Проверяет, истекла ли задержка, установленная методом delay()
     * 
     * @return true, если задержка истекла
     */
    public boolean isDelayComplete() {
        return System.currentTimeMillis() >= lastMS;
    }
}