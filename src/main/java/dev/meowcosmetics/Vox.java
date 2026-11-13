package dev.meowcosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Coloured boxes and pixel-art "voxel" shapes, drawn with a plain white texture and vertex colours. */
final class Vox {
    private final PoseStack.Pose p;
    private final VertexConsumer vc;
    private final int light;

    Vox(PoseStack.Pose p, VertexConsumer vc, int light) { this.p = p; this.vc = vc; this.light = light; }

    /** A box from (x0,y0,z0) to (x1,y1,z1), in whatever units the pose is in. */
    void box(float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        face(argb, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        face(argb, 0, 0, 1, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        face(shade(argb, 0.86f), -1, 0, 0, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        face(shade(argb, 0.86f), 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        face(argb, 0, -1, 0, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        face(argb, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    /**
     * A pixel-art shape: one box per character (rows top to bottom, '.' = empty), [px] wide, [depth] deep,
     * centred on x. [down]: rows go towards +y (model space); otherwise towards -y (world space, y up).
     */
    void pattern(String[] rows, String keys, int[] colors, float px, float z0, float depth, boolean down) {
        int h = rows.length;
        for (int r = 0; r < h; r++) {
            String row = rows[r];
            float w = row.length();
            for (int c = 0; c < row.length(); c++) {
                int k = keys.indexOf(row.charAt(c));
                if (k < 0) continue;
                float x = (c - w / 2f) * px, y = down ? r * px : (h - 1 - r) * px;
                box(x, y, z0, x + px, y + px, z0 + depth, colors[k]);
            }
        }
    }

    private void face(int c, float nx, float ny, float nz, float... v) {
        for (int i = 0; i < 12; i += 3)
            vc.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(c).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 255) * f)), g = Math.min(255, (int) (((argb >> 8) & 255) * f)), b = Math.min(255, (int) ((argb & 255) * f));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    static int alpha(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
}
