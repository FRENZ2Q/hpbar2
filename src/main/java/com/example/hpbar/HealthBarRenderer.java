package com.example.hpbar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;

public final class HealthBarRenderer {
    private static final int LIGHT = 0xF000F0; // полная яркость
    private static final float BAR_W = 40f;
    private static final float BAR_H = 5f;

    private HealthBarRenderer() {}

    public static void render(LivingEntity e, MatrixStack matrices, VertexConsumerProvider vcp) {
        HpBarConfig c = HpBarConfig.get();
        if (!c.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e.isDead() || e.getHealth() <= 0f) return;
        if (!c.shouldShow(e, mc)) return;

        double maxSq = (double) c.maxDistance * c.maxDistance;
        if (mc.gameRenderer.getCamera().getPos().squaredDistanceTo(e.getPos()) > maxSq) return;

        float hp = e.getHealth();
        float max = Math.max(e.getMaxHealth(), 0.001f);
        float ratio = Math.min(1f, Math.max(0f, hp / max));

        matrices.push();
        // над головой, под ником (ник рисуется на height+0.5)
        matrices.translate(0.0, e.getHeight() + 0.25, 0.0);
        matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
        float s = 0.025f * c.scale;
        matrices.scale(s, -s, s);

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(c.throughWalls
            ? RenderLayer.getTextBackgroundSeeThrough()
            : RenderLayer.getTextBackground());

        float x1 = -BAR_W / 2f;
        float x2 = BAR_W / 2f;

        // рамка, фон, заполнение (z растёт в сторону камеры)
        rect(vc, m, x1 - 1, -1, x2 + 1, BAR_H + 1, 0.0f, 0xCC000000);
        rect(vc, m, x1, 0, x2, BAR_H, 0.1f, 0xFF3A1010);
        rect(vc, m, x1, 0, x1 + BAR_W * ratio, BAR_H, 0.2f, healthColor(ratio));

        if (c.showText) {
            String text = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
            TextRenderer tr = mc.textRenderer;
            tr.draw(text, x2 + 3f, -1.5f, 0xFFFFFFFF, false, m, vcp,
                c.throughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                0, LIGHT);
        }

        matrices.pop();
    }

    /** зелёный -> жёлтый -> красный */
    private static int healthColor(float ratio) {
        int r, g;
        if (ratio > 0.5f) {
            r = (int) (255 * (1f - ratio) * 2f);
            g = 255;
        } else {
            r = 255;
            g = (int) (255 * ratio * 2f);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private static void rect(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, int argb) {
        int a = (argb >>> 24) & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        vc.vertex(m, x1, y2, z).color(r, g, b, a).light(LIGHT);
        vc.vertex(m, x2, y2, z).color(r, g, b, a).light(LIGHT);
        vc.vertex(m, x2, y1, z).color(r, g, b, a).light(LIGHT);
        vc.vertex(m, x1, y1, z).color(r, g, b, a).light(LIGHT);
    }
}
