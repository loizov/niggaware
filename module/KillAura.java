package ru.niggaware.module;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;
import ru.niggaware.utils.TimerUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class KillAura extends Module {
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil rotationTimer = new TimerUtil();
    private final Random random = new Random();
    
    private EntityLivingBase target = null;
    private float[] serverRotation = new float[2];
    private boolean rotated = false;
    
    // Настройки модуля
    private float range = 3.8f; // Радиус работы KillAura
    private int delay = 90; // Интервал атак (мс)
    private int rotationDelay = 70; // Интервал поворота (мс)
    private float fov = 180f; // Угол обзора для выбора цели
    private boolean silentRotation = true; // Невидимые повороты
    private boolean randomizeHits = true; // Рандомизация ударов
    private boolean players = true; // Атаковать игроков
    private boolean mobs = false; // Атаковать мобов
    private boolean invisibles = false; // Атаковать невидимых
    private boolean walls = false; // Атаковать через стены
    
    public KillAura() {
        super("KillAura", Category.COMBAT);
    }
    
    @Override
    public void onEnable() {
        target = null;
        rotated = false;
    }
    
    @Override
    public void onDisable() {
        target = null;
        rotated = false;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Поиск цели
        findTarget();
        
        if (target != null) {
            // Поворот к цели (с задержкой)
            if (rotationTimer.hasTimeElapsed(rotationDelay, true)) {
                rotateToTarget(target);
                rotated = true;
            }
            
            // Атака (с задержкой)
            if (rotated && attackTimer.hasTimeElapsed(delay + (randomizeHits ? random.nextInt(50) : 0), true)) {
                attackTarget();
            }
        } else {
            rotated = false;
        }
    }
    
    private void findTarget() {
        List<EntityLivingBase> targets = mc.world.loadedEntityList.stream()
            .filter(entity -> isValidTarget(entity))
            .map(entity -> (EntityLivingBase) entity)
            .collect(Collectors.toList());
        
        // Сортировка по расстоянию
        targets.sort(Comparator.comparingDouble(entity -> 
            mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ)));
        
        if (!targets.isEmpty()) {
            target = targets.get(0);
        } else {
            target = null;
        }
    }
    
    private boolean isValidTarget(Entity entity) {
        // Проверка базовых условий
        if (!(entity instanceof EntityLivingBase)) return false;
        if (entity == mc.player) return false;
        if (entity.isDead) return false;
        if (((EntityLivingBase)entity).getHealth() <= 0) return false;
        
        // Проверка расстояния
        double distSq = mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ);
        if (distSq > range * range) return false;
        
        // Проверка FOV
        if (getFov(entity) > fov) return false;
        
        // Проверка видимости
        if (!walls && !mc.player.canEntityBeSeen(entity)) return false;
        
        // Проверка типа сущности
        if (entity instanceof EntityPlayer) {
            if (!players) return false;
            // Не атакуем команду или себя
            EntityPlayer player = (EntityPlayer) entity;
            if (mc.player.isOnSameTeam(player)) return false;
        } else {
            // Проверка на моба
            if (!mobs) return false;
        }
        
        // Проверка на невидимость
        if (entity.isInvisible() && !invisibles) return false;
        
        return true;
    }
    
    private float getFov(Entity entity) {
        // Расчет FOV до цели
        double x = entity.posX - mc.player.posX;
        double z = entity.posZ - mc.player.posZ;
        double yaw = Math.atan2(z, x) * 57.2957795;
        yaw = yaw - mc.player.rotationYaw;
        while (yaw > 180) yaw -= 360;
        while (yaw < -180) yaw += 360;
        return Math.abs((float) yaw);
    }
    
    private void rotateToTarget(EntityLivingBase target) {
        // Получаем точку нацеливания (тело или голова)
        Vec3d eyesPos = mc.player.getPositionEyes(1.0f);
        Vec3d targetPos = new Vec3d(target.posX, 
                                     target.posY + target.getEyeHeight() / 2, 
                                     target.posZ);
        
        // Векторы направления
        double diffX = targetPos.xCoord - eyesPos.xCoord;
        double diffY = targetPos.yCoord - eyesPos.yCoord;
        double diffZ = targetPos.zCoord - eyesPos.zCoord;
        
        // Рассчитываем углы
        double distance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));
        
        // Сглаживаем повороты для маскировки под человека
        if (silentRotation) {
            // Сервер видит эти повороты
            serverRotation[0] = limitAngleChange(serverRotation[0], yaw, 35 + random.nextInt(5));
            serverRotation[1] = limitAngleChange(serverRotation[1], pitch, 35 + random.nextInt(5));
            
            // Пакет поворота на сервер (без анимации для игрока)
            mc.player.connection.getNetworkManager().sendPacket(
                new net.minecraft.network.play.client.CPacketPlayer.Rotation(
                    serverRotation[0], serverRotation[1], mc.player.onGround));
        } else {
            // Обычный поворот (видимый)
            mc.player.rotationYaw = limitAngleChange(mc.player.rotationYaw, yaw, 35 + random.nextInt(5));
            mc.player.rotationPitch = limitAngleChange(mc.player.rotationPitch, pitch, 35 + random.nextInt(5));
        }
    }
    
    private float limitAngleChange(float current, float target, float maxChange) {
        float deltaAngle = MathHelper.wrapDegrees(target - current);
        if (deltaAngle > maxChange) deltaAngle = maxChange;
        if (deltaAngle < -maxChange) deltaAngle = -maxChange;
        return MathHelper.wrapDegrees(current + deltaAngle);
    }
    
    private void attackTarget() {
        if (target == null) return;
        
        // Проверяем расстояние (защита от лагов)
        double distSq = mc.player.getDistanceSq(target.posX, target.posY, target.posZ);
        if (distSq <= range * range) {
            // Поворот перед ударом (дополнительная защита от античитов)
            if (!silentRotation) {
                rotateToTarget(target);
            }
            
            // Делаем критический удар с вероятностью 30%
            boolean crit = mc.player.fallDistance > 0.1f && !mc.player.onGround && !mc.player.isInWater() && !mc.player.isOnLadder() && random.nextInt(100) < 30;
            
            // Отправляем пакет атаки напрямую (обход некоторых античитов)
            mc.player.connection.sendPacket(new CPacketUseEntity(target));
            mc.player.swingArm(EnumHand.MAIN_HAND);
            
            // Дополнительная защита от детекта - небольшая задержка после удара
            mc.player.resetCooldown();
        }
    }
}