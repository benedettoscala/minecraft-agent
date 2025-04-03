package com.example.examplemod.aivillager;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.MinecraftAIManager;
import com.example.examplemod.gui.MyGuiScreen;
import com.example.examplemod.network.OpenGuiPacket;
import com.example.examplemod.network.ToggleFollowPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumSet;

public class CustomVillager extends Villager {
    private boolean followingPlayer = false;
    private Player targetPlayer;
    private int ticksUntilNextPathRecalculation = 0;
    private double pathedTargetX, pathedTargetY, pathedTargetZ;
    private final double speedModifier = 1.0D;

    public CustomVillager(EntityType<? extends Villager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createMobAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            ExampleMod.CHANNEL_GUI.send(new OpenGuiPacket(this.getId()), PacketDistributor.PLAYER.with((ServerPlayer) pPlayer));
        }
        return InteractionResult.SUCCESS;
    }



    /***
    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            toggleFollow(pPlayer);
        }
        return InteractionResult.SUCCESS;
    }***/

    public void toggleFollow(boolean pFollow) {
        if (this.level().isClientSide) {
            // Se siamo sul client, invia il pacchetto al server
            ExampleMod.CHANNEL_TOGGLEFOLLOW.send(new ToggleFollowPacket(this.getId(), pFollow), PacketDistributor.SERVER.noArg());
        } else {
            // Se siamo sul server, cambia direttamente il valore
            if (!pFollow) {
                followingPlayer = false;
                targetPlayer = null;
                this.getNavigation().stop();  // FERMA il Villager quando smette di seguire
            } else {
                followingPlayer = true;
                targetPlayer = this.level().getNearestPlayer(this, 10);
            }
        }
    }

    public boolean isFollowingPlayer() {
        return followingPlayer;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    @Override
    public void tick() {
        super.tick();

        if (followingPlayer && targetPlayer == null) {
            targetPlayer= Minecraft.getInstance().player;
        }
        if(!followingPlayer) {
            this.getNavigation().stop();
            return;
        }

        this.getNavigation().moveTo(targetPlayer, speedModifier);
    }


}
