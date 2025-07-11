package ru.niggaware.module;

import net.minecraft.network.play.client.CPacketPlayer;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;

import java.util.Random;

public class Flight extends Module {
    private final Random random = new Random();
    private double startY;
    private int mode = 0; // 0 - Vanilla, 1 - Glide, 2 - Packet
    private float speed = 0.8f;
    private int ticks = 0;
    
    public Flight() {
        super("Flight", Category.MOVEMENT);
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        startY = mc.player.posY;
        ticks = 0;
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        // Сбрасываем скорость падения, чтобы не было урона
        if (mode != 2) {
            mc.player.capabilities.isFlying = false;
            mc.player.capabilities.setFlySpeed(0.05f);
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        ticks++;
        
        switch(mode) {
            case 0: // Vanilla - простой полет с небольшими флуктуациями для обхода
                mc.player.capabilities.isFlying = true;
                mc.player.capabilities.setFlySpeed(speed * 0.05f);
                
                // Небольшие колебания по Y для обхода
                if (ticks % 20 == 0) {
                    mc.player.motionY += (random.nextFloat() - 0.5f) * 0.05;
                }
                break;
                
            case 1: // Glide - медленное падение
                if (!mc.player.onGround) {
                    mc.player.motionY = -0.125; // Медленное падение
                    
                    // Ускорение движения
                    if (mc.gameSettings.keyBindForward.isKeyDown() || 
                        mc.gameSettings.keyBindBack.isKeyDown() || 
                        mc.gameSettings.keyBindLeft.isKeyDown() || 
                        mc.gameSettings.keyBindRight.isKeyDown()) {
                        
                        float yaw = mc.player.rotationYaw;
                        if (mc.gameSettings.keyBindForward.isKeyDown()) {
                            if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                                yaw -= 45;
                            } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
                                yaw += 45;
                            }
                        } else if (mc.gameSettings.keyBindBack.isKeyDown()) {
                            yaw += 180;
                            if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                                yaw += 45;
                            } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
                                yaw -= 45;
                            }
                        } else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                            yaw -= 90;
                        } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
                            yaw += 90;
                        }
                        
                        float mx = (float) Math.cos(Math.toRadians(yaw + 90)) * speed;
                        float mz = (float) Math.sin(Math.toRadians(yaw + 90)) * speed;
                        mc.player.motionX = mx;
                        mc.player.motionZ = mz;
                    } else {
                        mc.player.motionX = 0;
                        mc.player.motionZ = 0;
                    }
                }
                break;
                
            case 2: // Packet - обход на основе пакетов (работает на многих античитах)
                // Сбрасываем реальные движения
                mc.player.motionX = 0;
                mc.player.motionY = 0;
                mc.player.motionZ = 0;
                mc.player.setVelocity(0, 0, 0);
                
                // Каждые 10 тиков отправляем пакет с позицией немного ниже
                // Это обманывает античиты, которые проверяют падение
                if (ticks % 10 == 0) {
                    mc.player.connection.sendPacket(new CPacketPlayer.Position(
                        mc.player.posX,
                        mc.player.posY - 0.03125, // Выглядит как легитимное падение
                        mc.player.posZ,
                        mc.player.onGround));
                }
                
                // Если нажаты клавиши движения
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    // Поднимаемся
                    mc.player.setPosition(mc.player.posX, mc.player.posY + speed * 0.5, mc.player.posZ);
                }
                
                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    // Опускаемся
                    mc.player.setPosition(mc.player.posX, mc.player.posY - speed * 0.5, mc.player.posZ);
                }
                
                // Горизонтальное движение
                double forward = mc.player.movementInput.field_192832_b;
                double strafe = mc.player.movementInput.moveStrafe;
                float yaw = mc.player.rotationYaw;
                
                if (forward != 0 || strafe != 0) {
                    if (forward != 0) {
                        if (strafe > 0) {
                            yaw += (forward > 0 ? -45 : 45);
                        } else if (strafe < 0) {
                            yaw += (forward > 0 ? 45 : -45);
                        }
                        strafe = 0;
                        if (forward > 0) {
                            forward = 1;
                        } else {
                            forward = -1;
                        }
                    }
                    
                    mc.player.setPosition(
                        mc.player.posX + (forward * speed * Math.cos(Math.toRadians(yaw + 90)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90))),
                        mc.player.posY,
                        mc.player.posZ + (forward * speed * Math.sin(Math.toRadians(yaw + 90)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90)))
                    );
                }
                break;
        }
    }
}