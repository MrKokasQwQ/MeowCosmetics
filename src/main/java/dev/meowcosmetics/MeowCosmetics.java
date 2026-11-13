package dev.meowcosmetics;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-side cosmetics: capes, cat ears and tail, auras, pets, hide armor. Only on your own screen. /cosmetics */
public final class MeowCosmetics implements ClientModInitializer {
    private static KeyMapping menu, armor;

    @Override
    public void onInitializeClient() {
        CosConfig.get();
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("meowcosmetics", "main"));
        menu = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.meowcosmetics.menu", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat));
        armor = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.meowcosmetics.armor", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.consumeClick()) if (mc.screen == null) mc.setScreen(new SettingsScreen(null));
            while (armor.consumeClick()) {
                CosConfig c = CosConfig.get();
                c.hideArmor = !c.hideArmor;
                CosConfig.save();
                if (mc.player != null) mc.player.sendSystemMessage(Component.literal("§dCosmetics §7» Armor " + (c.hideArmor ? "§chidden" : "§ashown") + " §7(only on your screen)"));
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((d, reg) -> d.register(literal("cosmetics").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new SettingsScreen(null)));
            return 1;
        })));
    }
}
