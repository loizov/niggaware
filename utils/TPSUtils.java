package ru.niggaware.utils;

import java.util.LinkedList;

public class TPSUtils {
    private static final int MAX_SAMPLES = 20 * 20; // 20 секунд при 20 тиках
    private final LinkedList<Long> tickTimes = new LinkedList<>();
    private long lastTickTime = -1;
    
    public void update() {
        long now = System.currentTimeMillis();
        
        if (lastTickTime != -1) {
            long elapsed = now - lastTickTime;
            tickTimes.add(elapsed);
            
            while (tickTimes.size() > MAX_SAMPLES) {
                tickTimes.removeFirst();
            }
        }
        
        lastTickTime = now;
    }
    
    // Вызывать это из метода onTick() NiggaWare
    public void tick() {
        update();
    }
    
    // Возвращает текущий TPS (тиков в секунду)
    public float getTPS() {
        if (tickTimes.isEmpty()) return 20.0F;
        
        long sum = 0;
        for (long time : tickTimes) {
            sum += time;
        }
        
        float avgMsPerTick = (float)sum / tickTimes.size();
        float tps = 1000.0F / avgMsPerTick;
        
        // Ограничим значения от 0 до 20
        return Math.min(20.0F, Math.max(0.0F, tps));
    }
}