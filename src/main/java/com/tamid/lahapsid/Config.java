package com.tamid.lahapsid;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    // Bot
    public static boolean botActive = false;
    public static int botMs = 250;
    public static int botBind = -1;
    public static int botSlot = 1; // 1-9

    // Aim
    public static boolean aimActive = false;
    public static int aimRadius = 30;
    public static int aimFov = 90;
    public static boolean stickAim = true;
    public static boolean smoothAim = true;
    public static int vSpeed = 5;
    public static int hSpeed = 5;
    public static int aimBind = -1;
    public static int aimSlot = 1; // 1-9

    // Misc
    public static boolean offOnDeath = true;
    public static boolean offOnTp = true;
    public static boolean autoFirework = false;
    public static int autoFireworkBind = GLFW.GLFW_KEY_C;

    // Visuals
    public static boolean hud = true;
    public static int guiBind = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public static int guiX = -1;
    public static int guiY = -1;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("tamidlahapsid.properties");
    }

    public static void load() {
        File f = file().toFile();
        if (!f.exists()) return;
        try (FileReader r = new FileReader(f)) {
            Properties p = new Properties();
            p.load(r);
            botActive = b(p, "botActive", false);
            botMs = i(p, "botMs", 250, 50, 2000);
            botBind = i(p, "botBind", -1, -1, 1000);
            botSlot = i(p, "botSlot", 1, 1, 9);

            aimActive = b(p, "aimActive", false);
            aimRadius = i(p, "aimRadius", 30, 5, 100);
            aimFov = i(p, "aimFov", 90, 10, 360);
            stickAim = b(p, "stickAim", true);
            smoothAim = b(p, "smoothAim", true);
            vSpeed = i(p, "vSpeed", 15, 0, 50);
            hSpeed = i(p, "hSpeed", 15, 0, 50);
            aimBind = i(p, "aimBind", -1, -1, 1000);
            aimSlot = i(p, "aimSlot", 1, 1, 9);

            offOnDeath = b(p, "offOnDeath", true);
            offOnTp = b(p, "offOnTp", true);
            autoFirework = b(p, "autoFirework", false);
            autoFireworkBind = i(p, "autoFireworkBind", GLFW.GLFW_KEY_C, -1, 1000);
            hud = b(p, "hud", true);
            guiBind = i(p, "guiBind", GLFW.GLFW_KEY_RIGHT_SHIFT, 0, 1000);
            guiX = i(p, "guiX", -1, -1, 100000);
            guiY = i(p, "guiY", -1, -1, 100000);
        } catch (Exception ignored) {}
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("botActive", "" + botActive);
        p.setProperty("botMs", "" + botMs);
        p.setProperty("botBind", "" + botBind);
        p.setProperty("botSlot", "" + botSlot);
        p.setProperty("aimActive", "" + aimActive);
        p.setProperty("aimRadius", "" + aimRadius);
        p.setProperty("aimFov", "" + aimFov);
        p.setProperty("stickAim", "" + stickAim);
        p.setProperty("smoothAim", "" + smoothAim);
        p.setProperty("vSpeed", "" + vSpeed);
        p.setProperty("hSpeed", "" + hSpeed);
        p.setProperty("aimBind", "" + aimBind);
        p.setProperty("aimSlot", "" + aimSlot);
        p.setProperty("offOnDeath", "" + offOnDeath);
        p.setProperty("offOnTp", "" + offOnTp);
        p.setProperty("autoFirework", "" + autoFirework);
        p.setProperty("autoFireworkBind", "" + autoFireworkBind);
        p.setProperty("hud", "" + hud);
        p.setProperty("guiBind", "" + guiBind);
        p.setProperty("guiX", "" + guiX);
        p.setProperty("guiY", "" + guiY);
        try {
            File f = file().toFile();
            f.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(f)) { p.store(w, "Tamid Lahapsid"); }
        } catch (Exception ignored) {}
    }

    private static boolean b(Properties p, String k, boolean def) {
        return Boolean.parseBoolean(p.getProperty(k, "" + def));
    }

    private static int i(Properties p, String k, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(k, "" + def));
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) { return def; }
    }
}
