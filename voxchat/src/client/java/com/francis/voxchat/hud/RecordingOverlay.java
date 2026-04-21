package com.francis.voxchat.hud;

import com.francis.voxchat.VoxChatState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Draws a red ● REC badge in the top-left corner while voice recording is active.
 */
public final class RecordingOverlay {

    private RecordingOverlay() {}

    private static long blinkStart = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(RecordingOverlay::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter ticker) {
        if (!VoxChatState.isRecording()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        TextRenderer tr = mc.textRenderer;

        // Blink every 500 ms
        long now = System.currentTimeMillis();
        if (blinkStart == 0) blinkStart = now;
        boolean visible = ((now - blinkStart) % 1000) < 600;

        int x = 6;
        int y = 6;

        // Dark background pill
        ctx.fill(x - 2, y - 2, x + 68, y + 12, 0xAA000000);

        if (visible) {
            // Red dot
            ctx.drawText(tr, "●", x, y, 0xFF4444, true);
        }

        // "REC" label
        ctx.drawText(tr, " REC  [VoxChat]", x + 8, y, 0xFFFFFF, true);
    }
}
