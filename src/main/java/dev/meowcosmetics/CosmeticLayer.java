package dev.meowcosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Cat ears, tail, aura and pet on your own player. Pixel-style 3D, drawn from coloured boxes. */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float PX = 1 / 16f;
    private static final int GLOW = 0xF000F0;

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState s, float yRot, float xRot) {
        if (!(s instanceof Cosmetics.Me me) || me.meowcosmetics$look() == null || s.isInvisible) return;
        Cosmetics.Look l = me.meowcosmetics$look();
        RenderType solid = RenderTypes.entityCutout(Cosmetics.white()), see = RenderTypes.entityTranslucent(Cosmetics.white());
        float catT = Cosmetics.time(s, l.catSpeed());
        if (l.ears()) ears(ps, out, solid, light, Cosmetics.FUR[Math.floorMod(l.earColor(), Cosmetics.FUR.length)], catT);
        if (l.tail()) tail(ps, out, solid, light, s, Cosmetics.FUR[Math.floorMod(l.tailColor(), Cosmetics.FUR.length)], catT);
        if (!l.aura() && !l.pet()) return;
        ps.pushPose();
        ps.translate(0, 1.501f, 0);   // undo the model flip: y up, feet at 0, facing where your body faces
        ps.scale(-1, -1, 1);
        if (l.pet()) pet(ps, out, solid, light, Math.floorMod(l.petStyle(), Cosmetics.PETS.length), Cosmetics.time(s, l.petSpeed()));
        if (l.aura()) {
            ps.mulPose(Axis.YP.rotationDegrees(-(180 - s.bodyRot)));   // world-aligned, so the aura doesn't spin when you turn
            float t = Cosmetics.time(s, l.auraSpeed()), amt = Math.max(5, l.auraAmount()) / 75f;
            switch (Math.floorMod(l.auraStyle(), 4)) {
                case 0 -> bats(ps, out, solid, light, t, Math.max(1, Math.round(7 * amt)));
                case 1 -> hearts(ps, out, see, t, Math.max(1, Math.round(8 * amt)));
                case 2 -> sparkles(ps, out, see, t, Math.max(1, Math.round(12 * amt)));
                default -> cats(ps, out, solid, light, t, Math.max(1, Math.round(7 * amt)));
            }
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ cat ears

    private static final String[] EAR = {
        "..O..",
        ".OIO.",
        ".OIO.",
        "OIIIO",
        "OIIIO"};

    private void ears(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, int[] fur, float t) {
        float cyc = t % 4.3f, cyc2 = (t + 2.1f) % 5.1f;   // a quick flick every few seconds, one ear at a time
        float flickL = cyc < 0.25f ? (float) Math.sin(cyc / 0.25f * Math.PI) * 22 : 0;
        float flickR = cyc2 < 0.25f ? (float) Math.sin(cyc2 / 0.25f * Math.PI) * 22 : 0;
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            getParentModel().head.translateAndRotate(ps);
            var f = Cosmetics.cfg();
            ps.translate(side * (2.5f + f.earSpread / 10f) * PX, (-7.6f - f.earHeight / 10f) * PX, (-0.8f - f.earForward / 10f) * PX);
            ps.mulPose(Axis.ZP.rotationDegrees(side * f.earTilt));
            ps.mulPose(Axis.XP.rotationDegrees(-(side < 0 ? flickL : flickR) - 5));
            float v = 0.82f * PX * Math.max(20, f.earSize) / 100f;   // no mirroring: a flipped ear would be lit darker
            ps.translate(0, -EAR.length * v, 0);   // rows go down from the tip to the base on the head
            int[] col = {fur[0], fur[0]};
            out.submitCustomGeometry(ps, rt, (pose, vc) -> {
                Vox x = new Vox(pose, vc, light);
                x.pattern(EAR, "OI", col, v, 0, v * 1.3f, true);
                x.pattern(EAR, "I", new int[]{fur[1]}, v, -0.25f * v, 0.25f * v, true);
            });
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ cat tail

    private void tail(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, AvatarRenderState s, int[] fur, float t) {
        var f = Cosmetics.cfg();
        int n = Math.max(3, Math.min(20, f.tailLength));
        // a steady swish; walking adds a swing that follows your steps (never changes the swish speed, so no twitching)
        float walk = Math.min(1, s.walkAnimationSpeed), step = s.walkAnimationPos * 0.6662f;
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        ps.translate(0, (10.6f - f.tailHeight / 10f) * PX, 2.1f * PX);
        ps.mulPose(Axis.YP.rotationDegrees(f.tailSide));
        ps.mulPose(Axis.XP.rotationDegrees(-24 + walk * 10));
        float curl = f.tailCurl * 0.28f;
        for (int i = 0; i < n; i++) {
            float k = i / (float) (n - 1);
            float sway = (float) Math.sin(t * 2.4f - i * 0.42f) * (2.5f + 4 * k) + walk * (float) Math.sin(step - i * 0.4f) * (3 + 5 * k);
            ps.mulPose(Axis.YP.rotationDegrees(sway));
            ps.mulPose(Axis.XP.rotationDegrees(i < 2 ? 0 : curl + (float) Math.sin(t * 1.3f - i * 0.3f) * 2));
            float w = (1.9f - 0.5f * k) * PX, len = 1.5f * PX;
            int color = i >= n - 2 ? fur[2] : (i % 4 == 3 ? Vox.shade(fur[0], 0.92f) : fur[0]);
            out.submitCustomGeometry(ps, rt, (pose, vc) -> new Vox(pose, vc, light).box(-w / 2, -w / 2, 0, w / 2, w / 2, len + 0.2f * PX, color));
            ps.translate(0, 0, len);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ bats

    private static final String[] WING = {
        "........O",
        "O.....OOO",
        "OOOOOOOOO",
        "OOMOOMOOO",
        "OO.OO.OO.",
        "O..O..O.."};

    /** Pixel bats facing outwards with their wings spread, circling you. */
    private void bats(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, float t, int count) {
        for (int i = 0; i < count; i++) {
            float sp = 0.9f + (i % 3) * 0.25f, dir = i % 2 == 0 ? 1 : -1;
            float a = t * sp * dir + i * 0.9f;
            float r = 0.85f + 0.2f * (float) Math.sin(t * 0.7f + i * 1.3f);
            float h = 0.3f + (i * 0.27f) % 1.7f + 0.2f * (float) Math.sin(t * 1.9f + i);
            ps.pushPose();
            ps.translate(Math.cos(a) * r, h, Math.sin(a) * r);
            ps.mulPose(Axis.YP.rotation((float) (Math.PI / 2 - a)));   // face outwards
            ps.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 3 + i) * 10 * dir));
            ps.scale(0.62f, 0.62f, 0.62f);
            float flap = (float) Math.sin(t * 14 + i * 2);
            out.submitCustomGeometry(ps, rt, (pose, vc) -> {
                Vox v = new Vox(pose, vc, light);
                v.box(-1.3f * PX, -1.8f * PX, -1.1f * PX, 1.3f * PX, 1.4f * PX, 1.1f * PX, 0xFF2A1E2F);   // body
                v.box(-1.5f * PX, 1.4f * PX, -1.2f * PX, 1.5f * PX, 3.7f * PX, 1.3f * PX, 0xFF34263C);   // head
                v.box(-1.5f * PX, 3.7f * PX, -0.4f * PX, -0.6f * PX, 5.0f * PX, 0.5f * PX, 0xFF2A1E2F);   // ears
                v.box(0.6f * PX, 3.7f * PX, -0.4f * PX, 1.5f * PX, 5.0f * PX, 0.5f * PX, 0xFF2A1E2F);
                v.box(-0.5f * PX, -2.6f * PX, -0.4f * PX, -0.1f * PX, -1.8f * PX, 0.4f * PX, 0xFF1C141F);   // feet
                v.box(0.1f * PX, -2.6f * PX, -0.4f * PX, 0.5f * PX, -1.8f * PX, 0.4f * PX, 0xFF1C141F);
            });
            out.submitCustomGeometry(ps, rt, (pose, vc) -> {
                Vox v = new Vox(pose, vc, GLOW);
                v.box(-1.1f * PX, 2.4f * PX, 1.3f * PX, -0.3f * PX, 3.1f * PX, 1.45f * PX, 0xFFFF3B47);   // eyes
                v.box(0.3f * PX, 2.4f * PX, 1.3f * PX, 1.1f * PX, 3.1f * PX, 1.45f * PX, 0xFFFF3B47);
            });
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 1.2f * PX, 0.9f * PX, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(side * (10 + flap * 42)));   // flap up and down
                ps.mulPose(Axis.YP.rotationDegrees(side * flap * -18));
                int sd = side;
                out.submitCustomGeometry(ps, rt, (pose, vc) -> {
                    Vox v = new Vox(pose, vc, light);
                    float q = 0.95f * PX;
                    for (int row = 0; row < WING.length; row++) for (int col = 0; col < WING[row].length(); col++) {
                        char ch = WING[row].charAt(col);
                        if (ch == '.') continue;
                        float xa = sd * col * q, xb = sd * (col + 1) * q, y0 = (2 - row) * q;
                        v.box(Math.min(xa, xb), y0, -0.2f * PX, Math.max(xa, xb), y0 + q, 0.2f * PX, ch == 'M' ? 0xFF4F3758 : 0xFF3A2942);
                    }
                });
                ps.popPose();
            }
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ hearts

    private static final String[] HEART = {
        ".DD.DD.",
        "DWLDAAD",
        "DLAAAAD",
        "DAAAAAD",
        ".DAAAD.",
        "..DAD..",
        "...D..."};
    private static final int[] HEART_TINTS = {0xFF5C9A, 0xFF3B5C, 0xC86BFF, 0xFF8F6B, 0xFF7FC8};

    private void hearts(PoseStack ps, SubmitNodeCollector out, RenderType rt, float t, int count) {
        for (int i = 0; i < count; i++) {
            float life = ((t * 0.32f + i / (float) count) % 1f);
            float a = i * 2.399f + t * 0.6f + life * 2.2f, r = 0.72f + 0.12f * (float) Math.sin(i * 2.1f);
            float alpha = Math.min(1, life * 5) * Math.min(1, (1 - life) * 3.5f) * 0.95f;
            if (alpha <= 0.02f) continue;
            float sc = 0.8f + 0.2f * (float) Math.sin(t * 4 + i);
            ps.pushPose();
            ps.translate(Math.cos(a) * r, 0.15f + life * 2.1f, Math.sin(a) * r);
            ps.mulPose(Axis.YP.rotation(t * 1.4f + i));
            ps.scale(sc * 0.65f, sc * 0.65f, sc * 0.65f);
            int main = 0xFF000000 | HEART_TINTS[i % HEART_TINTS.length];
            int[] col = {Vox.alpha(Vox.shade(main, 0.62f), alpha), Vox.alpha(CapeArt.mix(main, 0xFFFFFFFF, 0.42f), alpha),
                Vox.alpha(main, alpha), Vox.alpha(0xFFFFFF, alpha)};
            out.submitCustomGeometry(ps, rt, (pose, vc) -> new Vox(pose, vc, GLOW).pattern(HEART, "DLAW", col, PX, -0.5f * PX, PX, false));
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ sparkles

    private static final String[] STAR = {
        "..A..",
        "..A..",
        "AAWAA",
        "..A..",
        "..A.."};

    private void sparkles(PoseStack ps, SubmitNodeCollector out, RenderType rt, float t, int count) {
        int[] tints = {0xFFFFFF, 0x9DF3FF, 0xFFB3D9, 0xFFE38A};
        for (int i = 0; i < count; i++) {
            float tw = (float) Math.sin(t * (2.2f + (i % 4) * 0.4f) + i * 1.7f);
            if (tw <= 0) continue;
            float a = t * (0.35f + (i % 3) * 0.12f) * (i % 2 == 0 ? 1 : -1) + i * 0.52f;
            float r = 0.65f + (i % 4) * 0.12f, h = 0.2f + (i * 0.37f) % 1.9f + 0.1f * (float) Math.sin(t + i);
            ps.pushPose();
            ps.translate(Math.cos(a) * r, h, Math.sin(a) * r);
            ps.mulPose(Axis.YP.rotation(t * 2 + i));
            ps.mulPose(Axis.ZP.rotationDegrees(45 * (i % 2)));
            float sc = 0.25f + 0.35f * tw;
            ps.scale(sc, sc, sc);
            int[] col = {Vox.alpha(tints[i % 4], 0.85f * tw), Vox.alpha(0xFFFFFF, tw)};
            out.submitCustomGeometry(ps, rt, (pose, vc) -> new Vox(pose, vc, GLOW).pattern(STAR, "AW", col, PX, -0.5f * PX, PX, false));
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ cat heads

    /** Little cat heads (every coat) bobbing around you, looking outwards. */
    private void cats(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, float t, int count) {
        for (int i = 0; i < count; i++) {
            float a = t * 0.45f * (i % 2 == 0 ? 1 : -1) + i * (6.283f / count);
            float r = 0.85f + 0.1f * (float) Math.sin(t * 0.8f + i);
            float h = 0.35f + (i * 0.41f) % 1.6f + 0.12f * (float) Math.sin(t * 1.6f + i * 1.3f);
            ps.pushPose();
            ps.translate(Math.cos(a) * r, h, Math.sin(a) * r);
            ps.mulPose(Axis.YP.rotation((float) (-Math.PI / 2 - a)));   // face outwards
            ps.mulPose(Axis.ZP.rotationDegrees(12 * (float) Math.sin(t * 1.2f + i * 2)));
            ps.scale(0.72f, 0.72f, 0.72f);
            head(ps, out, rt, light, i % Cosmetics.PETS.length, t + i * 1.7f, true);
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ the pet

    /** Colours of one coat: base, second (stripes / patches / points), light (belly, muzzle), eyes. */
    private static final int[][] COATS = {
        {0xFFE38B3E, 0xFFB8652A, 0xFFF6C99A, 0xFF7CF29A},   // ginger tabby
        {0xFF1E1D24, 0xFF1E1D24, 0xFFF1EFEA, 0xFFFFD54A},   // tuxedo
        {0xFF8E9099, 0xFF5E5F67, 0xFFDCDCE2, 0xFFC8F27C},   // grey tabby
        {0xFFF3EFE8, 0xFFE38B3E, 0xFF2A2830, 0xFF7CF29A},   // calico (white, orange, black)
        {0xFFEADBC4, 0xFF4A3428, 0xFFF3E8D6, 0xFF6EC6FF},   // siamese
        {0xFF1E1D24, 0xFF26252E, 0xFF2C2B33, 0xFFFFD54A},   // black
        {0xFFF1EFEA, 0xFFE4E0DA, 0xFFFFFFFF, 0xFF6EC6FF},   // white
    };

    private static float noise(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** A little fur texture: each voxel a touch lighter or darker. */
    private static int fur(int c, int i, int j, int k) { return Vox.shade(c, 0.9f + 0.14f * noise(i * 3 + 1, j * 5 + 2, k * 7 + 3)); }

    /** Body voxel colour: i across, j up (0 = belly), k front to back. */
    private static int bodyColor(int coat, int i, int j, int k) {
        int[] c = COATS[coat];
        return switch (coat) {
            case 0, 2 -> j == 0 || (k == 0 && j <= 1) ? c[2] : (k % 2 == 1 && j >= 1 ? c[1] : c[0]);
            case 1 -> j == 0 || (k == 0 && j <= 1) ? c[2] : c[0];
            case 3 -> { if (j == 0) yield c[0]; float n = noise(i / 2, j, k / 2); yield n < 0.32f ? c[1] : n < 0.52f ? c[2] : c[0]; }
            case 4 -> k >= 6 ? CapeArt.mix(c[0], c[1], 0.35f) : c[0];
            case 5 -> j == 2 ? c[1] : c[0];
            default -> c[0];
        };
    }

    /** Head voxel colour: i across (0..4), j up (0..3), k front to back (0 = face). */
    private static int headColor(int coat, int i, int j, int k) {
        int[] c = COATS[coat];
        boolean muzzle = k == 0 && j <= 1 && i >= 1 && i <= 3;
        return switch (coat) {
            case 0, 2 -> muzzle ? c[2] : (j == 3 && (i == 1 || i == 3)) || (j == 2 && (i == 0 || i == 4) && k <= 1) ? c[1] : c[0];
            case 1 -> muzzle || (k == 0 && j == 0) ? c[2] : c[0];
            case 3 -> j >= 2 && i <= 1 ? c[1] : j >= 2 && i >= 3 ? c[2] : c[0];
            case 4 -> k == 0 ? (j >= 3 ? CapeArt.mix(c[0], c[1], 0.4f) : c[1]) : c[0];
            case 6 -> muzzle ? c[2] : c[0];
            default -> c[0];
        };
    }

    /** A cat head with ears, eyes, nose; [blush] for the aura's extra cute ones. */
    private void head(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, int coat, float t, boolean blush) {
        int[] c = COATS[coat];
        int earCol = coat == 4 || coat == 3 ? c[1] : c[0], inner = coat == 1 || coat == 5 ? 0xFF8E5E6E : 0xFFF2A7BA;
        out.submitCustomGeometry(ps, rt, (pose, vc) -> {
            Vox v = new Vox(pose, vc, light);
            for (int i = 0; i < 5; i++) for (int j = 0; j < 4; j++) for (int k = 0; k < 4; k++)
                v.box((i - 2.5f) * PX, (j - 0.5f) * PX, (k - 4) * PX, (i - 1.5f) * PX, (j + 0.5f) * PX, (k - 3) * PX, fur(headColor(coat, i, j, k), i + 11, j, k));
            for (int e = -1; e <= 1; e += 2) {
                v.box((e * 1.6f - 0.8f) * PX, 3.5f * PX, -2.8f * PX, (e * 1.6f + 0.8f) * PX, 4.8f * PX, -1.5f * PX, earCol);
                v.box((e * 1.6f - 0.4f) * PX, 3.55f * PX, -2.9f * PX, (e * 1.6f + 0.4f) * PX, 4.4f * PX, -2.75f * PX, inner);
            }
            v.box(-0.4f * PX, 0.6f * PX, -4.12f * PX, 0.4f * PX, 1.1f * PX, -3.98f * PX, 0xFFF08AA6);   // nose
            if (blush) for (int e = -1; e <= 1; e += 2) v.box((e * 1.9f - 0.4f) * PX, 0.7f * PX, -4.1f * PX, (e * 1.9f + 0.4f) * PX, 1.1f * PX, -3.98f * PX, 0xFFFF9FBF);
        });
        boolean blink = (t % 3.7f) < 0.13f;
        out.submitCustomGeometry(ps, rt, (pose, vc) -> {
            Vox v = new Vox(pose, vc, GLOW);
            for (int e = -1; e <= 1; e += 2) {
                v.box((e * 1.0f - 0.45f) * PX, (blink ? 1.85f : 1.4f) * PX, -4.1f * PX, (e * 1.0f + 0.45f) * PX, 2.3f * PX, -4.0f * PX, blink ? Vox.shade(c[0], 0.7f) : c[3]);
                if (!blink) v.box((e * 1.0f - 0.15f) * PX, 1.5f * PX, -4.13f * PX, (e * 1.0f + 0.15f) * PX, 2.2f * PX, -4.08f * PX, 0xFF15141A);   // pupils
            }
        });
    }

    // Slime pets: sprites from Lin-ain's "Slime" pack on itch.io (free to use), with faces added, made 3D here
    private static final String[] SLIMES = {"green", "berry", "ice", "red", "orange", "tuxedo_cat"};
    private static final java.util.Map<String, int[][]> SPRITES = new java.util.HashMap<>();

    private static int[][] sprite(String n) {
        return SPRITES.computeIfAbsent(n, k -> {
            try (var in = CosmeticLayer.class.getResourceAsStream("/assets/meowcosmetics/textures/pets/" + k + ".png")) {
                var img = com.mojang.blaze3d.platform.NativeImage.read(in);
                int[][] px = new int[img.getWidth()][img.getHeight()];
                for (int x = 0; x < img.getWidth(); x++) for (int y = 0; y < img.getHeight(); y++) px[x][y] = img.getPixel(x, y);
                img.close();
                return px;
            } catch (Exception e) {
                return new int[1][1];
            }
        });
    }

    /** A slime: its sprite as a front of pixels, thickened into a jelly block that bobs and squishes. */
    private void slime(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, int idx, float t) {
        int[][] px = sprite(SLIMES[idx]);
        int w = px.length, h = px[0].length;
        java.util.Map<Integer, Integer> count = new java.util.HashMap<>();
        for (int[] col : px) for (int c : col) if ((c >>> 24) > 0) count.merge(c, 1, Integer::sum);
        int body = count.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xFF888888);
        float sq = (float) Math.sin(t * 3.2f);
        ps.scale(1 - 0.05f * sq, 1 + 0.08f * sq, 1 - 0.05f * sq);
        float v = 0.62f * PX, depth = Math.max(6, w * 0.6f) * v;
        // round, like a blob: thickest in the middle, thinning to the sides and the top (in whole pixels, so it stays pixel-art)
        int top = h, bottom = 0;
        for (int[] col : px) for (int y = 0; y < h; y++) if ((col[y] >>> 24) > 0) { top = Math.min(top, y); bottom = Math.max(bottom, y); }
        int t0 = top, b0 = bottom;
        out.submitCustomGeometry(ps, rt, (pose, vc) -> {
            Vox b = new Vox(pose, vc, light);
            float max = depth / 2;
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                int c = px[x][y];
                if ((c >>> 24) == 0) continue;
                float nx = (x + 0.5f - w / 2f) / (w / 2f), ny = (b0 - y + 0.5f) / Math.max(1f, b0 - t0 + 1f);   // 0 at the bottom, 1 at the top
                float k = (float) Math.sqrt(Math.max(0.12f, 1 - nx * nx)) * (float) Math.sqrt(Math.max(0.25f, 1 - Math.max(0, ny - 0.35f) * Math.max(0, ny - 0.35f) / 0.42f));
                float half = Math.max(v, Math.round(max * k / v) * v);
                float x0 = (x - w / 2f) * v, y0 = (h - 1 - y) * v;
                b.box(x0, y0, -half, x0 + v, y0 + v, -half + v, c | 0xFF000000);   // the face side, on the curve
                b.box(x0, y0, -half + v, x0 + v, y0 + v, half, (isFace(c) ? body : c) | 0xFF000000);
            }
        });
        if (idx == 5) {   // the cat slime's tail
            ps.translate(0, 3 * v, depth / 2);
            ps.mulPose(Axis.XP.rotationDegrees(25));
            for (int i = 0; i < 6; i++) {
                ps.mulPose(Axis.YP.rotationDegrees((float) Math.sin(t * 2.6f - i * 0.5f) * (4 + i * 2)));
                ps.mulPose(Axis.XP.rotationDegrees(-14));
                out.submitCustomGeometry(ps, rt, (pose, vc) -> new Vox(pose, vc, light).box(-0.7f * v, -0.7f * v, 0, 0.7f * v, 0.7f * v, 1.7f * v, 0xFF1E1D24));
                ps.translate(0, 0, 1.5f * v);
            }
        }
    }

    private static boolean isFace(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, bl = c & 255;
        return (r < 60 && g < 40 && bl < 60) || (r > 230 && g < 170 && bl > 130) || (r > 230 && g > 200 && bl < 100) || (r > 235 && g > 235 && bl > 225);
    }

    private void pet(PoseStack ps, SubmitNodeCollector out, RenderType rt, int light, int coat, float t) {
        if (coat >= COATS.length) {
            var f = Cosmetics.cfg();
            float size = Math.max(30, f.petSize) / 100f;
            ps.pushPose();
            ps.translate((f.petSide == 1 ? 0.82f : -0.82f), 1.72f + 0.07f * (float) Math.sin(t * 2.1f), 0.18f);
            ps.mulPose(Axis.YP.rotationDegrees(14 * (float) Math.sin(t * 0.45f)));
            ps.scale(size, size, size);
            slime(ps, out, rt, light, coat - COATS.length, t);
            ps.popPose();
            return;
        }
        int[] c = COATS[coat];
        ps.pushPose();
        var f = Cosmetics.cfg();
        float size = Math.max(30, f.petSize) / 100f;
        ps.translate((f.petSide == 1 ? 0.62f : -0.62f) - (f.petSide == 1 ? -0.2f : 0.2f) * size, 1.72f + 0.07f * (float) Math.sin(t * 2.1f), 0.18f);
        ps.mulPose(Axis.YP.rotationDegrees(14 * (float) Math.sin(t * 0.45f)));
        ps.mulPose(Axis.XP.rotationDegrees(4 * (float) Math.sin(t * 2.1f + 1)));
        ps.scale(0.82f * size, 0.82f * size, 0.82f * size);
        int paw = coat == 1 ? c[2] : coat == 4 ? c[1] : coat == 0 || coat == 2 ? c[2] : c[0];
        out.submitCustomGeometry(ps, rt, (pose, vc) -> {
            Vox v = new Vox(pose, vc, light);
            for (int i = 0; i < 4; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 7; k++)
                v.box((i - 2) * PX, j * PX, (k - 3) * PX, (i - 1) * PX, (j + 1) * PX, (k - 2) * PX, fur(bodyColor(coat, i, j, k), i, j, k));
            for (int lx = -1; lx <= 1; lx += 2) for (int lz = -1; lz <= 1; lz += 2) {   // paws, dangling
                float sw = 0.4f * (float) Math.sin(t * 2.4f + lx + lz * 2) * PX;
                v.box((lx * 1.1f - 0.6f) * PX, -1.4f * PX, (lz * 2.1f - 0.6f) * PX + sw, (lx * 1.1f + 0.6f) * PX, 0.1f * PX, (lz * 2.1f + 0.6f) * PX + sw, paw);
            }
        });
        ps.pushPose();   // head, in front and a bit up
        ps.translate(0, 2.6f * PX, -2.6f * PX);
        ps.mulPose(Axis.ZP.rotationDegrees(9 * (float) Math.sin(t * 0.8f)));
        head(ps, out, rt, light, coat, t, false);
        ps.popPose();
        ps.translate(0, 2.4f * PX, 3.9f * PX);   // tail, curling up behind
        ps.mulPose(Axis.XP.rotationDegrees(20));
        for (int i = 0; i < 8; i++) {
            ps.mulPose(Axis.YP.rotationDegrees((float) Math.sin(t * 2.6f - i * 0.5f) * (4 + i * 1.6f)));
            ps.mulPose(Axis.XP.rotationDegrees(-12));
            int color = switch (coat) {
                case 0, 2 -> i % 2 == 0 ? c[0] : c[1];
                case 3 -> i < 4 ? c[1] : c[2];
                case 4 -> CapeArt.mix(c[0], c[1], Math.min(1, i / 3f));
                default -> c[0];
            };
            out.submitCustomGeometry(ps, rt, (pose, vc) -> new Vox(pose, vc, light).box(-0.6f * PX, -0.6f * PX, 0, 0.6f * PX, 0.6f * PX, 1.4f * PX, color));
            ps.translate(0, 0, 1.25f * PX);
        }
        ps.popPose();
    }
}
