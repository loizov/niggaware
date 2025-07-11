package ru.niggaware.module;

import net.minecraft.client.Minecraft;
import ru.niggaware.module.Module;

public class Sprint extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public Sprint() {
        super("Sprint");
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.gameSettings.keyBindForward.isKeyDown() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }
}