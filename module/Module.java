package ru.niggaware.module;

import net.minecraft.client.Minecraft;

public class Module {
    protected final Minecraft mc = Minecraft.getMinecraft();
    private String name;
    private boolean enabled;
    private Category category;
    
    public Module(String name) {
        this(name, Category.MISC);
    }
    
    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.enabled = false;
    }
    
    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }
    
    public void onEnable() {}
    
    public void onDisable() {}
    
    public void onTick() {}
    
    public void onRender() {}
    
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Category getCategory() {
        return category;
    }
}
