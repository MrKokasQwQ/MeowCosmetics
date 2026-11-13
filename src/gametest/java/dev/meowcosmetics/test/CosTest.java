package dev.meowcosmetics.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

@SuppressWarnings("UnstableApiUsage")
public class CosTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            ctx.waitTicks(60);
            ctx.runOnClient(mc -> { var c = dev.meowcosmetics.CosConfig.get(); c.capeOn = true; c.petOn = true; c.petStyle = 12; c.ears = true; c.tail = true; });
            for (int t = 0; t < 6; t += 2) {
                int tab = t;
                ctx.runOnClient(mc -> { try { var f = dev.meowcosmetics.SettingsScreen.class.getDeclaredField("tab"); f.setAccessible(true); f.setInt(null, tab); } catch (Exception e) { throw new RuntimeException(e); } });
                ctx.setScreen(() -> new dev.meowcosmetics.SettingsScreen(null));
                ctx.waitTicks(10);
                ctx.takeScreenshot("cos_tab" + tab);
            }
        }
    }
}
