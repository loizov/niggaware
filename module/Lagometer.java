package ru.niggaware.module;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import ru.niggaware.NiggaWare;
import ru.niggaware.event.EventReceivePacket;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

public class Lagometer extends Module {
    // Хранилище для измерения TPS
    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    
    // История пинга
    private final LinkedList<Integer> pingHistory = new LinkedList<>();
    private int displayPing = 0;
    private float displayTPS = 20.0f;
    
    // Индикаторы состояния сервера
    private long lastPacketTime = 0;
    private long lastUpdateTime = 0;
    private int packetCount = 0;
    private ServerStatus serverStatus = ServerStatus.GOOD;
    
    private enum ServerStatus {
        GOOD(new Color(0, 255, 0, 200).getRGB()),
        MEDIUM(new Color(255, 255, 0, 200).getRGB()),
        BAD(new Color(255, 150, 0, 200).getRGB()),
        TERRIBLE(new Color(255, 0, 0, 200).getRGB());
        
        private final int color;
        
        ServerStatus(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
    
    public Lagometer() {
        super("Lagometer", Category.MISC);
        Arrays.fill(tickRates, 20.0f);
    }
    
    @Override
    public void onEnable() {
        timeLastTimeUpdate = -1;
        nextIndex = 0;
        Arrays.fill(tickRates, 20.0f);
        pingHistory.clear();
        lastPacketTime = System.currentTimeMillis();
        lastUpdateTime = System.currentTimeMillis();
        packetCount = 0;
        displayTPS = 20.0f;
        displayPing = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Обновляем пинг каждый тик
        updatePing();
        
        // Обновляем статус сервера
        updateServerStatus(currentTime);
        
        // Сброс счетчика пакетов каждую секунду
        if (currentTime - lastUpdateTime >= 1000) {
            packetCount = 0;
            lastUpdateTime = currentTime;
        }
    }
    
    private void updateServerStatus(long currentTime) {
        long timeSinceLastPacket = currentTime - lastPacketTime;
        
        // Если мы в одиночной игре или интегрированном сервере
        if (mc.isSingleplayer() || mc.getIntegratedServer() != null) {
            serverStatus = ServerStatus.GOOD;
            return;
        }
        
        // Для многопользовательских серверов
        if (timeSinceLastPacket > 5000) {
            serverStatus = ServerStatus.TERRIBLE;
        } else if (timeSinceLastPacket > 2000) {
            serverStatus = ServerStatus.BAD;
        } else if (timeSinceLastPacket > 1000) {
            serverStatus = ServerStatus.MEDIUM;
        } else {
            serverStatus = ServerStatus.GOOD;
        }
    }
    
    @Override
    public void onRender() {
        if (mc.player == null || mc.fontRendererObj == null) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        int x = sr.getScaledWidth() - 120;
        int y = 35;
        int width = 110;
        int height = 70;
        
        // Фон
        Gui.drawRect(x, y, x + width, y + height, new Color(0, 0, 0, 150).getRGB());
        
        // Индикатор статуса сервера (цветная полоса сверху)
        Gui.drawRect(x, y, x + width, y + 2, serverStatus.getColor());
        
        // Заголовок
        String title = "Server Status";
        mc.fontRendererObj.drawString(title, x + width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, y + 4, Color.WHITE.getRGB());
        
        // TPS
        String tpsText = "TPS: " + String.format("%.1f", displayTPS);
        mc.fontRendererObj.drawString(tpsText, x + 5, y + 20, getTpsColor(displayTPS));
        
        // Пинг
        String pingText = "Ping: " + displayPing + "ms";
        mc.fontRendererObj.drawString(pingText, x + 5, y + 32, getPingColor(displayPing));
        
        // Задержка пакетов - исправлено
        long packetDelay = getPacketDelay();
        String packetText = "Packet: " + packetDelay + "ms";
        mc.fontRendererObj.drawString(packetText, x + 5, y + 44, getPacketColor(packetDelay));
        
        // FPS
        String fpsText = "FPS: " + mc.getDebugFPS();
        mc.fontRendererObj.drawString(fpsText, x + 5, y + 56, getFpsColor(mc.getDebugFPS()));
    }
    
    private long getPacketDelay() {
        // Если в одиночной игре, возвращаем 0
        if (mc.isSingleplayer() || mc.getIntegratedServer() != null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long delay = currentTime - lastPacketTime;
        
        // Ограничиваем максимальное значение для читаемости
        return Math.min(delay, 9999);
    }
    
    // Метод для обработки входящих пакетов
    public void onPacket(EventReceivePacket event) {
        if (event.isCancelled()) return;
        
        // Обновляем время последнего пакета для всех пакетов
        lastPacketTime = System.currentTimeMillis();
        packetCount++;
        
        // Обработка пакетов времени для расчета TPS
        if (event.getPacket() instanceof SPacketTimeUpdate) {
            calculateTPS();
        }
    }
    
    private void calculateTPS() {
        long currentTime = System.currentTimeMillis();
        
        if (timeLastTimeUpdate != -1) {
            long timeDiff = currentTime - timeLastTimeUpdate;
            
            if (timeDiff > 0) {
                // SPacketTimeUpdate приходит каждую секунду (примерно каждые 1000ms)
                // Нормальный интервал - 1000ms, что соответствует 20 TPS
                float tps = 20000.0f / timeDiff;
                
                // Ограничиваем значения TPS
                tps = Math.min(20.0f, Math.max(0.0f, tps));
                
                // Обновляем массив TPS
                tickRates[nextIndex] = tps;
                nextIndex = (nextIndex + 1) % tickRates.length;
                
                // Рассчитываем средний TPS
                displayTPS = calculateAverageTPS();
            }
        }
        
        timeLastTimeUpdate = currentTime;
    }
    
    private void updatePing() {
        if (mc.getConnection() == null) {
            displayPing = 0;
            return;
        }
        
        NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
        if (info != null) {
            int currentPing = info.getResponseTime();
            
            // Для одиночной игры пинг всегда 0
            if (mc.isSingleplayer() || mc.getIntegratedServer() != null) {
                currentPing = 0;
            }
            
            // Добавляем в историю
            pingHistory.add(currentPing);
            
            // Ограничиваем размер истории
            while (pingHistory.size() > 10) {
                pingHistory.removeFirst();
            }
            
            // Рассчитываем средний пинг
            if (!pingHistory.isEmpty()) {
                int sum = 0;
                for (Integer ping : pingHistory) {
                    sum += ping;
                }
                displayPing = sum / pingHistory.size();
            }
        }
    }
    
    private float calculateAverageTPS() {
        float sum = 0.0f;
        int count = 0;
        
        for (float rate : tickRates) {
            if (rate > 0) {
                sum += rate;
                count++;
            }
        }
        
        return count > 0 ? sum / count : 20.0f;
    }
    
    private int getTpsColor(float tps) {
        if (tps >= 19.0f) return new Color(0, 255, 0).getRGB();
        if (tps >= 15.0f) return new Color(255, 255, 0).getRGB();
        if (tps >= 10.0f) return new Color(255, 150, 0).getRGB();
        return new Color(255, 0, 0).getRGB();
    }
    
    private int getPingColor(int ping) {
        if (ping < 60) return new Color(0, 255, 0).getRGB();
        if (ping < 120) return new Color(255, 255, 0).getRGB();
        if (ping < 200) return new Color(255, 150, 0).getRGB();
        return new Color(255, 0, 0).getRGB();
    }
    
    private int getPacketColor(long delay) {
        if (delay < 100) return new Color(0, 255, 0).getRGB();
        if (delay < 500) return new Color(255, 255, 0).getRGB();
        if (delay < 1500) return new Color(255, 150, 0).getRGB();
        return new Color(255, 0, 0).getRGB();
    }
    
    private int getFpsColor(int fps) {
        if (fps > 120) return new Color(0, 255, 0).getRGB();
        if (fps > 60) return new Color(255, 255, 0).getRGB();
        if (fps > 30) return new Color(255, 150, 0).getRGB();
        return new Color(255, 0, 0).getRGB();
    }
    
    // Этот метод должен вызываться из главного класса при приеме пакетов
    public static void handlePacket(EventReceivePacket event) {
        Lagometer module = (Lagometer) NiggaWare.instance.moduleManager.getModuleByName("Lagometer");
        if (module != null && module.isEnabled()) {
            module.onPacket(event);
        }
    }
}