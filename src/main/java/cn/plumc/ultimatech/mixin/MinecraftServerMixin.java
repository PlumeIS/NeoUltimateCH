package cn.plumc.ultimatech.mixin;

import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.utils.TickUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Unique
    private int ultimateCH$tickCounter = 0;

    @Shadow @Nullable public abstract ServerLevel getLevel(ResourceKey<Level> pDimension);

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTickServer(CallbackInfo ci) {
        UltimateCH.INSTANCE.tick();
        TickUtil.tick();
        ultimateCH$tickCounter++;
        if (ultimateCH$tickCounter % 20 == 0) {
            UltimateCH.INSTANCE.second();
            ultimateCH$tickCounter = 0;
        }
    }
}
