package com.example.examplemod.aivillager;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.MinecraftAIManager;
import com.example.examplemod.gui.MyGuiScreen;
import com.example.examplemod.network.OpenGuiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

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
            ExampleMod.CHANNEL_GUI.send(new OpenGuiPacket(), PacketDistributor.PLAYER.with((ServerPlayer) pPlayer));
            // Qui potresti attivare una GUI o inviare un pacchetto di rete per aprire la chat con l'LLM
        }
        return InteractionResult.SUCCESS; // Evita di aprire il menu di scambio
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        return super.mobInteract(pPlayer, pHand);
    }
}
