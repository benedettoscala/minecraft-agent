package com.example.examplemod.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Nonostante il nome "SpawnEntitiesPacket", ora questo packet
 * può gestire lo spawn di qualsiasi entità in base al suo ResourceLocation.
 */
public class SpawnEntitiesPacket {
    private final String entityTypeId;  // Esempio: "minecraft:zombie"
    private final int numEntities;      // Numero di entità da generare

    public SpawnEntitiesPacket(String entityTypeId, int numEntities) {
        this.entityTypeId = entityTypeId;
        this.numEntities = numEntities;
    }

    /**
     * Codifica i campi del packet nel buffer.
     */
    public static void encode(SpawnEntitiesPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.entityTypeId);
        buf.writeInt(packet.numEntities);
    }

    /**
     * Decodifica i campi del packet dal buffer.
     */
    public static SpawnEntitiesPacket decode(FriendlyByteBuf buf) {
        String entityTypeId = buf.readUtf();
        int numEntities = buf.readInt();
        return new SpawnEntitiesPacket(entityTypeId, numEntities);
    }

    /**
     * Gestione lato server del packet ricevuto.
     */
    public static void handle(SpawnEntitiesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                Vec3 look = player.getLookAngle();

                // Prova a interpretare la stringa (es. "minecraft:zombie") come un ResourceLocation
                ResourceLocation entityLocation = ResourceLocation.tryParse(packet.entityTypeId);
                if (entityLocation == null) {
                    player.sendSystemMessage(Component.literal("Nome di entità non valido: " + packet.entityTypeId)
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                // Recupera il tipo di entità corrispondente
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityLocation);
                if (entityType != null) {
                    // Genera "numEntities" entità
                    for (int i = 0; i < packet.numEntities; i++) {
                        Entity entity = entityType.create(level, null);
                        if (entity != null) {
                            // Posiziona l'entità qualche blocco in avanti, in base a i
                            double offset = 2 + i;
                            double x = player.getX() + look.x * offset;
                            double y = player.getY();
                            double z = player.getZ() + look.z * offset;

                            entity.setPos(x, y, z);
                            level.addFreshEntity(entity);
                        }
                    }

                    // Messaggio di conferma
                    player.sendSystemMessage(
                            Component.literal("Generate " + packet.numEntities + " entità di tipo: " + packet.entityTypeId)
                                    .withStyle(ChatFormatting.GREEN)
                    );
                } else {
                    // Caso in cui l'entità non esiste nel registry
                    player.sendSystemMessage(Component.literal("Tipo di entità non valido: " + packet.entityTypeId)
                            .withStyle(ChatFormatting.RED));
                }
            }
        });
        ctx.setPacketHandled(true);
    }



}
