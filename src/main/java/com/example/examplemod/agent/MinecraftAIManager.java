package com.example.examplemod.agent;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.Screenshot;
import com.example.examplemod.aivillager.CustomVillager;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.internal.Utils.readBytes;

public class MinecraftAIManager {
    private final LocalPlayer player;
    private Tools tools;
    private CustomVillager villager;

    public MinecraftAIManager(LocalPlayer player, CustomVillager villager) {
        this.player = player;
        this.villager = villager;
        this.tools = new Tools(villager);
    }

    public CompletableFuture<Void> processUserInput(String userInput, String screenShotPath) {
        return CompletableFuture.runAsync(() -> {
            byte[] imageBytes = readBytes(screenShotPath);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            ImageContent imageContent = ImageContent.from(base64Data, "image/jpg");

            String prompt = "You are a helpful Minecraft Assistant";
            UserMessage userMessage = UserMessage.from(
                    TextContent.from(prompt + userInput),
                    imageContent
            );

            List<ChatMessage> conversation = new ArrayList<>();
            conversation.add(userMessage);

            boolean finished = false;
            while (!finished) {
                ChatRequest request = ChatRequest.builder()
                        .messages(conversation)
                        .toolSpecifications(ExampleMod.toolSpecifications)
                        .build();

                ChatResponse response = ExampleMod.model.chat(request);
                AiMessage aiMessage = response.aiMessage();
                conversation.add(aiMessage);

                if (aiMessage.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        executeTool(toolRequest, conversation);
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

    private void executeTool(ToolExecutionRequest toolRequest, List<ChatMessage> conversation) {
        try {
            List<Object> arguments = parseArguments(toolRequest.arguments());
            Method method = DynamicInvoker.getMethod(tools, toolRequest.name(), arguments);
            String result = (String) method.invoke(tools, arguments.toArray());

            conversation.add(ToolExecutionResultMessage.from(toolRequest, result));
        } catch (Exception e) {
            sendPlayerMessage("Error executing tool: " + e.getMessage());
        }
    }

    private void sendPlayerMessage(String message) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
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
}
