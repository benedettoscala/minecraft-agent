package com.example.examplemod.network;

import com.example.examplemod.aivillager.CustomVillager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;


public class ToggleFollowPacket {
    private final int villagerId;
    private final boolean follow;

    public ToggleFollowPacket(int villagerId, boolean follow) {
        this.villagerId = villagerId;
        this.follow = follow;
    }

    public static void encode(ToggleFollowPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.villagerId);
        buf.writeBoolean(msg.follow);
    }

    public static ToggleFollowPacket decode(FriendlyByteBuf buf) {
        return new ToggleFollowPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ToggleFollowPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Assicura che siamo nel server!
            if (ctx.getSender() != null) {
                // Trova l'entità sul server
                CustomVillager villager = (CustomVillager) ctx.getSender().level().getEntity(msg.villagerId);
                if (villager != null) {
                    villager.toggleFollow(msg.follow);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
