package com.tamid.lahapsid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class TamidClient implements ClientModInitializer {
    private final Map<Integer, Boolean> down = new HashMap<>();
    private long lastClick = 0;
    private long lastFirework = 0;
    private long lastFrame = System.nanoTime();
    private int targetId = -1;
    private Vec3d lastPos = null;
    private Object lastWorld = null;
    private int pendingRestoreSlot = -1;
    private int fireworkSlot = -1;

    @Override
    public void onInitializeClient() {
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(this::frame);
    }

    private boolean edge(MinecraftClient mc, int key) {
        if (key < 0) return false;
        boolean pressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
        boolean previous = down.getOrDefault(key, false);
        down.put(key, pressed);
        return pressed && !previous;
    }

    private void tick(MinecraftClient mc) {
        boolean gui = edge(mc, Config.guiBind);
        boolean bot = edge(mc, Config.botBind);
        boolean aim = edge(mc, Config.aimBind);
        boolean firework = edge(mc, Config.autoFireworkBind);

        if (mc.player == null || mc.world == null) {
            lastPos = null;
            lastWorld = null;
            pendingRestoreSlot = -1;
            return;
        }

        // Death guard: check both the dead flag and health because death/respawn timing varies.
        if (Config.offOnDeath && (mc.player.isDead() || mc.player.getHealth() <= 0.0f)) {
            turnOff(mc, "death");
        }

        // Teleport guard: world replacement catches dimension changes; distance catches same-world TP.
        Vec3d now = mc.player.getPos();
        if (Config.offOnTp && lastPos != null) {
            boolean worldChanged = lastWorld != null && lastWorld != mc.world;
            if (worldChanged || lastPos.squaredDistanceTo(now) > 36.0) {
                turnOff(mc, "teleport");
            }
        }
        lastPos = now;
        lastWorld = mc.world;

        if (mc.currentScreen != null) return;

        if (gui) { mc.setScreen(new ClickGui()); return; }
        if (bot) Config.botActive = !Config.botActive;
        if (aim) { Config.aimActive = !Config.aimActive; targetId = -1; }
        if (firework) Config.autoFirework = !Config.autoFirework;

        // Bot uses the selected 1-9 slot and waits for the vanilla attack cooldown.
        if (Config.botActive && mc.player.getInventory().selectedSlot == Config.botSlot - 1) {
            long nowMs = System.currentTimeMillis();
            boolean cooledDown = mc.player.getAttackCooldownProgress(0.0f) >= 0.90f;
            Entity target = mc.targetedEntity;
            boolean validTarget = lookingAtRealEntity(mc) && target != null
                    && mc.player.distanceTo(target) <= 3.0f;
            // Avoid early/repeated swings: wait for vanilla cooldown and only attack
            // during a falling critical window when airborne. Ground attacks remain normal.
            boolean critReady = mc.player.isOnGround() || canCriticalHit(mc.player);
            if (nowMs - lastClick >= Config.botMs && cooledDown && validTarget && critReady) {
                lastClick = nowMs;
                KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
                        mc.options.attackKey.getBoundKeyTranslationKey()));
            }
        }

        tickAutoFirework(mc);
    }

    private boolean canCriticalHit(ClientPlayerEntity player) {
        return !player.isOnGround()
                && player.fallDistance > 0.0f
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasVehicle();
    }

    private void tickAutoFirework(MinecraftClient mc) {
        if (!Config.autoFirework || !mc.player.isGliding() || mc.interactionManager == null) return;
        // Offhand-only use: never switch the selected hotbar slot away from the sword.
        if (!mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) return;
        long now = System.currentTimeMillis();
        if (now - lastFirework < 350) return;
        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        lastFirework = now;
    }

    private void turnOff(MinecraftClient mc, String reason) {
        if (Config.botActive || Config.aimActive) {
            Config.botActive = false;
            Config.aimActive = false;
            targetId = -1;
            Config.save();
            mc.player.sendMessage(Text.literal("Tamid Lahapsid: Bot and Aa turned off (" + reason + ")"), true);
        }
    }

    private boolean lookingAtRealEntity(MinecraftClient mc) {
        Entity e = mc.targetedEntity;
        if (e == null || !e.isAlive() || e.isInvisible() || e.isSpectator()) return false;
        if (e instanceof PlayerEntity p) return listed(mc, p);
        return true;
    }

    private boolean listed(MinecraftClient mc, PlayerEntity p) {
        return mc.getNetworkHandler() != null
                && mc.getNetworkHandler().getPlayerListEntry(p.getUuid()) != null;
    }

    private void frame(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long nowN = System.nanoTime();
        float dt = Math.min((nowN - lastFrame) / 1_000_000_000f, 0.1f);
        lastFrame = nowN;
        if (mc.player == null || mc.world == null) return;

        if (Config.aimActive && mc.currentScreen == null
                && mc.player.getInventory().selectedSlot == Config.aimSlot - 1) {
            aim(mc, dt);
        }

        if (Config.hud && !mc.options.hudHidden) {
            int y = 4;
            ctx.drawTextWithShadow(mc.textRenderer, "Tamid Lahapsid", 4, y, 0xFF3B82F6);
            y += 11;
            if (Config.botActive) {
                ctx.drawTextWithShadow(mc.textRenderer, "Bot [" + Config.botMs + "ms] [Slot " + Config.botSlot + "]", 4, y, 0xFFFFFFFF);
                y += 10;
            }
            if (Config.aimActive) {
                ctx.drawTextWithShadow(mc.textRenderer, "Aa [FOV " + Config.aimFov + "] [Slot " + Config.aimSlot + "]", 4, y, 0xFFFFFFFF);
                y += 10;
            }
            if (Config.autoFirework) ctx.drawTextWithShadow(mc.textRenderer, "Auto Firework", 4, y, 0xFFFFFFFF);
        }
    }

    private boolean withinFov(ClientPlayerEntity me, PlayerEntity target) {
        Vec3d toTarget = target.getBoundingBox().getCenter().subtract(me.getEyePos());
        if (toTarget.lengthSquared() < 1.0E-6) return true;
        double dot = MathHelper.clamp(me.getRotationVec(1.0f).normalize().dotProduct(toTarget.normalize()), -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= Config.aimFov / 2.0;
    }

    private boolean valid(ClientPlayerEntity me, PlayerEntity p) {
        return p != me && p.isAlive() && !p.isSpectator() && !p.isInvisible()
                && listed(MinecraftClient.getInstance(), p)
                && me.distanceTo(p) <= Config.aimRadius
                && withinFov(me, p);
    }

    private PlayerEntity pickTarget(MinecraftClient mc) {
        ClientPlayerEntity me = mc.player;

        if (Config.stickAim && targetId != -1) {
            Entity e = mc.world.getEntityById(targetId);
            if (e instanceof PlayerEntity p && valid(me, p)) return p;
            targetId = -1;
        }

        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (!valid(me, p)) continue;
            // Prefer the target closest to the crosshair, with distance as a tie-breaker.
            Vec3d direction = p.getBoundingBox().getCenter().subtract(me.getEyePos()).normalize();
            double angleScore = 1.0 - me.getRotationVec(1.0f).normalize().dotProduct(direction);
            double score = angleScore * 1000.0 + me.squaredDistanceTo(p);
            if (score < bestScore) { bestScore = score; best = p; }
        }
        targetId = best == null ? -1 : best.getId();
        return best;
    }

    private void aim(MinecraftClient mc, float dt) {
        PlayerEntity target = pickTarget(mc);
        if (target == null) return;
        ClientPlayerEntity me = mc.player;

        Vec3d eye = me.getEyePos();
        Vec3d pos = target.getBoundingBox().getCenter();
        double dx = pos.x - eye.x, dy = pos.y - eye.y, dz = pos.z - eye.z;
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float targetPitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);

        float dYaw = MathHelper.wrapDegrees(targetYaw - me.getYaw());
        float dPitch = targetPitch - me.getPitch();
        float yaw, pitch;

        if (Config.smoothAim) {
            float horizontalFactor = Config.hSpeed <= 0 ? 0.0f : MathHelper.clamp(Config.hSpeed * dt * 3.5f, 0.01f, 1.0f);
            float verticalFactor = Config.vSpeed <= 0 ? 0.0f : MathHelper.clamp(Config.vSpeed * dt * 3.5f, 0.01f, 1.0f);
            yaw = me.getYaw() + dYaw * horizontalFactor;
            pitch = me.getPitch() + dPitch * verticalFactor;
        } else {
            float maxH = Config.hSpeed * 180f * dt;
            float maxV = Config.vSpeed * 180f * dt;
            yaw = me.getYaw() + MathHelper.clamp(dYaw, -maxH, maxH);
            pitch = me.getPitch() + MathHelper.clamp(dPitch, -maxV, maxV);
        }

        pitch = MathHelper.clamp(pitch, -90f, 90f);
        me.setYaw(yaw);
        me.setPitch(pitch);
        me.prevYaw = yaw;
        me.prevPitch = pitch;
    }
}
