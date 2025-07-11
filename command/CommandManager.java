package ru.niggaware.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import ru.niggaware.NiggaWare;
import ru.niggaware.module.Module;

public class CommandManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    public boolean handleChat(String message) {
        if (!message.startsWith(".")) return false;

        String[] args = message.substring(1).split(" ");
        if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
            Module m = NiggaWare.instance.moduleManager.getModuleByName(args[1]);
            if (m != null) {
                m.toggle();
                sendMessage("Toggled " + m.getName() + ": " + (m.isEnabled() ? "ON" : "OFF"));
            } else {
                sendMessage("Module not found: " + args[1]);
            }
            return true;
        }
        return false;
    }

    private void sendMessage(String msg) {
        EntityPlayerSP player = mc.player;
        if (player != null) player.sendChatMessage("\2477[NiggaWare] \247f" + msg);
    }
}