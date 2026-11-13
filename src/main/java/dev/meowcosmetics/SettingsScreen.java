package dev.meowcosmetics;

import dev.meowcosmetics.mixin.InventoryScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** /cosmetics: a tab per cosmetic, a preview of you wearing it (drag to turn, scroll to zoom), and its settings. */
public class SettingsScreen extends Screen {
    private static final String[] TABS = {"Capes", "Cat Ears", "Cat Tail", "Auras", "Pets", "Hide Armor"};
    private static int tab;
    private static float yaw = 180, pitch = 8, zoom = 1;
    private static long touched;
    private final Screen parent;
    private int px, py, pw, ph;
    private boolean dragging;

    public SettingsScreen(Screen parent) { super(Component.literal("Meow Cosmetics")); this.parent = parent; }

    private static CosConfig c() { return CosConfig.get(); }

    private Cosmetics.Kind kind() { return tab < 5 ? Cosmetics.Kind.values()[tab] : null; }
    private String[] names() { return switch (tab) { case 0 -> Cosmetics.CAPES; case 1, 2 -> Cosmetics.COLORS; case 3 -> Cosmetics.AURAS; case 4 -> java.util.Arrays.copyOfRange(Cosmetics.PETS, petFrom(), petTo()); default -> new String[0]; }; }
    private int petFrom() { return c().petGroup == 1 ? 7 : 0; }
    private int petTo() { return c().petGroup == 1 ? Cosmetics.PETS.length : 7; }
    private int option() { return switch (tab) { case 0 -> c().capeStyle; case 1 -> c().earColor; case 2 -> c().tailColor; case 3 -> c().auraStyle; case 4 -> c().petStyle >= petFrom() && c().petStyle < petTo() ? c().petStyle - petFrom() : 0; default -> 0; }; }
    private void setOption(int v) { switch (tab) { case 0 -> c().capeStyle = v; case 1 -> c().earColor = v; case 2 -> c().tailColor = v; case 3 -> c().auraStyle = v; case 4 -> c().petStyle = petFrom() + v; default -> {} } }

    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "§aOn" : "§cOff")), b -> {
            set.accept(!get.getAsBoolean());
            CosConfig.save();
            b.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "§aOn" : "§cOff")));
        }).bounds(x, y, w, 20).build());
    }

    private void slider(int x, int y, int w, String label, int min, int max, IntSupplier get, IntConsumer set, IntFunction<String> fmt) {
        addRenderableWidget(new AbstractSliderButton(x, y, w, 20, Component.empty(), (get.getAsInt() - min) / (double) (max - min)) {
            { updateMessage(); }
            private int val() { return min + (int) Math.round(value * (max - min)); }
            @Override protected void updateMessage() { setMessage(Component.literal(label + ": " + fmt.apply(val()))); }
            @Override protected void applyValue() { set.accept(val()); CosConfig.save(); }
        });
    }

    @Override
    protected void init() {
        int tw = 84, x0 = Math.max(6, width / 2 - 230);
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            addRenderableWidget(Button.builder(Component.literal((i == tab ? "§d» " : "") + TABS[i]), b -> { tab = t; rebuildWidgets(); }).bounds(x0, 30 + i * 22, tw, 20).build());
        }
        px = x0 + tw + 8; py = 30; pw = 170; ph = Math.min(height - 84, 200);
        int cx = px + pw + 8, cw = Math.min(170, width - cx - 6), y = 30;
        IntFunction<String> pct = v -> v + "%", tenth = v -> v == 0 ? "0" : String.format("%+.1fpx", v / 10f), deg = v -> v + "°";
        CosConfig c = c();
        switch (tab) {
            case 0 -> {
                toggle(cx, y, cw, "Cape", () -> c.capeOn, v -> c.capeOn = v);
                slider(cx, y += 22, cw, "Speed", 10, 300, () -> c.capeSpeed, v -> c.capeSpeed = v, pct);
                addRenderableWidget(Button.builder(Component.literal("Image: " + (c.capeFile.isEmpty() ? "none" : c.capeFile)), b -> {
                    List<String> fs = CapeArt.files();
                    c.capeFile = fs.isEmpty() ? "" : fs.get((fs.indexOf(c.capeFile) + 1) % fs.size());
                    if (!fs.isEmpty()) c.capeStyle = 3;
                    CosConfig.save();
                    rebuildWidgets();
                }).bounds(cx, y += 22, cw, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Open capes folder"), b -> Cosmetics.openCapes()).bounds(cx, y + 22, cw, 20).build());
            }
            case 1 -> {
                toggle(cx, y, cw, "Cat ears", () -> c.ears, v -> c.ears = v);
                slider(cx, y += 22, cw, "Speed", 10, 300, () -> c.catSpeed, v -> c.catSpeed = v, pct);
                slider(cx, y += 22, cw, "Size", 40, 250, () -> c.earSize, v -> c.earSize = v, pct);
                slider(cx, y += 22, cw, "Spacing", -25, 25, () -> c.earSpread, v -> c.earSpread = v, tenth);
                slider(cx, y += 22, cw, "Forward/back", -35, 35, () -> c.earForward, v -> c.earForward = v, tenth);
                slider(cx, y += 22, cw, "Height", -25, 25, () -> c.earHeight, v -> c.earHeight = v, tenth);
                slider(cx, y + 22, cw, "Tilt", -30, 45, () -> c.earTilt, v -> c.earTilt = v, deg);
            }
            case 2 -> {
                toggle(cx, y, cw, "Cat tail", () -> c.tail, v -> c.tail = v);
                slider(cx, y += 22, cw, "Speed", 10, 300, () -> c.catSpeed, v -> c.catSpeed = v, pct);
                slider(cx, y += 22, cw, "Length", 3, 20, () -> c.tailLength, v -> c.tailLength = v, String::valueOf);
                slider(cx, y += 22, cw, "Curl", -100, 100, () -> c.tailCurl, v -> c.tailCurl = v, pct);
                slider(cx, y += 22, cw, "Side", -90, 90, () -> c.tailSide, v -> c.tailSide = v, deg);
                slider(cx, y + 22, cw, "Height", -40, 30, () -> c.tailHeight, v -> c.tailHeight = v, tenth);
            }
            case 3 -> {
                toggle(cx, y, cw, "Aura", () -> c.auraOn, v -> c.auraOn = v);
                slider(cx, y += 22, cw, "Speed", 10, 300, () -> c.auraSpeed, v -> c.auraSpeed = v, pct);
                slider(cx, y + 22, cw, "Amount", 5, 100, () -> c.auraAmount, v -> c.auraAmount = v, pct);
            }
            case 4 -> {
                toggle(cx, y, cw, "Pet", () -> c.petOn, v -> c.petOn = v);
                addRenderableWidget(Button.builder(Component.literal("Type: " + (c.petGroup == 1 ? "Slimes" : "Cats")), b -> {
                    c.petGroup = 1 - c.petGroup; c.petStyle = c.petGroup == 1 ? 7 : 0; CosConfig.save(); rebuildWidgets();
                }).bounds(cx, y += 22, cw, 20).build());
                slider(cx, y += 22, cw, "Speed", 10, 300, () -> c.petSpeed, v -> c.petSpeed = v, pct);
                slider(cx, y += 22, cw, "Size", 50, 250, () -> c.petSize, v -> c.petSize = v, pct);
                addRenderableWidget(Button.builder(Component.literal("Shoulder: " + (c.petSide == 1 ? "Right" : "Left")), b -> {
                    c.petSide = 1 - c.petSide; CosConfig.save(); rebuildWidgets();
                }).bounds(cx, y + 22, cw, 20).build());
            }
            default -> toggle(cx, y, cw, "Hide armor", () -> c.hideArmor, v -> c.hideArmor = v);
        }
        if (kind() != null) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { setOption(Math.floorMod(option() - 1, names().length)); CosConfig.save(); }).bounds(px + 4, py + ph / 2 - 10, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { setOption(Math.floorMod(option() + 1, names().length)); CosConfig.save(); }).bounds(px + pw - 24, py + ph / 2 - 10, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width - 106, height - 26, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        g.fill(px, py, px + pw, py + ph, 0x60000000);
        Minecraft mc = Minecraft.getInstance();
        if (tab == 5) g.centeredText(font, Component.literal("§7Set a key in Controls"), px + pw / 2, py + ph / 2, 0xFFFFFFFF);
        if (mc.player != null) {
            long now = System.currentTimeMillis();
            if (now - touched > 3000) yaw += 0.4f;
            Cosmetics.Kind k = kind();
            if (k != null) Cosmetics.preview(k, Math.floorMod(option(), names().length) + (tab == 4 ? petFrom() : 0));
            EntityRenderState st;
            try { st = InventoryScreenAccess.meowcosmetics$extract(mc.player); } finally { Cosmetics.preview(null, 0); }
            if (st instanceof LivingEntityRenderState ls) {
                ls.bodyRot = 180 + yaw; ls.yRot = 0; ls.xRot = 0;
                ls.boundingBoxWidth /= ls.scale; ls.boundingBoxHeight /= ls.scale; ls.scale = 1;
            }
            int area = ph - 24;
            float focus = switch (tab) { case 1 -> 1.8f; case 2 -> 0.85f; case 4 -> 1.45f; default -> 1.0f; };
            float size = (tab == 1 ? area / 1.15f : tab == 3 ? area / 2.6f : area / 2.1f) * zoom;
            Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(pitch));
            g.entity(st, size, new Vector3f(0, focus, 0), new Quaternionf().rotateZ((float) Math.PI).mul(cam), cam, px + 2, py + 16, px + pw - 2, py + ph - 2);
        } else g.centeredText(font, Component.literal("§7Join a world to see the preview"), px + pw / 2, py + ph / 2, 0xFFFFFFFF);
        if (kind() != null) g.centeredText(font, Component.literal("§l" + names()[Math.floorMod(option(), names().length)] + " §7(" + (Math.floorMod(option(), names().length) + 1) + "/" + names().length + ")"), px + pw / 2, py + 4, 0xFFFFFFFF);
        g.centeredText(font, Component.literal("§8Drag: turn · Scroll: zoom"), px + pw / 2, py + ph + 4, 0xFFFFFFFF);
        if (tab == 4 && c().petGroup == 1) g.centeredText(font, Component.literal("§8Slime sprites: Lin-ain (itch.io)"), px + pw / 2, py + ph + 14, 0xFFFFFFFF);
    }

    private boolean inPreview(double x, double y) { return x >= px && x < px + pw && y >= py && y < py + ph; }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (super.mouseClicked(e, doubled)) return true;
        if (inPreview(e.x(), e.y())) { dragging = true; touched = System.currentTimeMillis(); return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging) { touched = System.currentTimeMillis(); yaw += (float) dx * 1.5f; pitch = Math.max(-40, Math.min(60, pitch + (float) dy * 0.8f)); return true; }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) { dragging = false; return super.mouseReleased(e); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (inPreview(mx, my)) { touched = System.currentTimeMillis(); zoom = Math.max(0.5f, Math.min(4f, zoom * (sy > 0 ? 1.12f : 1 / 1.12f))); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void onClose() { CosConfig.save(); Minecraft.getInstance().setScreen(parent); }
}
