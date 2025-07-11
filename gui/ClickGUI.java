package ru.niggaware.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import ru.niggaware.NiggaWare;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;
import ru.niggaware.utils.AnimationUtils;
import ru.niggaware.utils.PastebinReader;
import ru.niggaware.utils.RenderUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUI extends GuiScreen {
    private float guiX, guiY, guiWidth, guiHeight;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private boolean resizing = false;
    private int resizeOffsetX, resizeOffsetY;
    private boolean closing = false;
    
    // Плавное перемещение и изменение размера
    private AnimationUtils.Animation guiXAnimation;
    private AnimationUtils.Animation guiYAnimation;
    private AnimationUtils.Animation guiWidthAnimation;
    private AnimationUtils.Animation guiHeightAnimation;
    private float targetX, targetY, targetWidth, targetHeight;
    
    // Двойной клик для центрирования
    private long lastClickTime = 0;
    private int lastClickX = 0;
    private int lastClickY = 0;
    private static final long DOUBLE_CLICK_TIME = 500; // 500ms для двойного клика
    private static final int DOUBLE_CLICK_DISTANCE = 10; // максимальное расстояние между кликами
    
    // Сохранение состояния GUI
    private static Category savedCategory = Category.COMBAT;
    private static int savedTab = 0;
    private static int savedModuleScroll = 0;
    private static int savedNewsScroll = 0;
    private static float savedX = -1;
    private static float savedY = -1;
    private static float savedWidth = 520;
    private static float savedHeight = 320;
    
    private final int minWidth = 480;
    private final int minHeight = 280;
    private final int categoryWidth = 140;
    
    // Улучшенная цветовая схема
    private final Color backgroundColor = new Color(18, 18, 23, 250);
    private final Color panelColor = new Color(25, 25, 30, 255);
    private final Color panelHoverColor = new Color(30, 30, 35, 255);
    private final Color accentColor = new Color(88, 166, 255);
    private final Color accentHoverColor = new Color(108, 186, 255);
    private final Color textColor = new Color(235, 235, 240);
    private final Color secondaryTextColor = new Color(160, 160, 170);
    private final Color successColor = new Color(82, 196, 26);
    private final Color dangerColor = new Color(255, 77, 79);
    
    // Структура GUI
    private Category selectedCategory = savedCategory;
    private int activeTab = savedTab;
    private final String[] tabs = {"Modules", "News", "Settings"};
    private String newsContent = "Loading news...";
    
    // Скролл и анимации
    private int moduleScroll = savedModuleScroll;
    private int maxModuleScroll = 0;
    private int newsScroll = savedNewsScroll;
    private int maxNewsScroll = 0;
    
    // Анимации
    private final Map<Category, AnimationUtils.Animation> categoryAnimations = new HashMap<>();
    private final Map<Module, AnimationUtils.Animation> moduleAnimations = new HashMap<>();
    private final Map<Integer, AnimationUtils.Animation> tabAnimations = new HashMap<>();
    private AnimationUtils.Animation guiAnimation;
    
    // Описания модулей
    private final Map<String, String> moduleDescriptions = new HashMap<>();
    
    public ClickGUI() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        
        // Восстанавливаем сохраненные позицию и размер
        if (savedX != -1 && savedY != -1) {
            this.guiX = savedX;
            this.guiY = savedY;
            this.guiWidth = savedWidth;
            this.guiHeight = savedHeight;
        } else {
            this.guiWidth = savedWidth;
            this.guiHeight = savedHeight;
            this.guiX = sr.getScaledWidth() / 2 - guiWidth / 2;
            this.guiY = sr.getScaledHeight() / 2 - guiHeight / 2;
        }
        
        // Инициализация целевых позиций
        this.targetX = guiX;
        this.targetY = guiY;
        this.targetWidth = guiWidth;
        this.targetHeight = guiHeight;
        
        // Инициализация анимаций
        initAnimations();
        
        // Инициализация описаний модулей
        initModuleDescriptions();
        
        // Загружаем новости асинхронно
        loadNews();
    }
    
    private void initAnimations() {
        guiAnimation = new AnimationUtils.Animation(0f, 0.12f);
        guiAnimation.setTarget(1f);
        
        // Анимации для плавного перемещения и изменения размера
        guiXAnimation = new AnimationUtils.Animation(guiX, 0.08f);
        guiYAnimation = new AnimationUtils.Animation(guiY, 0.08f);
        guiWidthAnimation = new AnimationUtils.Animation(guiWidth, 0.1f);
        guiHeightAnimation = new AnimationUtils.Animation(guiHeight, 0.1f);
        
        for (Category category : Category.values()) {
            categoryAnimations.put(category, new AnimationUtils.Animation(0f, 0.18f));
        }
        
        for (Module module : NiggaWare.instance.moduleManager.getModules()) {
            moduleAnimations.put(module, new AnimationUtils.Animation(0f, 0.15f));
        }
        
        for (int i = 0; i < tabs.length; i++) {
            tabAnimations.put(i, new AnimationUtils.Animation(0f, 0.2f));
        }
    }
    
    private void initModuleDescriptions() {
        moduleDescriptions.put("KillAura", "Automatically attacks nearby entities");
        moduleDescriptions.put("Fly", "Allows you to fly in survival mode");
        moduleDescriptions.put("Speed", "Increases your movement speed");
        moduleDescriptions.put("NoFall", "Prevents fall damage");
        moduleDescriptions.put("Jesus", "Walk on water like Jesus");
        moduleDescriptions.put("Scaffold", "Automatically places blocks under you");
        moduleDescriptions.put("FullBright", "Increases brightness to maximum");
        moduleDescriptions.put("ESP", "Shows players through walls");
        moduleDescriptions.put("Tracers", "Draws lines to players");
        moduleDescriptions.put("AutoArmor", "Automatically equips best armor");
        moduleDescriptions.put("ChestStealer", "Automatically steals from chests");
        moduleDescriptions.put("Timer", "Changes game speed");
        moduleDescriptions.put("Blink", "Freezes your position on server");
        moduleDescriptions.put("Phase", "Walk through blocks");
        moduleDescriptions.put("Freecam", "Spectator-like camera mode");
    }
    
    private void loadNews() {
        new Thread(() -> {
            try {
                String content = PastebinReader.readRawPaste("https://pastebin.com/raw/f77Wd4fg");
                newsContent = content != null ? content : "Failed to load news.";
            } catch (Exception e) {
                newsContent = "Error loading news: " + e.getMessage();
            }
        }).start();
    }
    
    private void saveState() {
        savedCategory = selectedCategory;
        savedTab = activeTab;
        savedModuleScroll = moduleScroll;
        savedNewsScroll = newsScroll;
        savedX = guiX;
        savedY = guiY;
        savedWidth = guiWidth;
        savedHeight = guiHeight;
    }
    
    private void centerGUI() {
        ScaledResolution sr = new ScaledResolution(mc);
        targetX = sr.getScaledWidth() / 2 - guiWidth / 2;
        targetY = sr.getScaledHeight() / 2 - guiHeight / 2;
        
        // Обновляем цели анимации для плавного перемещения в центр
        guiXAnimation.setTarget(targetX);
        guiYAnimation.setTarget(targetY);
    }
    
    private boolean isDoubleClick(int mouseX, int mouseY) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastClickTime;
        
        if (timeDiff <= DOUBLE_CLICK_TIME) {
            // Проверяем расстояние между кликами
            int distanceX = Math.abs(mouseX - lastClickX);
            int distanceY = Math.abs(mouseY - lastClickY);
            
            if (distanceX <= DOUBLE_CLICK_DISTANCE && distanceY <= DOUBLE_CLICK_DISTANCE) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateAnimations();
        
        float animationScale = guiAnimation.getCurrent();
        
        // Если анимация закрытия завершена, закрываем GUI
        if (closing && animationScale <= 0.05f) {
            mc.displayGuiScreen(null);
            return;
        }
        
        // Обновляем позицию и размер GUI с анимацией
        guiX = guiXAnimation.getCurrent();
        guiY = guiYAnimation.getCurrent();
        guiWidth = guiWidthAnimation.getCurrent();
        guiHeight = guiHeightAnimation.getCurrent();
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiX + guiWidth / 2, guiY + guiHeight / 2, 0);
        GlStateManager.scale(animationScale, animationScale, 1);
        GlStateManager.translate(-(guiX + guiWidth / 2), -(guiY + guiHeight / 2), 0);
        
        // Тень
        RenderUtils.drawShadow(guiX, guiY, guiWidth, guiHeight, backgroundColor.getRGB());
        
        // Основной фон
        RenderUtils.drawRect(guiX, guiY, guiX + guiWidth, guiY + guiHeight, backgroundColor.getRGB());
        
        // Верхняя панель с градиентом
        RenderUtils.drawGradientRect(guiX, guiY, guiWidth, 40, 
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 200).getRGB(),
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100).getRGB());
        
        // Название с улучшенным шрифтом
        FontRenderer fr = mc.fontRendererObj;
        fr.drawStringWithShadow("NiggaWare", (int)guiX + 15, (int)guiY + 15, Color.WHITE.getRGB());
        
        // Версия
        fr.drawString("v2.0", (int)guiX + (int)guiWidth - 30, (int)guiY + 15, secondaryTextColor.getRGB());
        
        // Вкладки
        drawTabs(mouseX, mouseY);
        
        // Левая панель с категориями
        drawCategoryPanel(mouseX, mouseY);
        
        // Контент в зависимости от выбранной вкладки
        switch (activeTab) {
            case 0:
                drawModulesTab(mouseX, mouseY);
                break;
            case 1:
                drawNewsTab();
                break;
            case 2:
                drawSettingsTab(mouseX, mouseY);
                break;
        }
        
        // Индикатор ресайза
        drawResizeIndicator(mouseX, mouseY);
        
        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void updateAnimations() {
        guiAnimation.update();
        guiXAnimation.update();
        guiYAnimation.update();
        guiWidthAnimation.update();
        guiHeightAnimation.update();
        
        categoryAnimations.values().forEach(AnimationUtils.Animation::update);
        moduleAnimations.values().forEach(AnimationUtils.Animation::update);
        tabAnimations.values().forEach(AnimationUtils.Animation::update);
        
        // Обновляем целевые значения для категорий
        for (Category category : Category.values()) {
            AnimationUtils.Animation animation = categoryAnimations.get(category);
            animation.setTarget(category == selectedCategory ? 1f : 0f);
        }
        
        // Обновляем целевые значения для модулей
        for (Module module : NiggaWare.instance.moduleManager.getModules()) {
            AnimationUtils.Animation animation = moduleAnimations.get(module);
            animation.setTarget(module.isEnabled() ? 1f : 0f);
        }
        
        // Обновляем целевые значения для вкладок
        for (int i = 0; i < tabs.length; i++) {
            AnimationUtils.Animation animation = tabAnimations.get(i);
            animation.setTarget(i == activeTab ? 1f : 0f);
        }
    }
    
    private void drawTabs(int mouseX, int mouseY) {
        int tabWidth = 90;
        int tabHeight = 25;
        int tabX = (int) (guiX + categoryWidth + 20);
        int tabY = (int) (guiY + 8);
        
        for (int i = 0; i < tabs.length; i++) {
            boolean isActive = activeTab == i;
            boolean isHovered = RenderUtils.isHovered(mouseX, mouseY, tabX + i * (tabWidth + 5), tabY, tabWidth, tabHeight);
            
            AnimationUtils.Animation animation = tabAnimations.get(i);
            float progress = animation.getCurrent();
            
            Color tabColor = isActive ? accentColor : (isHovered ? panelHoverColor : panelColor);
            Color textColor = isActive ? Color.WHITE : (isHovered ? this.textColor : secondaryTextColor);
            
            // Фон вкладки
            RenderUtils.drawRect(tabX + i * (tabWidth + 5), tabY, tabX + i * (tabWidth + 5) + tabWidth, tabY + tabHeight, tabColor.getRGB());
            
            // Индикатор активной вкладки
            if (progress > 0) {
                RenderUtils.drawRect(tabX + i * (tabWidth + 5), tabY + tabHeight - 2, 
                        tabX + i * (tabWidth + 5) + tabWidth * progress, tabY + tabHeight, accentColor.getRGB());
            }
            
            // Текст
            FontRenderer fr = mc.fontRendererObj;
            int textX = tabX + i * (tabWidth + 5) + tabWidth / 2 - fr.getStringWidth(tabs[i]) / 2;
            int textY = tabY + tabHeight / 2 - fr.FONT_HEIGHT / 2;
            fr.drawString(tabs[i], textX, textY, textColor.getRGB());
        }
    }
    
    private void drawCategoryPanel(int mouseX, int mouseY) {
        // Панель категорий
        RenderUtils.drawRect(guiX + 8, guiY + 50, guiX + categoryWidth, guiY + guiHeight - 10, panelColor.getRGB());
        
        FontRenderer fr = mc.fontRendererObj;
        int categoryY = (int) (guiY + 60);
        int categoryHeight = 32;
        
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = RenderUtils.isHovered(mouseX, mouseY, guiX + 8, categoryY, categoryWidth - 8, categoryHeight);
            
            AnimationUtils.Animation animation = categoryAnimations.get(category);
            float progress = animation.getCurrent();
            
            Color bgColor = isSelected ? accentColor : (isHovered ? panelHoverColor : panelColor);
            Color textColor = isSelected ? Color.WHITE : (isHovered ? this.textColor : secondaryTextColor);
            
            // Фон категории
            RenderUtils.drawRect(guiX + 12, categoryY, guiX + categoryWidth - 4, categoryY + categoryHeight - 2, bgColor.getRGB());
            
            // Индикатор выбранной категории
            if (progress > 0) {
                RenderUtils.drawRect(guiX + 12, categoryY, guiX + 12 + 3 * progress, categoryY + categoryHeight - 2, Color.WHITE.getRGB());
            }
            
            // Иконка категории
            String icon = getCategoryIcon(category);
            fr.drawString(icon, (int) (guiX + 20), categoryY + 8, textColor.getRGB());
            
            // Название категории
            fr.drawString(category.name(), (int) (guiX + 35), categoryY + 8, textColor.getRGB());
            
            // Количество модулей в категории
            int moduleCount = (int) NiggaWare.instance.moduleManager.getModules().stream()
                    .filter(m -> m.getCategory() == category)
                    .count();
            String countStr = String.valueOf(moduleCount);
            fr.drawString(countStr, (int) (guiX + categoryWidth - 25), categoryY + 8, secondaryTextColor.getRGB());
            
            categoryY += categoryHeight;
        }
    }
    
    private String getCategoryIcon(Category category) {
        switch (category) {
            case COMBAT: return "⚔";
            case MOVEMENT: return "⚡";
            case PLAYER: return "👤";
            case RENDER: return "👁";
            case MISC: return "⚙";
            default: return "?";
        }
    }
    
    private void drawModulesTab(int mouseX, int mouseY) {
        // Получаем модули для выбранной категории
        List<Module> modules = new ArrayList<>();
        for (Module module : NiggaWare.instance.moduleManager.getModules()) {
            if (module.getCategory() == selectedCategory) {
                modules.add(module);
            }
        }
        
        FontRenderer fr = mc.fontRendererObj;
        int contentX = (int) (guiX + categoryWidth + 20);
        int contentY = (int) (guiY + 50);
        int contentWidth = (int) (guiWidth - categoryWidth - 30);
        int contentHeight = (int) (guiHeight - 60);
        
        // Контейнер для модулей
        RenderUtils.drawRect(contentX, contentY, contentX + contentWidth, contentY + contentHeight, panelColor.getRGB());
        
        // Заголовок
        fr.drawStringWithShadow(selectedCategory.name() + " MODULES", contentX + 15, contentY + 15, textColor.getRGB());
        
        // Модули
        int moduleY = contentY + 40 - moduleScroll;
        int moduleHeight = 40;
        
        // Рассчитываем максимальный скролл
        int visibleHeight = contentHeight - 50;
        int totalContentHeight = modules.size() * moduleHeight;
        maxModuleScroll = Math.max(0, totalContentHeight - visibleHeight);
        
        for (Module module : modules) {
            if (moduleY + moduleHeight < contentY + 40 || moduleY > contentY + contentHeight) {
                moduleY += moduleHeight;
                continue;
            }
            
            drawModuleCard(module, contentX + 10, moduleY, contentWidth - 20, moduleHeight - 5, mouseX, mouseY);
            moduleY += moduleHeight;
        }
        
        // Полоса прокрутки
        drawScrollbar(contentX + contentWidth - 8, contentY + 40, visibleHeight, moduleScroll, maxModuleScroll);
    }
    
    private void drawModuleCard(Module module, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isHovered = RenderUtils.isHovered(mouseX, mouseY, x, y, width, height);
        AnimationUtils.Animation animation = moduleAnimations.get(module);
        float progress = animation.getCurrent();
        
        Color bgColor = isHovered ? panelHoverColor : new Color(20, 20, 25, 255);
        
        // Фон модуля
        RenderUtils.drawRect(x, y, x + width, y + height, bgColor.getRGB());
        
        // Граница для включенных модулей
        if (progress > 0) {
            RenderUtils.drawRect(x, y, x + width * progress, y + 2, accentColor.getRGB());
        }
        
        FontRenderer fr = mc.fontRendererObj;
        
        // Название модуля
        fr.drawString(module.getName(), x + 12, y + 8, textColor.getRGB());
        
        // Описание модуля
        String description = moduleDescriptions.get(module.getName());
        if (description != null && !description.isEmpty()) {
            fr.drawString(description, x + 12, y + 20, secondaryTextColor.getRGB());
        } else {
            String status = module.isEnabled() ? "Enabled" : "Disabled";
            fr.drawString(status, x + 12, y + 20, module.isEnabled() ? successColor.getRGB() : secondaryTextColor.getRGB());
        }
        
        // Компактный переключатель
        drawCompactToggle(x + width - 35, y + height / 2 - 6, 28, 12, module.isEnabled(), progress, isHovered);
    }
    
    private void drawCompactToggle(int x, int y, int width, int height, boolean enabled, float progress, boolean hovered) {
        Color bgColor = enabled ? accentColor : new Color(50, 50, 55);
        Color thumbColor = Color.WHITE;
        
        if (hovered) {
            bgColor = enabled ? accentHoverColor : new Color(60, 60, 65);
        }
        
        // Фон переключателя
        RenderUtils.drawRect(x, y, x + width, y + height, bgColor.getRGB());
        
        // Ползунок
        int thumbSize = height - 2;
        int thumbX = (int) (x + 1 + (width - thumbSize - 2) * progress);
        
        // Главный ползунок
        RenderUtils.drawRect(thumbX, y + 1, thumbX + thumbSize, y + 1 + thumbSize, thumbColor.getRGB());
        
        // Небольшая тень для ползунка
        RenderUtils.drawRect(thumbX + 1, y + 2, thumbX + thumbSize - 1, y + thumbSize, new Color(0, 0, 0, 30).getRGB());
        
        // Индикатор состояния (маленький кружок)
        if (enabled) {
            int dotSize = 3;
            int dotX = thumbX + thumbSize / 2 - dotSize / 2;
            int dotY = y + height / 2 - dotSize / 2;
            RenderUtils.drawCircle(dotX + dotSize / 2, dotY + dotSize / 2, dotSize / 2, accentColor.getRGB());
        }
    }
    
    private void drawNewsTab() {
        FontRenderer fr = mc.fontRendererObj;
        int contentX = (int) (guiX + categoryWidth + 20);
        int contentY = (int) (guiY + 50);
        int contentWidth = (int) (guiWidth - categoryWidth - 30);
        int contentHeight = (int) (guiHeight - 60);
        
        // Контейнер для новостей
        RenderUtils.drawRect(contentX, contentY, contentX + contentWidth, contentY + contentHeight, panelColor.getRGB());
        
        // Заголовок
        fr.drawStringWithShadow("NEWS & UPDATES", contentX + 15, contentY + 15, textColor.getRGB());
        
        // Разделитель
        RenderUtils.drawRect(contentX + 15, contentY + 30, contentX + contentWidth - 15, contentY + 31, secondaryTextColor.getRGB());
        
        // Контент новостей
        int newsY = contentY + 40 - newsScroll;
        String[] lines = newsContent.split("\n");
        
        int lineHeight = fr.FONT_HEIGHT + 4;
        int totalHeight = lines.length * lineHeight;
        int visibleHeight = contentHeight - 50;
        maxNewsScroll = Math.max(0, totalHeight - visibleHeight);
        
        for (int i = 0; i < lines.length; i++) {
            int lineY = newsY + i * lineHeight;
            
            if (lineY + lineHeight < contentY + 40 || lineY > contentY + contentHeight) {
                continue;
            }
            
            fr.drawString(lines[i], contentX + 15, lineY, textColor.getRGB());
        }
        
        // Полоса прокрутки
        drawScrollbar(contentX + contentWidth - 8, contentY + 40, visibleHeight, newsScroll, maxNewsScroll);
    }
    
    private void drawSettingsTab(int mouseX, int mouseY) {
        FontRenderer fr = mc.fontRendererObj;
        int contentX = (int) (guiX + categoryWidth + 20);
        int contentY = (int) (guiY + 50);
        int contentWidth = (int) (guiWidth - categoryWidth - 30);
        int contentHeight = (int) (guiHeight - 60);
        
        // Контейнер для настроек
        RenderUtils.drawRect(contentX, contentY, contentX + contentWidth, contentY + contentHeight, panelColor.getRGB());
        
        // Заголовок
        fr.drawStringWithShadow("SETTINGS", contentX + 15, contentY + 15, textColor.getRGB());
        
        // Разделитель
        RenderUtils.drawRect(contentX + 15, contentY + 30, contentX + contentWidth - 15, contentY + 31, secondaryTextColor.getRGB());
        
        // Настройки
        int settingY = contentY + 45;
        int settingHeight = 25;
        
        // Примеры настроек с переключателями
        drawSettingItem("Dark Theme", true, contentX + 15, settingY, contentWidth - 30, settingHeight, mouseX, mouseY);
        settingY += settingHeight + 5;
        
        drawSettingItem("Smooth Animations", true, contentX + 15, settingY, contentWidth - 30, settingHeight, mouseX, mouseY);
        settingY += settingHeight + 5;
        
        drawSettingItem("Sound Effects", false, contentX + 15, settingY, contentWidth - 30, settingHeight, mouseX, mouseY);
        settingY += settingHeight + 5;
        
        drawSettingItem("Rainbow Mode", false, contentX + 15, settingY, contentWidth - 30, settingHeight, mouseX, mouseY);
        settingY += settingHeight + 5;
        
        drawSettingItem("Auto Save", true, contentX + 15, settingY, contentWidth - 30, settingHeight, mouseX, mouseY);
    }
    
    private void drawSettingItem(String name, boolean enabled, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isHovered = RenderUtils.isHovered(mouseX, mouseY, x, y, width, height);
        Color bgColor = isHovered ? panelHoverColor : new Color(20, 20, 25, 255);
        
        // Фон настройки
        RenderUtils.drawRect(x, y, x + width, y + height, bgColor.getRGB());
        
        // Название
        FontRenderer fr = mc.fontRendererObj;
        fr.drawString(name, x + 10, y + height / 2 - fr.FONT_HEIGHT / 2, textColor.getRGB());
        
        // Переключатель
        drawCompactToggle(x + width - 35, y + height / 2 - 6, 28, 12, enabled, enabled ? 1f : 0f, isHovered);
    }
    
    private void drawScrollbar(int x, int y, int height, int scroll, int maxScroll) {
        if (maxScroll <= 0) return;
        
        int thumbHeight = Math.max(20, height * height / (height + maxScroll));
        int thumbY = y + (height - thumbHeight) * scroll / maxScroll;
        
        // Фон скроллбара
        RenderUtils.drawRect(x, y, x + 4, y + height, new Color(40, 40, 45).getRGB());
        
        // Ползунок
        RenderUtils.drawRect(x, thumbY, x + 4, thumbY + thumbHeight, accentColor.getRGB());
    }
    
    private void drawResizeIndicator(int mouseX, int mouseY) {
        int size = 12;
        int x = (int) (guiX + guiWidth - size - 5);
        int y = (int) (guiY + guiHeight - size - 5);
        
        boolean isHovered = RenderUtils.isHovered(mouseX, mouseY, x, y, size, size);
        Color color = isHovered ? accentColor : secondaryTextColor;
        
        // Индикатор изменения размера
        for (int i = 0; i < 3; i++) {
            RenderUtils.drawRect(x + i * 3, y + size - 1, x + i * 3 + 2, y + size, color.getRGB());
            RenderUtils.drawRect(x + size - 1, y + i * 3, x + size, y + i * 3 + 2, color.getRGB());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // ESC для закрытия с сохранением состояния
        if (keyCode == 1) { // ESC
            saveState(); // Сохраняем состояние перед закрытием
            startClosingAnimation();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
    
    private void startClosingAnimation() {
        closing = true;
        guiAnimation.setTarget(0f);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            // Проверяем двойной клик для центрирования
            if (isDoubleClick(mouseX, mouseY)) {
                centerGUI();
                // Сбрасываем время последнего клика, чтобы избежать тройного клика
                lastClickTime = 0;
                return;
            }
            
            // Обновляем данные для отслеживания двойного клика
            lastClickTime = System.currentTimeMillis();
            lastClickX = mouseX;
            lastClickY = mouseY;
            
            // Перетаскивание
            if (RenderUtils.isHovered(mouseX, mouseY, guiX, guiY, guiWidth, 40)) {
                dragging = true;
                dragOffsetX = (int) (mouseX - guiX);
                dragOffsetY = (int) (mouseY - guiY);
                return;
            }
            
            // Клик по вкладкам
            int tabWidth = 90;
            int tabHeight = 25;
            int tabX = (int) (guiX + categoryWidth + 20);
            int tabY = (int) (guiY + 8);
            
            for (int i = 0; i < tabs.length; i++) {
                if (RenderUtils.isHovered(mouseX, mouseY, tabX + i * (tabWidth + 5), tabY, tabWidth, tabHeight)) {
                    activeTab = i;
                    return;
                }
            }
            
            // Клик по категориям
            int categoryY = (int) (guiY + 60);
            int categoryHeight = 32;
            
            for (Category category : Category.values()) {
                if (RenderUtils.isHovered(mouseX, mouseY, guiX + 8, categoryY, categoryWidth - 8, categoryHeight)) {
                    selectedCategory = category;
                    moduleScroll = 0;
                    return;
                }
                categoryY += categoryHeight;
            }
            
            // Изменение размера
            int resizeSize = 12;
            if (RenderUtils.isHovered(mouseX, mouseY, guiX + guiWidth - resizeSize - 5, 
                    guiY + guiHeight - resizeSize - 5, resizeSize, resizeSize)) {
                resizing = true;
                resizeOffsetX = (int) (guiX + guiWidth - mouseX);
                resizeOffsetY = (int) (guiY + guiHeight - mouseY);
                return;
            }
            
            // Клик по модулям
            if (activeTab == 0) {
                handleModulesClick(mouseX, mouseY);
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    private void handleModulesClick(int mouseX, int mouseY) {
        List<Module> modules = new ArrayList<>();
        for (Module module : NiggaWare.instance.moduleManager.getModules()) {
            if (module.getCategory() == selectedCategory) {
                modules.add(module);
            }
        }
        
        int contentX = (int) (guiX + categoryWidth + 20);
        int contentY = (int) (guiY + 50);
        int contentWidth = (int) (guiWidth - categoryWidth - 30);
        int moduleY = contentY + 40 - moduleScroll;
        int moduleHeight = 40;
        
        for (Module module : modules) {
            if (moduleY + moduleHeight < contentY + 40 || moduleY > contentY + contentY + (guiHeight - 60)) {
                moduleY += moduleHeight;
                continue;
            }
            
            if (RenderUtils.isHovered(mouseX, mouseY, contentX + 10, moduleY, contentWidth - 20, moduleHeight - 5)) {
                module.toggle();
                return;
            }
            
            moduleY += moduleHeight;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        resizing = false;
        super.mouseReleased(mouseX, mouseY, state);
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        
        if (wheel != 0) {
            int scrollAmount = wheel > 0 ? -20 : 20;
            
            if (activeTab == 0) {
                moduleScroll = Math.max(0, Math.min(maxModuleScroll, moduleScroll + scrollAmount));
            } else if (activeTab == 1) {
                newsScroll = Math.max(0, Math.min(maxNewsScroll, newsScroll + scrollAmount));
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging) {
            // Устанавливаем целевые позиции для плавного перемещения
            targetX = mouseX - dragOffsetX;
            targetY = mouseY - dragOffsetY;
            
            // Обновляем цели анимации
            guiXAnimation.setTarget(targetX);
            guiYAnimation.setTarget(targetY);
        }
        if (resizing) {
            // Устанавливаем целевые размеры для плавного изменения размера
            targetWidth = Math.max(minWidth, mouseX - guiX + resizeOffsetX);
            targetHeight = Math.max(minHeight, mouseY - guiY + resizeOffsetY);
            
            // Обновляем цели анимации
            guiWidthAnimation.setTarget(targetWidth);
            guiHeightAnimation.setTarget(targetHeight);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void onGuiClosed() {
        saveState(); // Сохраняем состояние при закрытии
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}