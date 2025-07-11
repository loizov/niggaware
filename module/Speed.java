package ru.niggaware.module;

import net.minecraft.block.Block;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockPackedIce;
import net.minecraft.client.Minecraft;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;
import ru.niggaware.utils.Timer;

import java.lang.reflect.Field;

public class Speed extends Module {
    private final Timer timer = new Timer();
    private double moveSpeed;
    private double lastDist;
    private int stage;
    private int mode = 1; // 0 - Legit, 1 - Bypass, 2 - Strafe
    
    public Speed() {
        super("Speed", Category.MOVEMENT);
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        moveSpeed = getBaseMoveSpeed();
        lastDist = 0;
        stage = 2;
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        setMinecraftTimer(50.0f); // Возвращаем нормальную скорость тиков
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.player.isSneaking()) return;
        
        // Не работаем в воде или лаве
        if (mc.player.isInWater() || mc.player.isInLava()) return;
        
        // Рассчитываем последнее пройденное расстояние
        double xDist = mc.player.posX - mc.player.prevPosX;
        double zDist = mc.player.posZ - mc.player.prevPosZ;
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
        
        switch (mode) {
            case 0: // Legit - более медленный, но менее заметный
                handleLegitSpeed();
                break;
                
            case 1: // Bypass - обход большинства античитов
                handleBypassSpeed();
                break;
                
            case 2: // Strafe - более быстрый, но более заметный
                handleStrafeSpeed();
                break;
        }
    }
    
    private void handleLegitSpeed() {
        // Ускорение на льду и с эффектами
        double baseSpeed = getBaseMoveSpeed() * 1.1;
        
        // Если игрок на земле и двигается
        if (mc.player.onGround && isMoving()) {
            stage = 1;
            // Небольшой прыжок (выглядит естественно)
            if (mc.player.isSprinting()) {
                mc.player.motionY = 0.1;
                mc.player.motionX *= 1.1;
                mc.player.motionZ *= 1.1;
            }
        }
        
        // Если двигаемся в воздухе
        if (stage == 1 && isMoving()) {
            stage = 2;
            moveSpeed = baseSpeed * 1.12; // Немного ускоряемся после прыжка
            
        } else if (stage == 2) {
            stage = 3;
            
            // Применяем рассчитанную скорость
            double adjusted = 0.78 * baseSpeed;
            moveSpeed = lastDist - adjusted;
            moveSpeed = Math.max(moveSpeed, baseSpeed);
            
        } else {
            // На земле или в воде
            if (mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset(0, -0.1, 0)).isEmpty() && mc.player.ticksExisted % 4 == 0) {
                moveSpeed -= 0.05 * (moveSpeed - baseSpeed);
            }
            
            moveSpeed = Math.max(moveSpeed, baseSpeed);
        }
        
        setMoveSpeed(moveSpeed);
    }
    
    private void handleBypassSpeed() {
        // Более агрессивный алгоритм с обходом античитов
        if (isMoving()) {
            if (mc.player.onGround) {
                stage = 1;
                // Базовая скорость с рандомизацией для обхода патернов
                double boost = 1.9 + (Math.random() * 0.1);
                moveSpeed = getBaseMoveSpeed() * boost;
                mc.player.motionY = 0.42; // Прыжок
                
                // Имитация нормальной физики прыжка (обход античитов)
                if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                    mc.player.motionY += (mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;
                }
            } else {
                // В воздухе
                if (stage == 1) {
                    stage = 2;
                    // Первый тик в воздухе - максимальное ускорение
                    moveSpeed *= 1.63;
                } else {
                    stage = 3;
                    // Симуляция трения воздуха
                    double adjusted = 0.7 * moveSpeed;
                    moveSpeed = lastDist - adjusted;
                    moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed() * 0.95);
                }
                
                // Небольшая временная манипуляция игрой (сложнее обнаруживается античитами)
                if (mc.player.fallDistance < 0.1 && mc.player.ticksExisted % 2 == 0) {
                    setMinecraftTimer(45.0f); // Небольшое временное ускорение
                } else {
                    setMinecraftTimer(50.0f); // Нормальная скорость
                }
            }
            
            setMoveSpeed(moveSpeed);
        }
    }
    
    private void handleStrafeSpeed() {
        // Агрессивный strafe speed для PVP
        if (isMoving()) {
            if (mc.player.onGround) {
                stage = 1;
                moveSpeed = getBaseMoveSpeed() * 2.0; // Мощное начальное ускорение
                mc.player.motionY = 0.42;
            } else if (stage == 1) {
                stage = 2;
                moveSpeed *= 2.149; // Максимальная скорость после прыжка
            } else if (stage == 2) {
                stage = 3;
                double adjusted = 0.66 * (lastDist - getBaseMoveSpeed());
                moveSpeed = lastDist - adjusted;
            } else {
                moveSpeed = lastDist - lastDist / 159;
            }
            
            setMoveSpeed(Math.max(moveSpeed, getBaseMoveSpeed()));
            
            // Strafe boost
            if (mc.player.moveStrafing != 0) {
                mc.player.motionX *= 1.08;
                mc.player.motionZ *= 1.08;
            }
        }
    }
    
    private double getBaseMoveSpeed() {
        double baseSpeed = 0.2873; // Базовая скорость игрока
        
        // Учитываем эффекты зелий
        if (mc.player.isPotionActive(MobEffects.SPEED)) {
            int amplifier = mc.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        
        // Учитываем замедление
        if (mc.player.isPotionActive(MobEffects.SLOWNESS)) {
            int amplifier = mc.player.getActivePotionEffect(MobEffects.SLOWNESS).getAmplifier();
            baseSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        
        // Проверяем лед под ногами (дополнительное ускорение)
        if (isOnIce()) {
            baseSpeed *= 1.2;
        }
        
        return baseSpeed;
    }
    
    private boolean isOnIce() {
        BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY - 1, mc.player.posZ);
        Block block = mc.world.getBlockState(pos).getBlock();
        return block instanceof BlockIce || block instanceof BlockPackedIce;
    }
    
    private boolean isMoving() {
        // Проверка через нажатые клавиши вместо прямого доступа к полям
        return mc.gameSettings.keyBindForward.isKeyDown() || 
               mc.gameSettings.keyBindBack.isKeyDown() || 
               mc.gameSettings.keyBindLeft.isKeyDown() || 
               mc.gameSettings.keyBindRight.isKeyDown();
    }
    
    private void setMoveSpeed(double speed) {
        // Используем нажатые клавиши вместо доступа к полям
        boolean forward = mc.gameSettings.keyBindForward.isKeyDown();
        boolean back = mc.gameSettings.keyBindBack.isKeyDown();
        boolean left = mc.gameSettings.keyBindLeft.isKeyDown();
        boolean right = mc.gameSettings.keyBindRight.isKeyDown();
        
        float yaw = mc.player.rotationYaw;
        
        if (!forward && !back && !left && !right) {
            mc.player.motionX = 0.0;
            mc.player.motionZ = 0.0;
            return;
        }
        
        // Преобразование нажатий клавиш в направления
        double forwardValue = 0.0;
        double strafeValue = 0.0;
        
        if (forward) forwardValue += 1.0;
        if (back) forwardValue -= 1.0;
        if (left) strafeValue += 1.0;
        if (right) strafeValue -= 1.0;
        
        // Корректировка направления при диагональном движении
        if (forwardValue != 0.0 && strafeValue != 0.0) {
            yaw += (forwardValue > 0.0) ? (strafeValue > 0.0 ? -45 : 45) : (strafeValue > 0.0 ? 45 : -45);
            strafeValue = 0.0;
            if (forwardValue > 0.0) {
                forwardValue = 1.0;
            } else {
                forwardValue = -1.0;
            }
        }
        
        // Применение скорости
        mc.player.motionX = forwardValue * speed * Math.cos(Math.toRadians(yaw + 90.0f)) 
                         + strafeValue * speed * Math.sin(Math.toRadians(yaw + 90.0f));
        mc.player.motionZ = forwardValue * speed * Math.sin(Math.toRadians(yaw + 90.0f)) 
                         - strafeValue * speed * Math.cos(Math.toRadians(yaw + 90.0f));
    }
    
    private void setMinecraftTimer(float tickLength) {
        try {
            Field timerField = Minecraft.class.getDeclaredField("timer");
            timerField.setAccessible(true);
            Object timer = timerField.get(mc);
            
            Field tickLengthField = timer.getClass().getDeclaredField("tickLength");
            tickLengthField.setAccessible(true);
            tickLengthField.setFloat(timer, tickLength);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}