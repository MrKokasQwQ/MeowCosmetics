package dev.meowcosmetics.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InventoryScreen.class)
public interface InventoryScreenAccess {
    @Invoker("extractRenderState")
    static EntityRenderState meowcosmetics$extract(LivingEntity entity) { throw new AssertionError(); }
}
