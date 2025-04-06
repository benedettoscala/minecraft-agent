import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.MinecraftAIManager;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.internal.Utils.readBytes;
/*
public class UserInputPacket {
    private String userInput;
    private String screenShotPath;

    public UserInputPacket(String userInput, String screenShotPath) {
        this.userInput = userInput;
        this.screenShotPath = screenShotPath;
    }

    public static void encode(UserInputPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.userInput);
        buffer.writeUtf(msg.screenShotPath);
    }

    public static UserInputPacket decode(FriendlyByteBuf buffer) {
        String userInput = buffer.readUtf();
        String screenShotPath = buffer.readUtf();
        return new UserInputPacket(userInput, screenShotPath);
    }

    public static void handle(UserInputPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Get the server player
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                // Process the user input on the server
                MinecraftAIManager aiManager = new MinecraftAIManager(player, null); // Pass the villager instance if needed
                processUserInput(msg.userInput, msg.screenShotPath);
            }
        });
        ctx.setPacketHandled(true);
    }
}
*/