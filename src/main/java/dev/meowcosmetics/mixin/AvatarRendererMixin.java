package dev.meowcosmetics.mixin;

import dev.meowcosmetics.CosmeticLayer;
import dev.meowcosmetics.Cosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cosmetics on your own player only (client-side; nobody else sees them). */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void meowcosmetics$layer(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccess) this).meowcosmetics$addLayer(new CosmeticLayer((RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void meowcosmetics$me(Avatar entity, AvatarRenderState state, float partial, CallbackInfo ci) {
        Cosmetics.extract(state, entity == Minecraft.getInstance().player);
    }
}
