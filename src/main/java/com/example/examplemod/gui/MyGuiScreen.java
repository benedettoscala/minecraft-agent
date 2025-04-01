package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.DynamicInvoker;
import com.example.examplemod.agent.MinecraftAgent;
import com.example.examplemod.agent.Screenshot;
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

    private EditBox editBox;

    private LocalPlayer localPlayer;

    public MyGuiScreen() {
        super(Component.literal("My Mod GUI"));
    }


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
    String spawnVillager(@P("Number of Villagers To Spawn") Integer numVillagers) {
        ExampleMod.CHANNEL.send(new SpawnVillagersPacket(numVillagers), PacketDistributor.SERVER.noArg());
        return numVillagers + " villagers have been spawned successfully, no need to spawn others!";
    }

    @Tool("Spawn the provided number of entities, giving you know the entity type ID")
    String spawnEntites(@P("Entity Type ID of the entity to Spawn") String entityTypeId, @P("Number of Entities To Spawn") Integer numEntities) {
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
    String finalAnswer(String answer) {
        localPlayer = Minecraft.getInstance().player;
        if( localPlayer != null ){
            localPlayer.displayClientMessage(Component.literal(answer), false);
        }

        return "<|END|>";
    }
    private static Thread llmThread = null;



    @Override
    protected void init() {
        super.init();
        String screenShotPath = Screenshot.captureScreenshot();
        // Crea l'editbox centrato orizzontalmente
        editBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal("Inserisci messaggio"));
        editBox.setMaxLength(Integer.MAX_VALUE);
        this.addRenderableWidget(editBox);

        Button buttonSend = Button.builder(Component.literal("Invia"), (button) -> {
            // Invia un pacchetto al server per generare i villager
            localPlayer = this.minecraft.player;

            byte[] imageBytes = readBytes(screenShotPath);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            ImageContent imageContent = ImageContent.from(base64Data, "image/jpg");

            llmThread = new Thread(() -> {
                // Prepara il messaggio iniziale con il testo e l'immagine
                String prompt = "You are a helpful Minecraft Assistant with the ability to interact with the game world based on the player's requests. For every input from the player, you must analyze the best tool to use and determine the most appropriate parameters. You can invoke multiple tools, a single tool, or none at all, depending on the situation. However, you must always call the finalAnswer tool at the end of the interaction.\n" +
                        "\n" +
                        "you have an image of the player's current view in the game. Use this visual context to enhance your responses and make informed decisions";
                UserMessage userMessage = UserMessage.from(
                        TextContent.from(prompt + editBox.getValue()),
                        imageContent
                );

                // Inizializza la conversazione con il messaggio dell'utente
                List<ChatMessage> conversation = new ArrayList<>();
                conversation.add(userMessage);

                boolean finished = false;
                ChatResponse response = null;

                // Ciclo continuo fino a quando non si riceve "finito" oppure non ci sono richieste di tool
                while (!finished) {
                    // Costruisce la richiesta con la conversazione aggiornata
                    ChatRequest request = ChatRequest.builder()
                            .messages(conversation)
                            .toolSpecifications(toolSpecifications)
                            .build();

                    response = ExampleMod.model.chat(request);
                    AiMessage aiMessage = response.aiMessage();
                    conversation.add(aiMessage);

                    // Se il messaggio dell'AI contiene richieste di esecuzione tool, processiamole
                    if (aiMessage.hasToolExecutionRequests()) {
                        for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                            String toolName = toolRequest.name();
                            List<Object> arguments = null;
                            Method method = null;
                            String result = null;

                            try {
                                arguments = parseArguments(toolRequest.arguments());
                                method = DynamicInvoker.getMethod(this, toolName, arguments);
                                result = (String) method.invoke(this, arguments.toArray());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            // Crea e aggiunge il messaggio di risultato dell'esecuzione del tool
                            ToolExecutionResultMessage toolResultMessage = ToolExecutionResultMessage.from(toolRequest, result);
                            conversation.add(toolResultMessage);

                            // Se il tool restituisce "finito", interrompiamo il ciclo
                            if ("<|END|>".equalsIgnoreCase(result.trim())) {
                                finished = true;
                                break;
                            }
                        }
                    } else {
                        finalAnswer(aiMessage.text());
                        break;
                    }
                }
                minecraft.player.displayClientMessage(Component.literal("The agent has finished thinking"), false);
            });
            llmThread.start();

            minecraft.player.displayClientMessage(Component.literal("The agent is thinking..."), false);
            this.minecraft.setScreen(null);
        }).pos(this.width / 2 - 100, this.height / 2 + 10).width(200).build();

        this.addRenderableWidget(buttonSend);
    }

    // Parse arguments to a list
    public static List<Object> parseArguments(String arguments) throws Exception {
        List<Object> parsedArgs = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(arguments); // Parse the JSON array

        // Iterate through the array and add each element to the list
        for (Object argument : jsonObject.keySet()) {
            Object ArgumentValue = jsonObject.get(argument.toString());
            // Add more checks here to convert types based on expected types
            parsedArgs.add(ArgumentValue);
        }

        //invert list
        for (int i = 0; i < parsedArgs.size() / 2; i++) {
            Object temp = parsedArgs.get(i);
            parsedArgs.set(i, parsedArgs.get(parsedArgs.size() - 1 - i));
            parsedArgs.set(parsedArgs.size() - 1 - i, temp);
        }

        return parsedArgs;
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
