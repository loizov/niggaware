package ru.niggaware.module;
import ru.niggaware.module.Watermark;

import java.util.ArrayList;
import java.util.List;
import ru.niggaware.module.Sprint;
import ru.niggaware.module.Coordinates;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        modules.add(new Sprint());
        modules.add(new Coordinates());
        modules.add(new Watermark());
        
        // Combat
        modules.add(new KillAura());
        modules.add(new AntiKnockback());
        
        // Movement
        modules.add(new Flight());
        modules.add(new Speed());
        
        // Render
        modules.add(new ESP());
        modules.add(new ItemESP());
        
        // Player
        modules.add(new Scaffold());
        
        // Misc
        modules.add(new Lagometer());
        
        System.out.println("[NiggaWare] Initialized " + modules.size() + " modules");
    }

    public List<Module> getModules() { return modules; }

    public Module getModuleByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void onTick() {
        for (Module m : modules)
            if (m.isEnabled()) m.onTick();
    }

    public void onRender() {
        for (Module m : modules)
            if (m.isEnabled()) m.onRender();
    }
}
