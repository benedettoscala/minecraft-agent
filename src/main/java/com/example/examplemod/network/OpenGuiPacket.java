package com.example.examplemod.network;

import com.example.examplemod.gui.MyGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class OpenGuiPacket {
    public OpenGuiPacket() {} // Costruttore vuoto

    public static void encode(OpenGuiPacket msg, FriendlyByteBuf buf) {}

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket();
    }

    public static void handle(OpenGuiPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new MyGuiScreen()); // Apri la GUI lato client
        });
        ctx.setPacketHandled(true);
    }
}
