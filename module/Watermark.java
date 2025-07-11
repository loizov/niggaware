package ru.niggaware.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;
import ru.niggaware.utils.RenderUtils;
import ru.niggaware.utils.AnimationUtils;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Watermark extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastFpsUpdate = 0;
    private int currentFps = 0;
    
    // Анимация
    private AnimationUtils.Animation fadeAnimation;
    
    // Цвета
    private final Color backgroundColor = new Color(0, 0, 0, 80);
    private final Color accentColor = new Color(88, 166, 255);
    private final Color textColor = new Color(255, 255, 255);
    private final Color dimTextColor = new Color(180, 180, 180);

    public Watermark() {
        super("Watermark", Category.HUD);
        initAnimations();
    }
    
    private void initAnimations() {
        fadeAnimation = new AnimationUtils.Animation(0f, 0.12f);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        fadeAnimation.setTarget(1f);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        fadeAnimation.setTarget(0f);
    }

    @Override
    public void onRender() {
        if (mc.ingameGUI == null || mc.fontRendererObj == null || mc.player == null) return;

        // Обновляем анимацию
        fadeAnimation.update();
        float alpha = fadeAnimation.getCurrent();
        if (alpha < 0.01f) return;

        // Обновляем FPS
        if (System.currentTimeMillis() - lastFpsUpdate > 500) {
            currentFps = Minecraft.getDebugFPS();
            lastFpsUpdate = System.currentTimeMillis();
        }

        // Время
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());

        // Ник игрока
        String playerName = mc.player.getName();

        FontRenderer fr = mc.fontRendererObj;
        
        // Рассчитываем размеры
        String titleText = "NiggaWare";
        String fpsText = currentFps + " fps";
        
        // Размеры блока
        int titleWidth = fr.getStringWidth(titleText + " • " + playerName) + 16;
        int infoWidth = fr.getStringWidth(fpsText + " • " + currentTime) + 16;
        int maxWidth = Math.max(titleWidth, infoWidth);
        
        int x = 8;
        int y = 8;
        int padding = 8;
        
        // Основной блок
        int blockHeight = 38;
        
        // Фон с анимацией
        int bgAlpha = (int)(backgroundColor.getAlpha() * alpha);
        RenderUtils.drawRect(x, y, x + maxWidth, y + blockHeight, 
            new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), bgAlpha).getRGB());
        
        // Тонкая акцентная линия сверху
        int accentAlpha = (int)(255 * alpha);
        RenderUtils.drawRect(x, y, x + maxWidth, y + 1, 
            new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), accentAlpha).getRGB());
        
        // Текст с анимацией
        int textAlpha = (int)(255 * alpha);
        int dimAlpha = (int)(180 * alpha);
        
        // Первая строка: NiggaWare • playerName
        int firstLineY = y + padding;
        
        // Название клиента
        fr.drawString(titleText, x + padding, firstLineY, 
            new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), textAlpha).getRGB());
        
        // Разделитель
        int sepX = x + padding + fr.getStringWidth(titleText) + 4;
        fr.drawString("•", sepX, firstLineY, 
            new Color(dimTextColor.getRed(), dimTextColor.getGreen(), dimTextColor.getBlue(), dimAlpha).getRGB());
        
        // Ник игрока
        int nameX = sepX + fr.getStringWidth("• ");
        fr.drawString(playerName, nameX, firstLineY, 
            new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textAlpha).getRGB());
        
        // Вторая строка: FPS • время
        int secondLineY = firstLineY + 12;
        
        // FPS
        fr.drawString(fpsText, x + padding, secondLineY, getFpsColor(currentFps, alpha));
        
        // Разделитель
        int sepX2 = x + padding + fr.getStringWidth(fpsText) + 4;
        fr.drawString("•", sepX2, secondLineY, 
            new Color(dimTextColor.getRed(), dimTextColor.getGreen(), dimTextColor.getBlue(), dimAlpha).getRGB());
        
        // Время
        int timeX = sepX2 + fr.getStringWidth("• ");
        fr.drawString(currentTime, timeX, secondLineY, 
            new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textAlpha).getRGB());
    }
    
    // Цвет FPS
    private int getFpsColor(int fps, float alpha) {
        Color baseColor;
        if (fps >= 100) {
            baseColor = new Color(100, 255, 100);    // Светло-зеленый
        } else if (fps >= 60) {
            baseColor = new Color(255, 255, 100);    // Светло-желтый
        } else {
            baseColor = new Color(255, 100, 100);    // Светло-красный
        }
        
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(255 * alpha)).getRGB();
    }
}