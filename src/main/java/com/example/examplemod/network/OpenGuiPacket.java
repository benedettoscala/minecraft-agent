package com.example.examplemod.network;

import com.example.examplemod.aivillager.CustomVillager;
import com.example.examplemod.gui.MyGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class OpenGuiPacket {

    private final int villagerId;

    public OpenGuiPacket(int villagerId) {
        this.villagerId = villagerId;
    } // Costruttore vuoto

    public static void encode(OpenGuiPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.villagerId);
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket(buf.readInt());
    }

    public static void handle(OpenGuiPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.villagerId);
            if (entity instanceof CustomVillager villager) {
                Minecraft.getInstance().setScreen(new MyGuiScreen(villager)); // Apri la GUI lato client
            }

        });
        ctx.setPacketHandled(true);
    }
}
