package dev.meowcosmetics.mixin;

import dev.meowcosmetics.Cosmetics;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** What your own player wears in this render (null for everyone else), for the cosmetics. */
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements Cosmetics.Me {
    @Unique private Cosmetics.Look meowcosmetics$look;

    @Override public Cosmetics.Look meowcosmetics$look() { return meowcosmetics$look; }
    @Override public void meowcosmetics$setLook(Cosmetics.Look look) { meowcosmetics$look = look; }
}
