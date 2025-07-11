package ru.niggaware.module;

import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;
import ru.niggaware.NiggaWare;
import ru.niggaware.event.EventReceivePacket;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;

import java.lang.reflect.Field;
import java.util.Random;

public class AntiKnockback extends Module {
    private final Random random = new Random();
    
    // Настройки
    private int horizontal = 0; // 0-100%
    private int vertical = 0;   // 0-100%
    private boolean explosions = true;
    private int mode = 1;       // 0=Full, 1=Smart, 2=Partial
    
    public AntiKnockback() {
        super("AntiKnockback", Category.COMBAT);
    }
    
    @Override
    public void onEnable() {
        // Настройки по умолчанию для разных режимов
        switch (mode) {
            case 0: // Full - полное отключение отбрасывания
                horizontal = 0;
                vertical = 0;
                break;
            case 1: // Smart - имитация частичного отбрасывания
                horizontal = 10 + random.nextInt(20); // 10-30%
                vertical = 30 + random.nextInt(20);   // 30-50%
                break;
            case 2: // Partial - заметное уменьшение отбрасывания
                horizontal = 40 + random.nextInt(15); // 40-55%
                vertical = 50 + random.nextInt(20);   // 50-70%
                break;
        }
    }
    
    // Метод, вызываемый, когда приходит пакет с сервера
    public void onPacket(EventReceivePacket event) {
        if (mc.player == null) return;
        
        // Пакет отбрасывания от сущности (атаки)
        if (event.getPacket() instanceof SPacketEntityVelocity) {
            SPacketEntityVelocity packet = (SPacketEntityVelocity) event.getPacket();
            
            // Проверяем, что это наш игрок
            if (packet.getEntityID() == mc.player.getEntityId()) {
                
                // Если мы в режиме "Smart", корректируем силу в зависимости от ситуации
                if (mode == 1) {
                    // Если игрок в воздухе, чуть больше снижаем отбрасывание
                    if (!mc.player.onGround) {
                        horizontal = Math.max(0, horizontal - 5);
                        vertical = Math.max(0, vertical - 10);
                    }
                    
                    // Если у игрока мало здоровья, больше снижаем отбрасывание (выгодно)
                    if (mc.player.getHealth() < 10.0f) {
                        horizontal = Math.max(0, horizontal - 15);
                        vertical = Math.max(0, vertical - 15);
                    }
                }
                
                try {
                    // Используем рефлексию для доступа к приватным полям
                    Field motionXField = SPacketEntityVelocity.class.getDeclaredField("motionX");
                    Field motionYField = SPacketEntityVelocity.class.getDeclaredField("motionY");
                    Field motionZField = SPacketEntityVelocity.class.getDeclaredField("motionZ");
                    
                    motionXField.setAccessible(true);
                    motionYField.setAccessible(true);
                    motionZField.setAccessible(true);
                    
                    // Модифицируем силу отбрасывания
                    if (horizontal != 100) {
                        int motionX = motionXField.getInt(packet);
                        int motionZ = motionZField.getInt(packet);
                        
                        motionXField.setInt(packet, motionX * horizontal / 100);
                        motionZField.setInt(packet, motionZ * horizontal / 100);
                    }
                    
                    if (vertical != 100) {
                        int motionY = motionYField.getInt(packet);
                        motionYField.setInt(packet, motionY * vertical / 100);
                    }
                    
                    // Если мы хотим полностью отменить отбрасывание
                    if (horizontal == 0 && vertical == 0) {
                        event.setCancelled(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Пакет отбрасывания от взрыва
        if (explosions && event.getPacket() instanceof SPacketExplosion) {
            SPacketExplosion packet = (SPacketExplosion) event.getPacket();
            
            try {
                // Используем рефлексию для доступа к приватным полям
                Field motionXField = SPacketExplosion.class.getDeclaredField("motionX");
                Field motionYField = SPacketExplosion.class.getDeclaredField("motionY");
                Field motionZField = SPacketExplosion.class.getDeclaredField("motionZ");
                
                motionXField.setAccessible(true);
                motionYField.setAccessible(true);
                motionZField.setAccessible(true);
                
                // Изменяем силу взрыва с теми же параметрами
                if (horizontal != 100) {
                    float motionX = motionXField.getFloat(packet);
                    float motionZ = motionZField.getFloat(packet);
                    
                    motionXField.setFloat(packet, motionX * horizontal / 100);
                    motionZField.setFloat(packet, motionZ * horizontal / 100);
                }
                
                if (vertical != 100) {
                    float motionY = motionYField.getFloat(packet);
                    motionYField.setFloat(packet, motionY * vertical / 100);
                }
                
                // Полное отключение взрывов
                if (horizontal == 0 && vertical == 0) {
                    event.setCancelled(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Этот метод должен вызываться из главного класса при приеме пакетов
    public static void handlePacket(EventReceivePacket event) {
        AntiKnockback module = (AntiKnockback) NiggaWare.instance.moduleManager.getModuleByName("AntiKnockback");
        if (module != null && module.isEnabled()) {
            module.onPacket(event);
        }
    }
}