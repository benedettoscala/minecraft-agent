package com.example.examplemod.aivillager;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CustomVillager extends Villager {
    public CustomVillager(EntityType<? extends Villager> pEntityType, Level pLevel) {

        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createMobAttributes();
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            pPlayer.displayClientMessage(Component.literal("Ciao! Dimmi cosa vuoi sapere!"), false);
            // Qui potresti attivare una GUI o inviare un pacchetto di rete per aprire la chat con l'LLM
        }
        return InteractionResult.SUCCESS; // Evita di aprire il menu di scambio
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        return super.mobInteract(pPlayer, pHand);
    }
}
