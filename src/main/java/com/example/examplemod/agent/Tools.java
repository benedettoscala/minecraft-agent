package com.example.examplemod.agent;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.SpawnEntitiesPacket;
import com.example.examplemod.network.SpawnVillagersPacket;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class Tools {

    @Tool("Get all the entity types IDS available in the game. You should use it when you need to know the entity type ID of an entity")
    public String getAllEntityTypeIds() {
        List<String> list = new ArrayList<>();

        // ForgeRegistries.ENTITY_TYPES.getKeys() restituisce un Set di ResourceLocation
        for (ResourceLocation resourceLocation : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            // Convertilo in stringa (es. "minecraft:zombie", "minecraft:cow", ecc.)
            list.add(resourceLocation.toString());
        }

        return list.toString();
    }

    @Tool("Spawn the provided number of villagers")
    public String spawnVillager(@P("Number of Villagers To Spawn") Integer numVillagers) {
        ExampleMod.CHANNEL.send(new SpawnVillagersPacket(numVillagers), PacketDistributor.SERVER.noArg());
        return numVillagers + " villagers have been spawned successfully, no need to spawn others!";
    }

    @Tool("Spawn the provided number of entities, giving you know the entity type ID")
    public String spawnEntites(@P("Entity Type ID of the entity to Spawn") String entityTypeId, @P("Number of Entities To Spawn") Integer numEntities) {
        // Prova a interpretare la stringa (es. "minecraft:zombie") come un ResourceLocation
        ResourceLocation entityLocation = ResourceLocation.tryParse(entityTypeId);
        if (entityLocation == null) {
            return "Invalid entity name: " + entityTypeId;
        }

        // Invia un pacchetto al server per generare le entità
        ExampleMod.CHANNEL_ENTITIES.send(new SpawnEntitiesPacket(entityTypeId, numEntities), PacketDistributor.SERVER.noArg());
        return numEntities + " entities have been spawned successfully!";
    }

    @Tool("Final answer to give to the user after performing the action or actions, or if there are no more actions to do")
    public String finalAnswer(String answer) {
        Player localPlayer = Minecraft.getInstance().player;
        if( localPlayer != null ){
            localPlayer.displayClientMessage(Component.literal(answer), false);
        }

        return "<|END|>";
    }
}
