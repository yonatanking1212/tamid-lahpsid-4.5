package com.tamid.lahapsid;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGui extends Screen {
    static final int W = 360, H = 260;
    static final int BG = 0xF00A1226, BORDER = 0xFF2563EB, SEL = 0xFF1D4ED8,
            ON = 0xFF2563EB, OFF = 0xFF1E2A44, TXT = 0xFFE6ECFF, DIM = 0xFF8A96B5;
    final String[] tabNames = {"Bot", "Aa", "Visuals", "Misc"};
    final List<List<Widget>> pages = new ArrayList<>();
    int tab = 0, px, py;
    Widget dragging;
    Bind listening;
    boolean draggingPanel;
    int panelOffsetX, panelOffsetY;

    public ClickGui() { super(Text.literal("Tamid Lahapsid")); }

    @Override
    protected void init() {
        px = Config.guiX >= 0 ? Config.guiX : (width - W) / 2;
        py = Config.guiY >= 0 ? Config.guiY : (height - H) / 2;
        px = MathHelper.clamp(px, 0, Math.max(0, width - W));
        py = MathHelper.clamp(py, 0, Math.max(0, height - H));

        pages.clear();
        int x = px + 112, w = W - 112 - 16, y = py + 44;

        List<Widget> bot = new ArrayList<>();
        bot.add(new Toggle("Activated", x, y, w, () -> Config.botActive, v -> Config.botActive = v));
        bot.add(new Slider("Delay (ms)", x, y + 22, w, 50, 2000, 10, () -> Config.botMs, v -> Config.botMs = v));
        bot.add(new Slider("Hotbar Slot", x, y + 44, w, 1, 9, 1, () -> Config.botSlot, v -> Config.botSlot = v));
        bot.add(new Bind("Bot Bind", x, y + 66, w, () -> Config.botBind, v -> Config.botBind = v, true));
        pages.add(bot);

        List<Widget> aa = new ArrayList<>();
        aa.add(new Toggle("Activated", x, y, w, () -> Config.aimActive, v -> Config.aimActive = v));
        aa.add(new Slider("Radius", x, y + 22, w, 5, 100, 1, () -> Config.aimRadius, v -> Config.aimRadius = v));
        aa.add(new Slider("FOV", x, y + 44, w, 10, 360, 5, () -> Config.aimFov, v -> Config.aimFov = v));
        aa.add(new Toggle("Smooth Mode (Blatant)", x, y + 66, w, () -> Config.smoothAim, v -> Config.smoothAim = v));
        aa.add(new Toggle("Stick Aim", x, y + 88, w, () -> Config.stickAim, v -> Config.stickAim = v));
        aa.add(new Slider("Vertical Speed", x, y + 110, w, 0, 50, 1, () -> Config.vSpeed, v -> Config.vSpeed = v));
        aa.add(new Slider("Horizontal Speed", x, y + 132, w, 0, 50, 1, () -> Config.hSpeed, v -> Config.hSpeed = v));
        aa.add(new Slider("Hotbar Slot", x, y + 154, w, 1, 9, 1, () -> Config.aimSlot, v -> Config.aimSlot = v));
        aa.add(new Bind("Aim Bind", x, y + 176, w, () -> Config.aimBind, v -> Config.aimBind = v, true));
        pages.add(aa);

        List<Widget> vis = new ArrayList<>();
        vis.add(new Toggle("HUD", x, y, w, () -> Config.hud, v -> Config.hud = v));
        vis.add(new Bind("GUI Open Bind", x, y + 24, w, () -> Config.guiBind, v -> Config.guiBind = v, false));
        pages.add(vis);

        List<Widget> misc = new ArrayList<>();
        misc.add(new Toggle("Off on Death", x, y, w, () -> Config.offOnDeath, v -> Config.offOnDeath = v));
        misc.add(new Toggle("Off on Teleport", x, y + 22, w, () -> Config.offOnTp, v -> Config.offOnTp = v));
        misc.add(new Toggle("Auto Firework", x, y + 44, w, () -> Config.autoFirework, v -> Config.autoFirework = v));
        misc.add(new Bind("Firework Bind", x, y + 66, w, () -> Config.autoFireworkBind, v -> Config.autoFireworkBind = v, true));
        pages.add(misc);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x66000000);
        c.fill(px, py, px + W, py + H, BG);
        c.fill(px + 1, py + 1, px + W - 1, py + 27, 0xFF111D36);
        c.drawBorder(px, py, W, H, BORDER);
        c.drawText(textRenderer, "TAMID LAHAPSID", px + 12, py + 9, TXT, false);
        c.drawText(textRenderer, "v1.1", px + 118, py + 9, DIM, false);
        c.fill(px + W - 24, py + 5, px + W - 7, py + 21, 0xFF263653);
        c.drawText(textRenderer, "x", px + W - 18, py + 9, TXT, false);
        c.fill(px, py + 27, px + W, py + 28, BORDER);

        // Sidebar and content surface give the client a cleaner, more structured layout.
        c.fill(px + 7, py + 34, px + 101, py + H - 9, 0xCC101B31);
        c.fill(px + 108, py + 34, px + W - 8, py + H - 9, 0x66131F36);
        for (int i = 0; i < tabNames.length; i++) {
            int ty = py + 40 + i * 27;
            c.fill(px + 12, ty, px + 96, ty + 21, i == tab ? SEL : 0x00131F36);
            if (i == tab) c.fill(px + 12, ty, px + 15, ty + 21, 0xFF60A5FA);
            c.drawText(textRenderer, tabNames[i], px + 23, ty + 7, i == tab ? 0xFFFFFFFF : DIM, false);
        }
        c.fill(px + 105, py + 34, px + 106, py + H - 9, 0xFF263653);
        for (Widget wd : pages.get(tab)) {
            c.fill(wd.x - 5, wd.y + 20, wd.x + wd.w + 5, wd.y + 21, 0x332F4770);
            wd.render(c);
        }
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float d) { }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) { listening = null; return true; }
        if (button != 0) return true;
        if (mx >= px + W - 22 && mx <= px + W - 6 && my >= py + 4 && my <= py + 22) {
            close();
            return true;
        }
        // The whole title bar remains draggable; no instructional text is needed.
        if (mx >= px && mx <= px + W - 24 && my >= py && my <= py + 27) {
            draggingPanel = true;
            panelOffsetX = (int) mx - px;
            panelOffsetY = (int) my - py;
            return true;
        }
        for (int i = 0; i < tabNames.length; i++) {
            int ty = py + 36 + i * 22;
            if (mx >= px + 8 && mx <= px + 96 && my >= ty && my <= ty + 18) {
                tab = i;
                return true;
            }
        }
        for (Widget wd : pages.get(tab)) if (wd.click(mx, my)) return true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingPanel) {
            int newX = MathHelper.clamp((int) mx - panelOffsetX, 0, Math.max(0, width - W));
            int newY = MathHelper.clamp((int) my - panelOffsetY, 0, Math.max(0, height - H));
            int shiftX = newX - px, shiftY = newY - py;
            px = newX;
            py = newY;
            for (List<Widget> page : pages) {
                for (Widget widget : page) {
                    widget.x += shiftX;
                    widget.y += shiftY;
                }
            }
            return true;
        }
        if (dragging != null) { dragging.drag(mx); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        draggingPanel = false;
        Config.guiX = px;
        Config.guiY = py;
        Config.save();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (listening.canClear) listening.set.accept(-1);
            } else listening.set.accept(key);
            listening = null;
            return true;
        }
        if (key == Config.guiBind) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void removed() {
        Config.guiX = px;
        Config.guiY = py;
        Config.save();
    }
    @Override public boolean shouldPause() { return false; }

    abstract class Widget {
        String label;
        int x, y, w;
        Widget(String l, int x, int y, int w) { label = l; this.x = x; this.y = y; this.w = w; }
        abstract void render(DrawContext c);
        boolean click(double mx, double my) { return false; }
        void drag(double mx) {}
        boolean inRow(double mx, double my) { return mx >= x && mx <= x + w && my >= y && my < y + 18; }
    }

    class Toggle extends Widget {
        BooleanSupplier get;
        Consumer<Boolean> set;
        Toggle(String l, int x, int y, int w, BooleanSupplier g, Consumer<Boolean> s) {
            super(l, x, y, w); get = g; set = s;
        }
        void render(DrawContext c) {
            c.drawText(textRenderer, label, x, y + 4, TXT, false);
            int sx = x + w - 28;
            boolean on = get.getAsBoolean();
            c.fill(sx, y + 2, sx + 28, y + 16, on ? ON : OFF);
            int k = on ? sx + 16 : sx + 2;
            c.fill(k, y + 4, k + 10, y + 14, 0xFFFFFFFF);
        }
        boolean click(double mx, double my) {
            if (mx >= x + w - 30 && mx <= x + w && my >= y && my < y + 18) {
                set.accept(!get.getAsBoolean());
                return true;
            }
            return false;
        }
    }

    class Slider extends Widget {
        int min, max, step;
        IntSupplier get;
        IntConsumer set;
        Slider(String l, int x, int y, int w, int min, int max, int step, IntSupplier g, IntConsumer s) {
            super(l, x, y, w); this.min = min; this.max = max; this.step = step; get = g; set = s;
        }
        int tx() { return x + 112; }
        int tw() { return w - 112 - 42; }
        void render(DrawContext c) {
            c.drawText(textRenderer, label, x, y + 4, TXT, false);
            c.fill(tx(), y + 8, tx() + tw(), y + 11, OFF);
            float f = (get.getAsInt() - min) / (float) (max - min);
            int kx = tx() + Math.round(f * tw());
            c.fill(tx(), y + 8, kx, y + 11, ON);
            c.fill(kx - 3, y + 4, kx + 3, y + 15, 0xFFFFFFFF);
            c.drawText(textRenderer, "" + get.getAsInt(), x + w - 34, y + 4, TXT, false);
        }
        boolean click(double mx, double my) {
            if (inRow(mx, my) && mx >= tx() - 4 && mx <= tx() + tw() + 4) {
                dragging = this;
                drag(mx);
                return true;
            }
            return false;
        }
        void drag(double mx) {
            double f = MathHelper.clamp((mx - tx()) / tw(), 0, 1);
            int v = (int) Math.round((min + f * (max - min)) / (double) step) * step;
            set.accept(MathHelper.clamp(v, min, max));
        }
    }

    class Bind extends Widget {
        IntSupplier get;
        IntConsumer set;
        boolean canClear;
        Bind(String l, int x, int y, int w, IntSupplier g, IntConsumer s, boolean canClear) {
            super(l, x, y, w); get = g; set = s; this.canClear = canClear;
        }
        void render(DrawContext c) {
            c.drawText(textRenderer, label, x, y + 4, TXT, false);
            int bx = x + w - 82;
            c.fill(bx, y + 1, bx + 82, y + 16, listening == this ? SEL : OFF);
            c.drawBorder(bx, y + 1, 82, 15, BORDER);
            String value;
            if (listening == this) value = "...";
            else if (get.getAsInt() < 0) value = "None";
            else value = InputUtil.fromKeyCode(get.getAsInt(), 0).getLocalizedText().getString();
            if (value.length() > 12) value = value.substring(0, 12);
            c.drawText(textRenderer, value, bx + 41 - textRenderer.getWidth(value) / 2, y + 5, TXT, false);
        }
        boolean click(double mx, double my) {
            if (mx >= x + w - 82 && mx <= x + w && my >= y && my < y + 18) {
                listening = this;
                return true;
            }
            return false;
        }
    }
}
