package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.DynamicInvoker;
import com.example.examplemod.agent.MinecraftAIManager;
import com.example.examplemod.agent.MinecraftAgent;
import com.example.examplemod.agent.Screenshot;
import com.example.examplemod.aivillager.CustomVillager;
import com.example.examplemod.network.SpawnEntitiesPacket;
import com.example.examplemod.network.SpawnVillagersPacket;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.example.examplemod.ExampleMod.toolSpecifications;
import static dev.langchain4j.internal.Utils.readBytes;

public class MyGuiScreen extends Screen {
    private final CustomVillager villager;

    private EditBox editBox;

    private LocalPlayer localPlayer;

    public MyGuiScreen(CustomVillager villager)
    {
        super(Component.literal("My Mod GUI"));
        this.villager = villager;
    }


    @Override
    protected void init() {
        super.init();
        String screenShotPath = Screenshot.captureScreenshot();
        // Crea l'editbox centrato orizzontalmente
        editBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal("Inserisci messaggio"));
        editBox.setMaxLength(Integer.MAX_VALUE);
        this.addRenderableWidget(editBox);

        Button buttonSend = Button.builder(Component.literal("Invia"), (button) -> {
            localPlayer = this.minecraft.player;
            if (localPlayer != null) {
                MinecraftAIManager aiManager = new MinecraftAIManager(localPlayer, villager);
                aiManager.processUserInput(editBox.getValue(), screenShotPath);
                this.minecraft.setScreen(null);
            }
        }).pos(this.width / 2 - 100, this.height / 2 + 10).width(200).build();

        this.addRenderableWidget(buttonSend);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Disegna lo sfondo e gli elementi della GUI
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Chiude la GUI se viene premuto ESC (codice 256)
        if (keyCode == 256) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
