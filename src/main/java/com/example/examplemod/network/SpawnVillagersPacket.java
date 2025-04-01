package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;
import java.util.function.Supplier;

public class SpawnVillagersPacket {

    private final int numVillagers;

    public SpawnVillagersPacket(int numVillagers) {
        this.numVillagers = numVillagers;
    }

    public static void encode(SpawnVillagersPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.numVillagers);
    }

    public static SpawnVillagersPacket decode(FriendlyByteBuf buf) {
        return new SpawnVillagersPacket(buf.readInt());
    }

    public static void handle(SpawnVillagersPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Ottieni il giocatore che ha inviato il pacchetto (server-side)
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                Vec3 look = player.getLookAngle();
                // Genera 10 villager lungo la direzione in cui il giocatore guarda
                for (int i = 0; i <  packet.numVillagers; i++) {

                    Villager villager = EntityType.VILLAGER.create(level, null);
                    if (villager != null) {
                        double offset = 2 + i; // Partiamo da 2 blocchi e incrementiamo l'offset per ogni villager
                        double x = player.getX() + look.x * offset;
                        double y = player.getY();
                        double z = player.getZ() + look.z * offset;
                        villager.setPos(x, y, z);
                        level.addFreshEntity(villager);
                    }
                }
                // Invia un messaggio al giocatore
                player.sendSystemMessage(Component.literal("Sono stati generati "+packet.numVillagers+ " villager!"));
            }
        });
        ctx.setPacketHandled(true);
    }
}
