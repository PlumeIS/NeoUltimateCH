package cn.plumc.ultimatech.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class EffectProvider {
    public static void second(List<ServerPlayer> players){
        for(ServerPlayer player : players){
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 0, false, false));
        }
    }
}
