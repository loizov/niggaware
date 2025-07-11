package ru.niggaware.module;

import net.minecraft.client.Minecraft;
import ru.niggaware.module.Module;

public class Coordinates extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public Coordinates() {
        super("Coordinates");
    }

    @Override
    public void onRender() {
        if (mc.player != null && mc.ingameGUI != null) {
            String coords = String.format("XYZ: %.1f, %.1f, %.1f", mc.player.posX, mc.player.posY, mc.player.posZ);
            mc.ingameGUI.drawString(mc.fontRendererObj, coords, 2, 2, 0xFFFFFF);
        }
    }
}