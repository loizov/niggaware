package ru.niggaware.module;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.niggaware.module.Category;
import ru.niggaware.module.Module;
import ru.niggaware.utils.TimerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Scaffold extends Module {
    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();
    
    private BlockPos currentBlock = null;
    private EnumFacing currentFace = null;
    private boolean rotated = false;
    private float[] rotations = new float[2];
    private int ticksOnAir = 0;
    
    // Настройки
    private boolean safeWalk = true;
    private boolean tower = true;
    private boolean swing = true;
    private int tickDelay = 2;
    private int placeDelay = 50;
    private boolean sneak = true;
    
    // Список блоков, которые можно использовать
    private final List<Block> validBlocks = Arrays.asList(
        Blocks.OBSIDIAN,
        Blocks.COBBLESTONE,
        Blocks.STONE,
        Blocks.DIRT,
        Blocks.GRASS,
        Blocks.GRAVEL,
        Blocks.SAND
        // Добавьте другие блоки по необходимости
    );
    
    public Scaffold() {
        super("Scaffold", Category.PLAYER);
    }
    
    @Override
    public void onEnable() {
        currentBlock = null;
        currentFace = null;
        rotated = false;
        ticksOnAir = 0;
    }
    
    @Override
    public void onDisable() {
        // Сбрасываем автоприседание, если было включено
        if (sneak && mc.player != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // SafeWalk - предотвращает падение при установке блоков на краю
        if (safeWalk && !mc.player.onGround) {
            ticksOnAir++;
            if (ticksOnAir <= 3) { // Безопасное падение только первые несколько тиков
                mc.player.motionX = 0;
                mc.player.motionZ = 0;
            }
        } else {
            ticksOnAir = 0;
        }
        
        // Находим блок под игроком
        BlockPos blockUnder = new BlockPos(
            mc.player.posX, 
            mc.player.posY - 1.0, 
            mc.player.posZ
        );
        
        // Проверяем, есть ли там воздух
        if (mc.world.getBlockState(blockUnder).getBlock() instanceof BlockAir) {
            // Ищем подходящую позицию и грань
            findNextBlock();
            
            // Если нашли куда ставить блок
            if (currentBlock != null && currentFace != null) {
                // Выбираем блок в хотбаре
                int slot = findBlockInHotbar();
                if (slot != -1) {
                    // Запоминаем текущий слот
                    int prevSlot = mc.player.inventory.currentItem;
                    
                    // Меняем на нужный слот
                    mc.player.inventory.currentItem = slot;
                    
                    // Поворот на блок (делаем случайные задержки)
                    if (!rotated && mc.player.ticksExisted % tickDelay == 0) {
                        float[] rotTo = getRotationToBlock(currentBlock, currentFace);
                        rotations[0] = limitAngleChange(mc.player.rotationYaw, rotTo[0], random.nextInt(40) + 30);
                        rotations[1] = limitAngleChange(mc.player.rotationPitch, rotTo[1], random.nextInt(30) + 15);
                        rotated = true;
                    }
                    
                    // Применяем поворот
                    if (rotated) {
                        mc.player.rotationYaw = rotations[0];
                        mc.player.rotationPitch = rotations[1];
                        
                        // Приседание для обхода античитов
                        if (sneak && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                        }
                        
                        // Tower (быстрое поднятие при прыжке)
                        if (tower && mc.gameSettings.keyBindJump.isKeyDown()) {
                            if (mc.player.onGround) {
                                mc.player.motionY = 0.42f;
                            } else if (mc.player.motionY < 0.1) {
                                mc.player.motionY = -0.3;
                            }
                        }
                        
                        // Установка блока с задержкой
                        if (timer.hasTimeElapsed(placeDelay, true)) {
                            // Установка блока
                            mc.playerController.processRightClickBlock(
                                mc.player, 
                                mc.world, 
                                currentBlock, 
                                currentFace, 
                                new Vec3d(
                                    currentBlock.getX() + 0.5 + currentFace.getDirectionVec().getX() * 0.5,
                                    currentBlock.getY() + 0.5 + currentFace.getDirectionVec().getY() * 0.5,
                                    currentBlock.getZ() + 0.5 + currentFace.getDirectionVec().getZ() * 0.5
                                ),
                                EnumHand.MAIN_HAND
                            );
                            
                            // Взмах рукой
                            if (swing) {
                                mc.player.swingArm(EnumHand.MAIN_HAND);
                            }
                            
                            // Сбрасываем
                            rotated = false;
                            currentBlock = null;
                            currentFace = null;
                        }
                    }
                    
                    // Возвращаем слот
                    mc.player.inventory.currentItem = prevSlot;
                }
            }
        } else {
            // Если блок уже есть, отключаем приседание
            if (sneak) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }
    
    private void findNextBlock() {
        // Проверяем блок под игроком
        BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY - 1.0, mc.player.posZ);
        
        // Если блок - воздух, пробуем установить блок
        if (mc.world.getBlockState(pos).getBlock() instanceof BlockAir) {
            // Ищем соседний блок, к которому можно прицепить новый
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos neighbor = pos.offset(facing);
                
                // Блок должен быть твердым
                if (mc.world.getBlockState(neighbor).getBlock().canCollideCheck(mc.world.getBlockState(neighbor), false)) {
                    // Ставим блок с обратной стороны
                    currentBlock = neighbor;
                    currentFace = facing.getOpposite();
                    return;
                }
            }
        }
    }
    
    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (validBlocks.contains(block)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private float[] getRotationToBlock(BlockPos pos, EnumFacing face) {
        Vec3d eyesPos = new Vec3d(
            mc.player.posX,
            mc.player.posY + mc.player.getEyeHeight(),
            mc.player.posZ
        );
        
        Vec3d hitVec = new Vec3d(pos)
            .addVector(0.5, 0.5, 0.5)
            .add(new Vec3d(face.getDirectionVec()).scale(0.5));
        
        double diffX = hitVec.xCoord - eyesPos.xCoord;
        double diffY = hitVec.yCoord - eyesPos.yCoord;
        double diffZ = hitVec.zCoord - eyesPos.zCoord;
        
        double dist = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        
        // Добавляем небольшие случайные колебания
        yaw += (random.nextFloat() - 0.5f) * 2.0f;
        pitch += (random.nextFloat() - 0.5f) * 2.0f;
        
        return new float[]{yaw, pitch};
    }
    
    private float limitAngleChange(float current, float target, float maxChange) {
        float deltaAngle = MathHelper.wrapDegrees(target - current);
        if (deltaAngle > maxChange) deltaAngle = maxChange;
        if (deltaAngle < -maxChange) deltaAngle = -maxChange;
        return MathHelper.wrapDegrees(current + deltaAngle);
    }
}