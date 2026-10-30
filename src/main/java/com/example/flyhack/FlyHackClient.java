package com.example.flyhack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class FlyHackClient implements ClientModInitializer {
    private static final float FIXED_VERTICAL_SPEED = 0.5f;
    private static final float MIN_SPEED = 0.1f;
    private static final float MAX_SPEED = 2.5f;
    private static final float SPEED_STEP = 0.1f;
    private static final long DOUBLE_TAP_WINDOW = 500;

    private float flySpeed = 0.5f;
    private boolean flying = false;
    private long lastSpacePressTime = 0;
    private boolean prevShift = false;
    private boolean prevLeft = false;
    private boolean prevRight = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            long window = client.getWindow().getHandle();
            boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
            boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;

            if (shift && left && !prevLeft) {
                flySpeed = Math.max(MIN_SPEED, flySpeed - SPEED_STEP);
                client.player.sendMessage(Text.literal("Fly Speed: " + String.format("%.1f", flySpeed * 20.0) + " blocks/s"), true);
            }
            if (shift && right && !prevRight) {
                flySpeed = Math.min(MAX_SPEED, flySpeed + SPEED_STEP);
                client.player.sendMessage(Text.literal("Fly Speed: " + String.format("%.1f", flySpeed * 20.0) + " blocks/s"), true);
            }

            prevShift = shift;
            prevLeft = left;
            prevRight = right;

            if (client.options.jumpKey.wasPressed()) {
                long now = System.currentTimeMillis();
                if (now - lastSpacePressTime <= DOUBLE_TAP_WINDOW) {
                    flying = !flying;
                    client.player.sendMessage(Text.literal("Fly Hack: " + (flying ? "ON" : "OFF")), true);
                }
                lastSpacePressTime = now;
            }
            if (flying) {
                applyFly(client.player);
            }
        });
    }

    private void applyFly(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d look = player.getRotationVec(1.0f);

        double dx = 0, dy = 0, dz = 0;
        if (client.options.jumpKey.isPressed()) dy += FIXED_VERTICAL_SPEED;
        if (client.options.sneakKey.isPressed()) dy -= FIXED_VERTICAL_SPEED;
        if (client.options.forwardKey.isPressed()) {
            dx += look.x * flySpeed;
            dz += look.z * flySpeed;
        }
        if (client.options.backKey.isPressed()) {
            dx -= look.x * flySpeed;
            dz -= look.z * flySpeed;
        }
        if (client.options.leftKey.isPressed()) {
            dx += look.z * flySpeed;
            dz -= look.x * flySpeed;
        }
        if (client.options.rightKey.isPressed()) {
            dx -= look.z * flySpeed;
            dz += look.x * flySpeed;
        }

        player.setVelocity(dx, dy, dz);
        player.fallDistance = 0;
        player.setOnGround(false);
    }
}
