package ru.niggaware.module;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ESP extends Module {
    // Настройки
    private boolean players = true;
    private boolean mobs = true;
    private boolean animals = false;
    private boolean throughWalls = true;
    private boolean skeleton = false;
    private boolean twoD = true;
    private boolean boxESP = true;
    private float lineWidth = 2.0f;
    
    // Кэш для скелета
    private final Map<EntityPlayer, float[][]> playerRotationMap = new HashMap<>();
    
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
    
    // Цвета для разных типов сущностей
    private static final Color PLAYER_COLOR = new Color(255, 0, 0, 120); // Красный для игроков
    private static final Color TEAMMATE_COLOR = new Color(0, 255, 0, 120); // Зеленый для команды
    private static final Color MOB_COLOR = new Color(255, 165, 0, 120); // Оранжевый для мобов
    private static final Color ANIMAL_COLOR = new Color(0, 255, 255, 120); // Голубой для животных
    
    public ESP() {
        super("ESP", Category.RENDER);
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
            setupGLState(true);
            
            // Проходим по всем сущностям
            for (Entity entity : mc.world.loadedEntityList) {
                if (shouldRender(entity)) {
                    Color color = getEntityColor(entity);
                    
                    if (boxESP) {
                        // Рисуем ESP бокс вокруг сущности
                        drawEntityBox(entity, color, renderPosX, renderPosY, renderPosZ);
                    }
                    
                    if (twoD && entity instanceof EntityPlayer) {
                        // Рисуем 2D ESP для игроков
                        draw2DESP(entity, color, renderPosX, renderPosY, renderPosZ);
                    }
                    
                    if (skeleton && entity instanceof EntityPlayer) {
                        // Здесь можно добавить логику для скелета, если нужно
                    }
                }
            }
            
            // Восстанавливаем состояние OpenGL
            setupGLState(false);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupGLState(boolean enable) {
        if (enable) {
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
            
        } else {
            // Восстанавливаем состояние OpenGL
            GlStateManager.enableTexture2D();
            
            if (throughWalls) {
                GlStateManager.enableDepth();
            }
            
            GlStateManager.disableBlend();
            GL11.glPopMatrix();
        }
    }
    
    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) return false; // Не рендерим самого себя
        if (entity.isDead) return false;       // Не рендерим мёртвых
        
        if (entity instanceof EntityPlayer && players) {
            return !((EntityPlayer) entity).isSpectator(); // Не рендерим наблюдателей
        }
        if (entity instanceof EntityMob && mobs) {
            return true;
        }
        if (entity instanceof EntityAnimal && animals) {
            return true;
        }
        
        return false;
    }
    
    private Color getEntityColor(Entity entity) {
        if (entity instanceof EntityPlayer) {
            // Проверяем команду только если это EntityPlayer
            EntityPlayer player = (EntityPlayer) entity;
            if (mc.player.isOnSameTeam(player)) {
                return TEAMMATE_COLOR;
            } else {
                return PLAYER_COLOR;
            }
        } else if (entity instanceof EntityMob) {
            return MOB_COLOR;
        } else if (entity instanceof EntityAnimal) {
            return ANIMAL_COLOR;
        }
        
        return PLAYER_COLOR; // По умолчанию
    }
    
    private void drawEntityBox(Entity entity, Color color, double renderPosX, double renderPosY, double renderPosZ) {
        // Интерполируем позицию для плавности
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.getRenderPartialTicks();
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.getRenderPartialTicks();
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.getRenderPartialTicks();
        
        // Смещение камеры
        x = x - renderPosX;
        y = y - renderPosY;
        z = z - renderPosZ;
        
        // Получаем хитбокс сущности
        AxisAlignedBB box = new AxisAlignedBB(
            x - entity.width / 2, 
            y, 
            z - entity.width / 2, 
            x + entity.width / 2, 
            y + entity.height, 
            z + entity.width / 2
        );
        
        // Рисуем контур
        RenderGlobal.drawSelectionBoundingBox(
            box, 
            color.getRed() / 255.0F, 
            color.getGreen() / 255.0F, 
            color.getBlue() / 255.0F, 
            color.getAlpha() / 255.0F
        );
        
        // Рисуем заполненный бокс с меньшей прозрачностью
        RenderGlobal.renderFilledBox(
            box,
            color.getRed() / 255.0F, 
            color.getGreen() / 255.0F, 
            color.getBlue() / 255.0F, 
            color.getAlpha() / 255.0F * 0.3F
        );
    }
    
    private void draw2DESP(Entity entity, Color color, double renderPosX, double renderPosY, double renderPosZ) {
        // Интерполируем позицию для плавности
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.getRenderPartialTicks();
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.getRenderPartialTicks();
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.getRenderPartialTicks();
        
        // Смещение камеры
        x = x - renderPosX;
        y = y - renderPosY;
        z = z - renderPosZ;
        
        // Получаем хитбокс сущности
        double width = entity.width / 2.0;
        double height = entity.height;
        
        // Рисуем линии 2D ESP (как в предоставленном коде)
        GL11.glColor4f(
            color.getRed() / 255.0F, 
            color.getGreen() / 255.0F, 
            color.getBlue() / 255.0F, 
            color.getAlpha() / 255.0F
        );
        
        // Рисуем 2D каркас
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x - width, y, z - width);
        GL11.glVertex3d(x - width, y + height, z - width);
        GL11.glVertex3d(x + width, y + height, z - width);
        GL11.glVertex3d(x + width, y, z - width);
        GL11.glEnd();
        
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x - width, y, z + width);
        GL11.glVertex3d(x - width, y + height, z + width);
        GL11.glVertex3d(x + width, y + height, z + width);
        GL11.glVertex3d(x + width, y, z + width);
        GL11.glEnd();
        
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x - width, y, z - width);
        GL11.glVertex3d(x - width, y, z + width);
        
        GL11.glVertex3d(x + width, y, z - width);
        GL11.glVertex3d(x + width, y, z + width);
        
        GL11.glVertex3d(x - width, y + height, z - width);
        GL11.glVertex3d(x - width, y + height, z + width);
        
        GL11.glVertex3d(x + width, y + height, z - width);
        GL11.glVertex3d(x + width, y + height, z + width);
        GL11.glEnd();
    }
    
    // Публичные методы для настройки модуля
    public void togglePlayers() {
        players = !players;
    }
    
    public void toggleMobs() {
        mobs = !mobs;
    }
    
    public void toggleAnimals() {
        animals = !animals;
    }
    
    public void toggleThroughWalls() {
        throughWalls = !throughWalls;
    }
    
    public void toggle2D() {
        twoD = !twoD;
    }
    
    public void toggleBoxESP() {
        boxESP = !boxESP;
    }
    
    public void toggleSkeleton() {
        skeleton = !skeleton;
    }
    
    public void setLineWidth(float width) {
        this.lineWidth = width;
    }
}