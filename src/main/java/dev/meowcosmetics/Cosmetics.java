package dev.meowcosmetics;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/** Client-side cosmetics on your own player only, seen only by you. */
public final class Cosmetics {
    private Cosmetics() {}

    /** What one render of you wears (from your settings, or a preview in the menu). Null: nothing. */
    public record Look(boolean cape, int capeStyle, String capeFile, int capeSpeed,
                       boolean ears, int earColor, boolean tail, int tailColor, int catSpeed,
                       boolean aura, int auraStyle, int auraSpeed, int auraAmount,
                       boolean pet, int petStyle, int petSpeed) {

        static Look of(CosConfig c) {
            return new Look(c.capeOn, c.capeStyle, c.capeFile, c.capeSpeed, c.ears, c.earColor, c.tail, c.tailColor, c.catSpeed,
                c.auraOn, c.auraStyle, c.auraSpeed, c.auraAmount, c.petOn, c.petStyle, c.petSpeed);
        }

        /** Where the ears and tail sit (from your settings, shared by the preview). */
        CosConfig fit() { return cfg(); }
    }

    /** Set on the render state while extracting, so the layer knows it's you and what to draw. */
    public interface Me {
        Look meowcosmetics$look();
        void meowcosmetics$setLook(Look look);
    }

    public enum Kind { CAPE, EARS, TAIL, AURA, PET }

    public static final String[] CAPES = {"Starry Forest", "Sakura", "Aurora", "Your image"};
    public static final String[] AURAS = {"Bats", "Hearts", "Sparkles", "Cats"};
    public static final String[] PETS = {"Ginger tabby", "Tuxedo", "Grey tabby", "Calico", "Siamese", "Black cat", "White cat",
        "Green slime", "Berry slime", "Ice slime", "Red slime", "Honey slime", "Cat slime"};
    public static final String[] COLORS = {"Black", "White", "Ginger", "Grey", "Pink", "Lilac"};
    /** Fur, inner ear, darker fur for stripes and tips. */
    static final int[][] FUR = {
        {0xFF1E1D24, 0xFF6E6873, 0xFF121117},
        {0xFFF1EFEA, 0xFFF4B8C6, 0xFFD8D4CD},
        {0xFFE38B3E, 0xFFF9CFA6, 0xFFB8652A},
        {0xFF85868F, 0xFFF0B6C3, 0xFF5E5F67},
        {0xFFF5A8C9, 0xFFFFE2EE, 0xFFE47FAA},
        {0xFFB89AE6, 0xFFF0E2FF, 0xFF9473C9},
    };

    static CosConfig cfg() { return CosConfig.get(); }

    /** While the menu draws a preview: which cosmetic and which option to show (forced on). */
    private static Kind previewKind;
    private static int previewOption;

    public static void preview(Kind kind, int option) { previewKind = kind; previewOption = option; }

    /** Your look for this render: your settings, with the previewed cosmetic swapped in. */
    public static Look look() {
        Look l = Look.of(cfg());
        if (previewKind == null) return l;
        int o = previewOption;
        return switch (previewKind) {
            case CAPE -> new Look(true, o, l.capeFile, l.capeSpeed, l.ears, l.earColor, l.tail, l.tailColor, l.catSpeed, l.aura, l.auraStyle, l.auraSpeed, l.auraAmount, l.pet, l.petStyle, l.petSpeed);
            case EARS -> new Look(l.cape, l.capeStyle, l.capeFile, l.capeSpeed, true, o, l.tail, l.tailColor, l.catSpeed, l.aura, l.auraStyle, l.auraSpeed, l.auraAmount, l.pet, l.petStyle, l.petSpeed);
            case TAIL -> new Look(l.cape, l.capeStyle, l.capeFile, l.capeSpeed, l.ears, l.earColor, true, o, l.catSpeed, l.aura, l.auraStyle, l.auraSpeed, l.auraAmount, l.pet, l.petStyle, l.petSpeed);
            case AURA -> new Look(l.cape, l.capeStyle, l.capeFile, l.capeSpeed, l.ears, l.earColor, l.tail, l.tailColor, l.catSpeed, true, o, l.auraSpeed, l.auraAmount, l.pet, l.petStyle, l.petSpeed);
            case PET -> new Look(l.cape, l.capeStyle, l.capeFile, l.capeSpeed, l.ears, l.earColor, l.tail, l.tailColor, l.catSpeed, l.aura, l.auraStyle, l.auraSpeed, l.auraAmount, true, o, l.petSpeed);
        };
    }

    /** Seconds of animation for this state, at a speed in percent. */
    static float time(AvatarRenderState s, int speed) { return s.ageInTicks / 20f * speed / 100f; }

    private static Identifier white;

    /** A plain white texture, coloured per vertex. Render thread. */
    static Identifier white() {
        if (white == null) {
            white = Identifier.fromNamespaceAndPath("meowcosmetics", "cosmetics/white");
            DynamicTexture t = new DynamicTexture(() -> "meowcosmetics white", 4, 4, false);
            t.getPixels().fillRect(0, 0, 4, 4, 0xFFFFFFFF);
            t.upload();
            Minecraft.getInstance().getTextureManager().register(white, t);
        }
        return white;
    }

    /** Marks your own render state with what you wear, and swaps in the cape. */
    public static void extract(AvatarRenderState state, boolean me) {
        Look l = me ? look() : null;
        ((Me) state).meowcosmetics$setLook(l);
        if (me && cfg().hideArmor) {   // only how you look on your own screen
            state.headEquipment = net.minecraft.world.item.ItemStack.EMPTY;
            state.chestEquipment = net.minecraft.world.item.ItemStack.EMPTY;
            state.legsEquipment = net.minecraft.world.item.ItemStack.EMPTY;
            state.feetEquipment = net.minecraft.world.item.ItemStack.EMPTY;
            state.headItem.clear();
            state.wornHeadType = null;
            state.wornHeadProfile = null;
        }
        if (l == null || !l.cape() || state.isInvisible) return;
        Identifier tex = CapeArt.texture(l.capeStyle(), l.capeFile(), time(state, l.capeSpeed()));
        if (tex == null) return;
        PlayerSkin s = state.skin;
        state.skin = new PlayerSkin(s.body(), new ClientAsset.ResourceTexture(tex, tex), s.elytra(), s.model(), s.secure());
        state.showCape = true;
    }

    public static java.util.List<String> capeFiles() { return CapeArt.files(); }

    public static void openCapes() {
        CapeArt.files();   // makes the folder
        net.minecraft.util.Util.getPlatform().openPath(CapeArt.dir());
    }
}
