package dev.meowcosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Animated capes, painted in code (original art) at 8x the vanilla cape resolution: the 10x16 outside of
 * the cape becomes 80x128 pixels. Only the frame on screen is painted, ~20 times a second.
 * Also plays your own PNGs from config/meowcosmetics/capes (a normal 64x32-layout cape, any size, or
 * an animated strip of such frames stacked top to bottom).
 */
final class CapeArt {
    private static final int S = 8, TW = 64 * S, TH = 32 * S, W = 10 * S, H = 16 * S;
    private static final int[] buf = new int[W * H];

    /** One texture per cape, so the menu preview and the cape you wear can differ. */
    private static final class Slot {
        DynamicTexture tex;
        Identifier id;
        int w, h, lastFrame = Integer.MIN_VALUE;
        int[] base;
    }

    private static final Slot[] SLOTS = new Slot[4];

    private CapeArt() {}

    /** [style]: 0 Starry Forest, 1 Sakura, 2 Aurora, 3 your image. */
    static Identifier texture(int style, String file, float t) {
        if (style == 3) return custom(file, t);
        if (style < 0 || style > 2) return null;
        Slot sl = slot(style, TW, TH);
        if (sl.base == null) {
            sl.base = switch (style) { case 0 -> forestBase(); case 1 -> sakuraBase(); default -> auroraBase(); };
            sl.tex.getPixels().fillRect(0, 0, TW, TH, switch (style) { case 0 -> 0xFF060B1E; case 1 -> 0xFF6E2A48; default -> 0xFF03060F; });
        }
        int frame = (int) Math.floor(t * 20);
        if (frame != sl.lastFrame) {
            sl.lastFrame = frame;
            System.arraycopy(sl.base, 0, buf, 0, buf.length);
            switch (style) { case 0 -> forest(t); case 1 -> sakura(t); default -> aurora(t); }
            frame();
            NativeImage img = sl.tex.getPixels();
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) img.setPixel(S + x, S + y, buf[y * W + x]);
            sl.tex.upload();
        }
        return sl.id;
    }

    private static Slot slot(int i, int w, int h) {
        Slot sl = SLOTS[i];
        if (sl != null && sl.w == w && sl.h == h) return sl;
        Minecraft mc = Minecraft.getInstance();
        if (sl == null) {
            sl = SLOTS[i] = new Slot();
            sl.id = Identifier.fromNamespaceAndPath("meowcosmetics", "cosmetics/cape_" + i);
        } else mc.getTextureManager().release(sl.id);
        sl.tex = new DynamicTexture(() -> "meowcosmetics cape", w, h, false);
        sl.w = w;
        sl.h = h;
        sl.lastFrame = Integer.MIN_VALUE;
        sl.base = null;
        mc.getTextureManager().register(sl.id, sl.tex);
        return sl;
    }

    // ------------------------------------------------------------------ your own images

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("meowcosmetics").resolve("capes"); }

    /** PNG names in the capes folder. */
    static List<String> files() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.map(p -> p.getFileName().toString()).filter(n -> n.toLowerCase().endsWith(".png")).sorted().forEach(out::add);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String loaded;
    private static NativeImage src;
    private static int fw, fh, frames;

    private static Identifier custom(String file, float t) {
        if (file == null || file.isEmpty()) return null;
        if (!file.equals(loaded)) {
            loaded = file;
            if (src != null) src.close();
            src = null;
            try (InputStream in = Files.newInputStream(dir().resolve(file))) {
                src = NativeImage.read(in);
                fw = src.getWidth();
                fh = Math.max(1, fw / 2);
                frames = src.getHeight() >= fh * 2 ? src.getHeight() / fh : 1;
                if (frames == 1) fh = src.getHeight();
            } catch (Exception e) {
                src = null;
            }
            if (SLOTS[3] != null) SLOTS[3].lastFrame = Integer.MIN_VALUE;
        }
        if (src == null) return null;
        Slot sl = slot(3, fw, fh);
        int f = frames <= 1 ? 0 : Math.floorMod((int) Math.floor(t * 12), frames);
        if (f != sl.lastFrame) {
            sl.lastFrame = f;
            NativeImage img = sl.tex.getPixels();
            for (int y = 0; y < fh; y++) for (int x = 0; x < fw; x++) img.setPixel(x, y, src.getPixel(x, f * fh + y));
            sl.tex.upload();
        }
        return sl.id;
    }

    // ------------------------------------------------------------------ painting helpers

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private static float bayer(int x, int y) { return (BAYER[(y & 3) * 4 + (x & 3)] + 0.5f) / 16f; }

    /** Dithered pick along a palette, like hand-made pixel art gradients. */
    private static int ramp(int[] pal, float t, int x, int y) {
        t = Math.max(0, Math.min(0.9999f, t)) * (pal.length - 1);
        int i = (int) t;
        return t - i > bayer(x, y) ? pal[Math.min(i + 1, pal.length - 1)] : pal[i];
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int r = (int) (ar + (((b >> 16) & 255) - ar) * t), g = (int) (ag + (((b >> 8) & 255) - ag) * t), bl = (int) (ab + ((b & 255) - ab) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static void blend(int[] p, int x, int y, int c, float a) {
        if (x < 0 || y < 0 || x >= W || y >= H || a <= 0) return;
        p[y * W + x] = mix(p[y * W + x], c, a);
    }

    private static void set(int[] p, int x, int y, int c) { if (x >= 0 && y >= 0 && x < W && y < H) p[y * W + x] = c; }

    private static float hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** Soft dark frame around the edge, like a woven border. */
    private static void frame() {
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int d = Math.min(Math.min(x, W - 1 - x), Math.min(y, H - 1 - y));
            if (d < 3) buf[y * W + x] = mix(buf[y * W + x], 0xFF000000, d == 0 ? 0.55f : d == 1 ? 0.3f : 0.12f);
        }
    }

    private static void disc(int[] p, float cx, float cy, float r, int c) {
        for (int y = (int) (cy - r - 1); y <= cy + r + 1; y++) for (int x = (int) (cx - r - 1); x <= cx + r + 1; x++) {
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
            if (d <= r) set(p, x, y, c);
        }
    }

    private static void halo(int[] p, float cx, float cy, float r0, float r1, int c, float a) {
        for (int y = (int) (cy - r1); y <= cy + r1; y++) for (int x = (int) (cx - r1); x <= cx + r1; x++) {
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
            if (d > r0 && d < r1) {
                float k = 1 - (d - r0) / (r1 - r0);
                if (k * k * a > bayer(x, y) * 0.5f) blend(p, x, y, c, k * k * a);
            }
        }
    }

    /** A stepped pixel pine, with a rim of light on one side. */
    private static void pine(int[] p, int cx, int baseY, int h, int w, int body, int rim, boolean rimRight) {
        for (int yy = 0; yy < h; yy++) {
            float rel = (yy + 1) / (float) h;
            float half = rel * w / 2f * (0.72f + 0.28f * ((yy % 6) / 5f));
            int y = baseY - h + yy, x0 = Math.round(cx - half), x1 = Math.round(cx + half);
            for (int x = x0; x <= x1; x++) set(p, x, y, body);
            set(p, rimRight ? x1 : x0, y, rim);
        }
        for (int y = baseY; y < baseY + 3; y++) set(p, cx, y, body);
    }

    private static List<float[]> stars(int n, int seed, int maxY) {
        Random r = new Random(seed);
        List<float[]> out = new ArrayList<>();
        for (int i = 0; i < n; i++)
            out.add(new float[]{r.nextInt(W), r.nextInt(maxY), r.nextFloat() * 6.28f, 0.6f + r.nextFloat() * 1.8f, r.nextFloat() < 0.12f ? 1 : 0, r.nextFloat()});
        return out;
    }

    private static void twinkle(List<float[]> stars, float t, int[] ok) {
        for (float[] s : stars) {
            int x = (int) s[0], y = (int) s[1];
            if (ok != null && ok[y * W + x] == 0) continue;
            float b = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(t * s[3] + s[2]));
            int c = s[5] < 0.2f ? 0xFFFFF1C9 : s[5] < 0.45f ? 0xFFBFDCFF : 0xFFFFFFFF;
            blend(buf, x, y, c, b);
            if (s[4] > 0 && b > 0.6f) {
                float a = (b - 0.6f) * 1.6f;
                blend(buf, x - 1, y, c, a * 0.55f); blend(buf, x + 1, y, c, a * 0.55f);
                blend(buf, x, y - 1, c, a * 0.55f); blend(buf, x, y + 1, c, a * 0.55f);
                if (b > 0.9f) { blend(buf, x - 2, y, c, a * 0.2f); blend(buf, x + 2, y, c, a * 0.2f); blend(buf, x, y - 2, c, a * 0.2f); blend(buf, x, y + 2, c, a * 0.2f); }
            }
        }
    }

    // ------------------------------------------------------------------ Starry Forest

    private static final int[] FOREST_SKY = {0xFF050918, 0xFF08112B, 0xFF0C1A40, 0xFF112656, 0xFF18356F, 0xFF214689, 0xFF2B57A0};
    private static int[] skyMask;
    private static final List<float[]> FOREST_STARS = stars(110, 7, 78);
    private static final List<float[]> FLIES = stars(18, 11, 30);

    private static float ridge1(int x) { return 66 - (9 * Math.abs((float) Math.sin(x * 0.055f + 0.4f)) + 4 * (float) Math.sin(x * 0.21f + 1.3f) + 1.5f * (float) Math.sin(x * 0.53f)); }
    private static float ridge2(int x) { return 82 - (4 * (float) Math.sin(x * 0.11f + 2) + 2 * (float) Math.sin(x * 0.31f)); }

    private static int[] forestBase() {
        int[] p = new int[W * H];
        skyMask = new int[W * H];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) p[y * W + x] = ramp(FOREST_SKY, y / 96f, x, y);
        // a faint band of the galaxy, top left to right
        for (int y = 0; y < 70; y++) for (int x = 0; x < W; x++) {
            float d = Math.abs(y - (6 + x * 0.42f)) / 9f;
            float a = (1 - d) * (0.18f + 0.22f * hash(x / 2, y / 2, 3));
            if (a > 0) blend(p, x, y, 0xFF4A62B8, a);
        }
        // moon and its glow
        halo(p, 60, 19, 7, 19, 0xFF7C9BE6, 0.55f);
        disc(p, 60, 19, 7, 0xFFE9F1FF);
        disc(p, 62, 17, 1.4f, 0xFFC6D5F3); disc(p, 57, 21, 1.8f, 0xFFCFDCF6); disc(p, 61, 23, 1, 0xFFC6D5F3);
        for (int y = 12; y < 27; y++) for (int x = 53; x < 68; x++) if (Math.hypot(x + 0.5 - 60, y + 0.5 - 19) <= 7 && Math.hypot(x + 0.5 - 62.5, y + 0.5 - 17) > 7.6) blend(p, x, y, 0xFFB9C9EE, 0.35f);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) skyMask[y * W + x] = y < ridge1(x) - 1 && Math.hypot(x - 60, y - 19) > 9 ? 1 : 0;
        // far mountains with moonlit edges, then hills
        for (int x = 0; x < W; x++) {
            int r1 = Math.round(ridge1(x));
            for (int y = r1; y < 100; y++) set(p, x, y, ramp(new int[]{0xFF223B7A, 0xFF1A2F66, 0xFF15285A}, (y - r1) / 30f, x, y));
            boolean lit = ridge1(x + 1) < ridge1(x);
            set(p, x, r1, lit ? 0xFF6F8CD6 : 0xFF3A5596);
            if (lit) set(p, x, r1 + 1, 0xFF3F5BA0);
            int r2 = Math.round(ridge2(x));
            for (int y = r2; y < 100; y++) set(p, x, y, ramp(new int[]{0xFF15295A, 0xFF10214B}, (y - r2) / 14f, x, y));
            set(p, x, r2, 0xFF22407F);
        }
        // mist over the valley
        for (int y = 84; y < 100; y++) for (int x = 0; x < W; x++) blend(p, x, y, 0xFF2D4F99, 0.18f * (1 - Math.abs(y - 92) / 8f));
        // back pines, then the bank, then front pines
        int[][] back = {{5, 95, 15, 7}, {13, 95, 21, 8}, {24, 96, 14, 6}, {36, 96, 17, 7}, {48, 95, 13, 6}, {66, 95, 19, 8}, {75, 96, 14, 6}};
        for (int[] tr : back) pine(p, tr[0], tr[1], tr[2], tr[3], 0xFF0D1C44, 0xFF2A4A92, true);
        for (int y = 98; y < 103; y++) for (int x = 0; x < W; x++) set(p, x, y, y == 98 ? 0xFF14285A : 0xFF0A1633);
        int[][] front = {{2, 103, 40, 14}, {12, 103, 28, 10}, {21, 104, 19, 8}, {58, 104, 17, 7}, {68, 103, 31, 12}, {78, 103, 44, 14}};
        for (int[] tr : front) pine(p, tr[0], tr[1], tr[2], tr[3], 0xFF060E24, 0xFF1C3772, true);
        // river
        for (int y = 103; y < H; y++) for (int x = 0; x < W; x++)
            set(p, x, y, ramp(new int[]{0xFF0B2257, 0xFF0F2D6E, 0xFF143A87, 0xFF0F2D6E, 0xFF0A1F50}, (y - 103) / 25f, x, y));
        // reeds on the bank
        for (int x = 26; x < 56; x += 3) { int hh = 2 + (int) (hash(x, 1, 5) * 4); for (int y = 103 - hh; y < 104; y++) set(p, x, y, 0xFF07112A); }
        return p;
    }

    private static void forest(float t) {
        twinkle(FOREST_STARS, t, skyMask);
        // a shooting star now and then
        float ph = (t % 9f) / 0.9f;
        if (ph < 1) {
            float hx = 6 + ph * 40, hy = 6 + ph * 18;
            for (int i = 0; i < 12; i++) blend(buf, (int) (hx - i * 1.6f), (int) (hy - i * 0.72f), 0xFFFFFFFF, (1 - i / 12f) * (1 - ph * 0.6f));
        }
        // river: flowing light, the moon's reflection, glints
        for (int y = 104; y < H; y++) for (int x = 0; x < W; x++) {
            float v = 0.6f * (float) Math.sin(x * 0.32f - t * 2.6f + y * 0.8f) + 0.5f * (float) Math.sin(x * 0.11f + t * 1.4f - y * 0.45f + 1.7f);
            if (v > 0.72f) blend(buf, x, y, 0xFF2E69CE, 0.7f);
            if (v > 0.98f) blend(buf, x, y, 0xFF66B0F5, 0.85f);
            if (v > 1.07f) set(buf, x, y, 0xFFA8E2FF);
            float wob = 3 + 2 * (float) Math.sin(y * 0.7f + t * 3);
            if (Math.abs(x - 60) < wob && ((y + (int) (t * 6)) % 3 != 0)) blend(buf, x, y, 0xFFDDEBFF, 0.55f * (1 - (y - 104) / 26f));
            if (hash(x, y, (int) (t * 4)) > 0.996f) set(buf, x, y, 0xFFFFFFFF);
        }
        // fireflies drifting between the trees
        for (int i = 0; i < FLIES.size(); i++) {
            float[] f = FLIES.get(i);
            float x = f[0] + 3.5f * (float) Math.sin(t * 0.45f + i * 1.7f), y = 74 + f[1] + 2.5f * (float) Math.sin(t * 0.33f + i * 2.3f);
            float b = (float) Math.pow(Math.max(0, Math.sin(t * 1.6f + i * 1.1f)), 2);
            int xi = Math.round(x), yi = Math.round(y);
            blend(buf, xi, yi, 0xFFC8FFF9, b);
            blend(buf, xi - 1, yi, 0xFF4FE3FF, b * 0.5f); blend(buf, xi + 1, yi, 0xFF4FE3FF, b * 0.5f);
            blend(buf, xi, yi - 1, 0xFF4FE3FF, b * 0.5f); blend(buf, xi, yi + 1, 0xFF4FE3FF, b * 0.5f);
            blend(buf, xi - 1, yi - 1, 0xFF4FE3FF, b * 0.18f); blend(buf, xi + 1, yi + 1, 0xFF4FE3FF, b * 0.18f);
            blend(buf, xi + 1, yi - 1, 0xFF4FE3FF, b * 0.18f); blend(buf, xi - 1, yi + 1, 0xFF4FE3FF, b * 0.18f);
        }
    }

    // ------------------------------------------------------------------ Sakura

    private static final List<float[]> PETALS = stars(40, 21, H);

    private static int[] sakuraBase() {
        int[] p = new int[W * H];
        int[] sky = {0xFFFFE6F0, 0xFFFFD3E3, 0xFFFDBCD3, 0xFFF7A3C3, 0xFFEC8CB3};
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) p[y * W + x] = ramp(sky, y / 104f, x, y);
        halo(p, 42, 56, 15, 26, 0xFFFFF3F8, 0.6f);
        disc(p, 42, 56, 15, 0xFFFFF6FA);
        for (int y = 41; y < 72; y++) for (int x = 27; x < 58; x++) if (Math.hypot(x + 0.5 - 42, y + 0.5 - 56) <= 15 && y > 56 && bayer(x, y) < (y - 56) / 30f) set(p, x, y, 0xFFFFEAF2);
        // a snowy peak far away
        for (int x = 0; x < W; x++) {
            float r = 92 - Math.max(0, 27 - Math.abs(x - 44) * 0.85f) - 1.5f * (float) Math.sin(x * 0.4f);
            for (int y = Math.round(r); y < 110; y++) {
                boolean snow = y < r + 5 + 2 * (float) Math.sin(x * 0.9f) && r < 80;
                set(p, x, y, snow ? (x < 44 ? 0xFFFFF4F8 : 0xFFF5DCE7) : ramp(new int[]{0xFFD98BB0, 0xFFCF7AA3}, (y - r) / 20f, x, y));
            }
        }
        for (int y = 100; y < H; y++) for (int x = 0; x < W; x++) {
            float hill = 104 + 3 * (float) Math.sin(x * 0.12f + 1);
            if (y >= hill) set(p, x, y, ramp(new int[]{0xFFC0628F, 0xFFA74F7B, 0xFF8E406A}, (y - hill) / 24f, x, y));
        }
        // a torii gate on the hill
        int g = 0xFF9E2A44, gd = 0xFF7A1D33;
        for (int y = 93; y < 112; y++) { set(p, 57, y, g); set(p, 58, y, gd); set(p, 69, y, g); set(p, 70, y, gd); }
        for (int x = 53; x <= 74; x++) { set(p, x, 91, 0xFF3A1420); set(p, x, 92, g); }
        set(p, 52, 90, 0xFF3A1420); set(p, 75, 90, 0xFF3A1420);
        for (int x = 55; x <= 72; x++) set(p, x, 96, g);
        set(p, 63, 93, gd); set(p, 64, 93, gd); set(p, 63, 94, gd); set(p, 64, 94, gd); set(p, 63, 95, gd); set(p, 64, 95, gd);
        // the branch, top left, with blossoms
        float[][] br = {{-2, 8}, {10, 11}, {22, 16}, {34, 19}, {46, 21}, {58, 26}, {66, 32}};
        for (int i = 0; i < br.length - 1; i++) {
            float w = 4.6f - i * 0.55f;
            for (int k = 0; k <= 20; k++) {
                float x = br[i][0] + (br[i + 1][0] - br[i][0]) * k / 20f, y = br[i][1] + (br[i + 1][1] - br[i][1]) * k / 20f;
                disc(p, x, y, w / 2, 0xFF4B2433);
                set(p, Math.round(x), Math.round(y - w / 2), 0xFF6E3449);
            }
        }
        for (int k = 0; k <= 14; k++) { disc(p, 24 + k * 0.6f, 16 - k * 0.7f, 0.8f, 0xFF4B2433); disc(p, 40 + k * 0.4f, 20 + k * 0.6f, 0.7f, 0xFF4B2433); }
        Random r = new Random(4);
        int[] pet = {0xFFFF8FBF, 0xFFFFA9CE, 0xFFFFC7DE, 0xFFFFE0EC};
        for (int i = 0; i < 80; i++) {
            int seg = r.nextInt(br.length - 1);
            float k = r.nextFloat();
            float x = br[seg][0] + (br[seg + 1][0] - br[seg][0]) * k + r.nextFloat() * 9 - 4.5f, y = br[seg][1] + (br[seg + 1][1] - br[seg][1]) * k + r.nextFloat() * 8 - 5;
            float rad = 1.6f + r.nextFloat() * 2.0f;
            disc(p, x, y, rad, pet[r.nextInt(pet.length)]);
            disc(p, x - 0.6f, y - 0.6f, rad * 0.45f, 0xFFFFE9F2);
            set(p, Math.round(x), Math.round(y), 0xFFE0577F);
        }
        return p;
    }

    private static void sakura(float t) {
        for (int i = 0; i < PETALS.size(); i++) {
            float[] f = PETALS.get(i);
            float x = f[0] + t * (4 + f[3] * 3) + 3 * (float) Math.sin(t * 1.5f + f[2]);
            float y = f[1] + t * (9 + f[3] * 5);
            int xi = Math.floorMod(Math.round(x), W), yi = Math.floorMod(Math.round(y), H + 8) - 4;
            int c = f[5] < 0.5f ? 0xFFFF94C2 : f[5] < 0.8f ? 0xFFFFC2DA : 0xFFFFE4EE;
            boolean flip = Math.sin(t * 3 + f[2]) > 0;
            set(buf, xi, yi, c);
            set(buf, xi + 1, flip ? yi : yi + 1, c);
            if (f[4] > 0) { set(buf, xi + (flip ? 0 : 1), yi + 1, Vox.shade(c, 0.9f)); set(buf, xi - 1, yi, Vox.shade(c, 0.95f)); }
        }
        // the sun breathing a little
        float a = 0.12f + 0.08f * (float) Math.sin(t * 0.8f);
        for (int y = 30; y < 82; y++) for (int x = 16; x < 68; x++) {
            float d = (float) Math.hypot(x + 0.5f - 42, y + 0.5f - 56);
            if (d > 15 && d < 26 && hash(x, y, 9) < a) blend(buf, x, y, 0xFFFFF8FB, 0.35f);
        }
    }

    // ------------------------------------------------------------------ Aurora

    private static final List<float[]> AURORA_STARS = stars(70, 31, 92);
    private static int[] auroraSkyMask;

    private static final float[][] PEAKS = {{6, 70}, {22, 79}, {38, 63}, {57, 75}, {73, 67}};

    private static float peak(int x) {
        float r = 104;
        for (float[] p : PEAKS) r = Math.min(r, p[1] + Math.abs(x - p[0]) * 1.2f + 1.2f * (float) Math.sin(x * 0.9f + p[0]));
        return r;
    }

    /** True on the side of a peak facing the light (left). */
    private static boolean litSide(int x) {
        float best = 999, px = 0;
        for (float[] p : PEAKS) { float v = p[1] + Math.abs(x - p[0]) * 1.2f; if (v < best) { best = v; px = p[0]; } }
        return x <= px;
    }

    private static int[] auroraBase() {
        int[] p = new int[W * H];
        auroraSkyMask = new int[W * H];
        int[] sky = {0xFF02030B, 0xFF040919, 0xFF07112A, 0xFF0B1B3E, 0xFF122B55};
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) p[y * W + x] = ramp(sky, y / 100f, x, y);
        for (int x = 0; x < W; x++) {
            int r = Math.round(peak(x));
            for (int y = 0; y < r - 1; y++) auroraSkyMask[y * W + x] = 1;
            boolean lit = litSide(x);
            for (int y = r; y < 112; y++) {
                float k = (y - r) / 22f;
                set(p, x, y, lit ? ramp(new int[]{0xFFEAF1FF, 0xFFC4D3F0, 0xFF93A8D4}, k, x, y) : ramp(new int[]{0xFF8EA2CF, 0xFF6C80B0, 0xFF52668F}, k, x, y));
            }
        }
        for (int y = 108; y < H; y++) for (int x = 0; x < W; x++) set(p, x, y, ramp(new int[]{0xFFDCE6FA, 0xFFB7C7E8, 0xFF94A8D2}, (y - 108) / 20f, x, y));
        int[][] trees = {{3, 122, 22, 9}, {11, 124, 16, 7}, {19, 123, 12, 6}, {62, 124, 14, 6}, {70, 122, 24, 9}, {78, 123, 18, 8}};
        for (int[] tr : trees) {
            pine(p, tr[0], tr[1], tr[2], tr[3], 0xFF06101F, 0xFF06101F, true);
            for (int yy = 0; yy < tr[2]; yy += 6) { int y = tr[1] - tr[2] + yy + 2; set(p, tr[0] - 1, y, 0xFFE8F0FF); set(p, tr[0], y, 0xFFE8F0FF); }
        }
        return p;
    }

    private static void aurora(float t) {
        twinkle(AURORA_STARS, t, auroraSkyMask);
        for (int x = 0; x < W; x++) {
            float c = 34 + 9 * (float) Math.sin(x * 0.065f + t * 0.5f) + 4 * (float) Math.sin(x * 0.19f - t * 0.8f);
            float th = 8 + 3.5f * (float) Math.sin(x * 0.09f + t * 0.7f);
            float rays = 0.6f + 0.4f * (float) Math.sin(x * 0.8f + t * 2.1f + 2 * (float) Math.sin(x * 0.13f));
            float c2 = 58 + 6 * (float) Math.sin(x * 0.05f - t * 0.4f + 2);
            for (int y = 0; y < 96; y++) {
                if (auroraSkyMask[y * W + x] == 0) continue;
                float d = (y - c) / th, i = (float) Math.exp(-d * d * 1.4f) * rays;
                if (y < c) i = Math.max(i, (float) Math.exp(-(c - y) / 20f) * 0.45f * rays);
                float d2 = (y - c2) / 5f, i2 = (float) Math.exp(-d2 * d2) * 0.35f * (0.6f + 0.4f * (float) Math.sin(x * 0.5f - t * 1.3f));
                int col = y > c ? 0xFF3CFFA0 : mix(0xFF31E8D0, 0xFFA06BFF, Math.min(1, (c - y) / 22f));
                if (i > 0.02f) blend(buf, x, y, col, Math.min(0.9f, i * 0.85f + (bayer(x, y) - 0.5f) * 0.08f));
                if (i2 > 0.02f) blend(buf, x, y, 0xFF5CF2FF, i2);
            }
        }
    }
}
