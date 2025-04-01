package com.example.examplemod.agent;

import dev.langchain4j.service.UserMessage;

public interface MinecraftAgent{
    String ask(@UserMessage String question,@UserMessage String imagePath);
}
