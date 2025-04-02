package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.aivillager.CustomVillager;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ExampleMod.CUSTOM_VILLAGER.get(), CustomVillager.createAttributes().build());
    }
}
