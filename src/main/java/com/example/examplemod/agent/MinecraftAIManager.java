package com.example.examplemod.agent;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.aivillager.CustomVillager;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.json.JSONObject;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.internal.Utils.readBytes;

public class MinecraftAIManager {
    private final LocalPlayer player;
    private final Tools tools;
    private final List<ChatMessage> chatHistory = new ArrayList<>(); // 🔁 mantiene la conversazione
    private static final int MAX_MESSAGES = 12; // o una soglia in token stimata

    public MinecraftAIManager(LocalPlayer player, CustomVillager villager) {
        this.player = player;
        this.tools = new Tools(villager);
    }

    public void processUserInput(String userInput, String screenShotPath) {
        CompletableFuture.runAsync(() -> {
            byte[] imageBytes = readBytes(screenShotPath);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            ImageContent imageContent = ImageContent.from(base64Data, "image/jpg");

            String prompt = "You are a villager from Minecraft. Your primary language consists of simple sounds like 'Hmm' and 'Hrmm,' but you can communicate in a way that is understandable to others. You are an expert in your trade, whether it's farming, blacksmithing, being a librarian, or another typical villager role. Always respond as a true villager would, with the mindset of someone who trades emeralds for goods and fears raids from pillagers.You have access to a screenshot of what the user is seeing, for informed question answering, in the screenshot there may be a villager, that is you. The user asked the following question:";
            UserMessage userMessage = UserMessage.from(
                    TextContent.from(prompt + userInput),
                    imageContent
            );

            chatHistory.add(userMessage);

            // 🔁 Se la storia è troppo lunga, fai un riassunto
            if (chatHistory.size() > MAX_MESSAGES) {
                summarizeConversation();
            }


            boolean finished = false;
            while (!finished) {
                ChatRequest request = ChatRequest.builder()
                        .messages(chatHistory)
                        .toolSpecifications(ExampleMod.toolSpecifications)
                        .build();

                ChatResponse response = ExampleMod.model.chat(request);
                AiMessage aiMessage = response.aiMessage();
                chatHistory.add(aiMessage);

                if (aiMessage.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        executeTool(toolRequest);
                        if (toolRequest.name().equals("finalAnswer")) {
                            finished = true;
                            break;
                        }
                    }
                } else {
                    sendPlayerMessage(aiMessage.text());
                    break;
                }
            }
            sendPlayerMessage("The agent has finished thinking");
        });
    }

    private void executeTool(ToolExecutionRequest toolRequest) {
        try {
            List<Object> arguments = parseArguments(toolRequest.arguments());
            Method method = DynamicInvoker.getMethod(tools, toolRequest.name(), arguments);
            String result = (String) method.invoke(tools, arguments.toArray());

            chatHistory.add(ToolExecutionResultMessage.from(toolRequest, result));
        } catch (Exception e) {
            sendPlayerMessage("Error executing tool: " + e.getMessage());
        }
    }

    private void summarizeConversation() {
        sendPlayerMessage("Conversation is too long. Summarizing...");

        List<ChatMessage> messagesToSummarize = new ArrayList<>(chatHistory);
        chatHistory.clear(); // puliamo subito per evitare overflow

        String summaryPrompt = "Summarize the following Minecraft villager conversation for memory retention. Maintain important context and recent facts:";

        List<ChatMessage> summaryInput = new ArrayList<>();
        summaryInput.add(UserMessage.from(TextContent.from(summaryPrompt)));
        summaryInput.addAll(messagesToSummarize);

        ChatRequest summaryRequest = ChatRequest.builder()
                .messages(summaryInput)
                .build();

        ChatResponse summaryResponse = ExampleMod.model.chat(summaryRequest);
        AiMessage summary = summaryResponse.aiMessage();

        // 🔁 Aggiungiamo il riassunto come base della nuova chat
        chatHistory.add(UserMessage.from(TextContent.from("Conversation summary:\n" + summary.text())));
    }

    private void sendPlayerMessage(String message) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    public static List<Object> parseArguments(String arguments) {
        List<Object> parsedArgs = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(arguments);

        for (Object key : jsonObject.keySet()) {
            Object value = jsonObject.get(key.toString());
            parsedArgs.add(value);
        }

        // invert list
        for (int i = 0; i < parsedArgs.size() / 2; i++) {
            Object temp = parsedArgs.get(i);
            parsedArgs.set(i, parsedArgs.size() - 1 - i);
            parsedArgs.set(parsedArgs.size() - 1 - i, temp);
        }

        return parsedArgs;
    }
}
