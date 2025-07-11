package ru.niggaware.module;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ItemESP extends Module {
    private boolean nameTag = true;
    private boolean showDistance = true;
    private boolean throughWalls = true;
    private boolean glow = true;
    private float lineWidth = 1.5f;
    
    // Категории предметов (для разных цветов)
    private boolean showWeapons = true;
    private boolean showArmor = true;
    private boolean showTools = true;
    private boolean showBlocks = true;
    
    // Кэш для хранения предметов
    private final Map<Integer, Long> seenItems = new HashMap<>();
    
    // Поля для рефлексии к renderPos
    private static Field renderPosXField;
    private static Field renderPosYField;
    private static Field renderPosZField;
    
    // Статическая инициализация полей рефлексии
    static {
        try {
            renderPosXField = RenderManager.class.getDeclaredField("renderPosX");
            renderPosYField = RenderManager.class.getDeclaredField("renderPosY");
            renderPosZField = RenderManager.class.getDeclaredField("renderPosZ");
            
            renderPosXField.setAccessible(true);
            renderPosYField.setAccessible(true);
            renderPosZField.setAccessible(true);
        } catch (Exception e) {
            try {
                // Пробуем альтернативные имена (обфусцированные)
                renderPosXField = RenderManager.class.getDeclaredField("field_78725_b");
                renderPosYField = RenderManager.class.getDeclaredField("field_78726_c");
                renderPosZField = RenderManager.class.getDeclaredField("field_78723_d");
                
                renderPosXField.setAccessible(true);
                renderPosYField.setAccessible(true);
                renderPosZField.setAccessible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // Цвета для разных типов предметов
    private static final Color WEAPON_COLOR = new Color(255, 0, 0, 180);   // Красный
    private static final Color ARMOR_COLOR = new Color(0, 0, 255, 180);    // Синий
    private static final Color TOOL_COLOR = new Color(255, 0, 255, 180);   // Фиолетовый
    private static final Color BLOCK_COLOR = new Color(0, 255, 0, 180);    // Зеленый
    private static final Color DEFAULT_COLOR = new Color(255, 255, 0, 180); // Желтый
    
    public ItemESP() {
        super("ItemESP", Category.RENDER);
    }
    
    @Override
    public void onEnable() {
        // Очищаем кэш при включении модуля
        seenItems.clear();
    }
    
    @Override
    public void onTick() {
        // Удаляем устаревшие предметы из кэша (более 5 секунд)
        long currentTime = System.currentTimeMillis();
        seenItems.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000);
    }
    
    @Override
    public void onRender() {
        if (mc.player == null || mc.world == null) return;
        
        try {
            // Получаем позиции рендера с помощью рефлексии
            double renderPosX = (double) renderPosXField.get(mc.getRenderManager());
            double renderPosY = (double) renderPosYField.get(mc.getRenderManager());
            double renderPosZ = (double) renderPosZField.get(mc.getRenderManager());
            
            // Подготавливаем OpenGL
            GL11.glPushMatrix();
            
            // Отключаем глубину для рисования через стены, если нужно
            if (throughWalls) {
                GlStateManager.disableDepth();
            }
            
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(lineWidth);
            
            // Текущее время для кэша
            long currentTime = System.currentTimeMillis();
            
            // Проходимся по всем предметам в мире
            for (Entity entity : mc.world.loadedEntityList) {
                if (entity instanceof EntityItem) {
                    EntityItem itemEntity = (EntityItem) entity;
                    
                    // Обновляем кэш для этого предмета
                    seenItems.put(itemEntity.getEntityId(), currentTime);
                    
                    // Получаем цвет для предмета
                    Color color = getItemColor(itemEntity);
                    
                    // Рисуем ESP для предмета
                    drawItemBox(itemEntity, color, renderPosX, renderPosY, renderPosZ);
                    
                    // Отрисовка названия, если включено
                    if (nameTag) {
                        drawItemNameTag(itemEntity, renderPosX, renderPosY, renderPosZ);
                    }
                }
            }
            
            // Восстанавливаем состояние OpenGL
            GlStateManager.enableTexture2D();
            
            if (throughWalls) {
                GlStateManager.enableDepth();
            }
            
            GlStateManager.disableBlend();
            GL11.glPopMatrix();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Color getItemColor(EntityItem item) {
        Item itemType = item.getEntityItem().getItem();
        
        // Оружие
        if (showWeapons && itemType instanceof ItemSword) {
            return WEAPON_COLOR;
        }
        
        // Броня
        if (showArmor && itemType instanceof ItemArmor) {
            return ARMOR_COLOR;
        }
        
        // Инструменты
        if (showTools && itemType instanceof ItemTool) {
            return TOOL_COLOR;
        }
        
        // Блоки
        if (showBlocks && itemType instanceof ItemBlock) {
            return BLOCK_COLOR;
        }
        
        // По умолчанию
        return DEFAULT_COLOR;
    }
    
    private void drawItemBox(EntityItem item, Color color, double renderPosX, double renderPosY, double renderPosZ) {
        // Интерполируем позицию для плавности
        double x = item.lastTickPosX + (item.posX - item.lastTickPosX) * mc.getRenderPartialTicks();
        double y = item.lastTickPosY + (item.posY - item.lastTickPosY) * mc.getRenderPartialTicks();
        double z = item.lastTickPosZ + (item.posZ - item.lastTickPosZ) * mc.getRenderPartialTicks();
        
        // Смещение камеры
        x = x - renderPosX;
        y = y - renderPosY;
        z = z - renderPosZ;
        
        // Создаем хитбокс для предмета (немного меньше стандартного)
        double boxSize = 0.2; // Уменьшенный размер для предметов
        AxisAlignedBB box = new AxisAlignedBB(
            x - boxSize, 
            y, 
            z - boxSize, 
            x + boxSize, 
            y + boxSize * 2, 
            z + boxSize
        );
        
        // Рисуем контур
        RenderGlobal.drawSelectionBoundingBox(
            box, 
            color.getRed() / 255.0F, 
            color.getGreen() / 255.0F, 
            color.getBlue() / 255.0F, 
            color.getAlpha() / 255.0F
        );
        
        // Эффект свечения, если включен
        if (glow) {
            RenderGlobal.renderFilledBox(
                box, 
                color.getRed() / 255.0F, 
                color.getGreen() / 255.0F, 
                color.getBlue() / 255.0F, 
                color.getAlpha() / 255.0F * 0.3F
            );
        }
    }
    
    private void drawItemNameTag(EntityItem item, double renderPosX, double renderPosY, double renderPosZ) {
        // Интерполируем позицию для плавности
        double x = item.lastTickPosX + (item.posX - item.lastTickPosX) * mc.getRenderPartialTicks();
        double y = item.lastTickPosY + (item.posY - item.lastTickPosY) * mc.getRenderPartialTicks();
        double z = item.lastTickPosZ + (item.posZ - item.lastTickPosZ) * mc.getRenderPartialTicks();
        
        // Смещение камеры
        x = x - renderPosX;
        y = y - renderPosY + 0.5;  // Поднимаем метку над предметом
        z = z - renderPosZ;
        
        // Получаем имя предмета
        String itemName = item.getEntityItem().getDisplayName();
        
        // Добавляем расстояние, если включено
        if (showDistance) {
            // Исправленный вариант для расчета расстояния между сущностями
            double distX = item.posX - mc.player.posX;
            double distY = item.posY - mc.player.posY;
            double distZ = item.posZ - mc.player.posZ;
            double distance = Math.sqrt(distX * distX + distY * distY + distZ * distZ);
            itemName += String.format(" §7[%.1fm]", distance);
        }
        
        // Включаем нужные OpenGL флаги для отрисовки текста
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableTexture2D();
        
        // Поворачиваем текст к игроку
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F); // Масштаб текста
        
        // Отрисовка текста
        FontRenderer fontRenderer = mc.fontRendererObj;
        int textWidth = fontRenderer.getStringWidth(itemName) / 2;
        
        // Фон для текста
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        
        // Рисуем полупрозрачный фон
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.5F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(-textWidth - 2, -2);
        GL11.glVertex2d(-textWidth - 2, 9);
        GL11.glVertex2d(textWidth + 2, 9);
        GL11.glVertex2d(textWidth + 2, -2);
        GL11.glEnd();
        
        // Возвращаем настройки для рисования текста
        GlStateManager.enableTexture2D();
        
        // Рисуем текст
        fontRenderer.drawStringWithShadow(itemName, -textWidth, 0, -1);
        
        // Восстанавливаем состояние OpenGL
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
    
    // Публичные методы для настройки модуля
    public void toggleNameTag() {
        nameTag = !nameTag;
    }
    
    public void toggleShowDistance() {
        showDistance = !showDistance;
    }
    
    public void toggleThroughWalls() {
        throughWalls = !throughWalls;
    }
    
    public void toggleGlow() {
        glow = !glow;
    }
    
    public void toggleWeapons() {
        showWeapons = !showWeapons;
    }
    
    public void toggleArmor() {
        showArmor = !showArmor;
    }
    
    public void toggleTools() {
        showTools = !showTools;
    }
    
    public void toggleBlocks() {
        showBlocks = !showBlocks;
    }
    
    public void setLineWidth(float width) {
        this.lineWidth = width;
    }
}