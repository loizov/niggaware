package ru.niggaware;

import ru.niggaware.module.ModuleManager;
import ru.niggaware.command.CommandManager;
import ru.niggaware.utils.TPSUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import ru.niggaware.gui.ClickGUI;

public class NiggaWare {
    public static NiggaWare instance;
    public ModuleManager moduleManager;
    public CommandManager commandManager;
    public TPSUtils tpsUtils;
    
    private boolean guiKeyWasDown = false;

    static {
        instance = new NiggaWare();
    }
    
    private NiggaWare() {
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        tpsUtils = new TPSUtils();
        
        moduleManager.init();
        
        System.out.println("[NiggaWare] Client initialized successfully!");
    }

    public void onTick() {
        // Обновляем TPS
        tpsUtils.tick();
        
        if (moduleManager != null) moduleManager.onTick();
        
        // Клавиша для ClickGUI
        boolean keyDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (keyDown && !guiKeyWasDown) {
            Minecraft.getMinecraft().displayGuiScreen(new ClickGUI());
        }
        guiKeyWasDown = keyDown;
    }

    public void onRender() {
        if (moduleManager != null) moduleManager.onRender();
    }

	public void init() {
		// TODO Auto-generated method stub
		
	}
}