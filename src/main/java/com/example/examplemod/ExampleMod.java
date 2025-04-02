package com.example.examplemod;

import com.example.examplemod.agent.MinecraftAgent;
import com.example.examplemod.agent.Tools;
import com.example.examplemod.aivillager.CustomVillager;
import com.example.examplemod.gui.MyGuiScreen;
import com.example.examplemod.network.OpenGuiPacket;
import com.example.examplemod.network.SpawnEntitiesPacket;
import com.example.examplemod.network.SpawnVillagersPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.*;
import net.minecraftforge.network.packets.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("example_block"))
            .mapColor(MapColor.STONE)
        )
    );
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
        () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().setId(ITEMS.key("example_block")))
    );

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("example_item"))
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build()
            )
        )
    );

    public static KeyMapping openGuiKey;

    public static ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();


    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ExampleMod.MODID);

    public static final RegistryObject<EntityType<CustomVillager>> CUSTOM_VILLAGER = ENTITIES.register("custom_villager",
            () -> EntityType.Builder.of(CustomVillager::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F) // Dimensioni standard del Villager
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("examplemod:custom_villager"))));


    public static List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(Tools.class);

    public static MinecraftAgent agent = AiServices.builder(MinecraftAgent.class)
            .chatLanguageModel(model)
            .tools(new Tools())
            .build();

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());


    private static final Integer PROTOCOL_VERSION = 1;
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static final SimpleChannel CHANNEL_ENTITIES = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "entities"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static final SimpleChannel CHANNEL_GUI = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "gui"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();




    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnvillager").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Level world = player.level();
            CustomVillager villager = new CustomVillager(CUSTOM_VILLAGER.get(), world);
            villager.setPos(player.getX(), player.getY(), player.getZ());
            world.addFreshEntity(villager);
            return 1;
        }));
    }

    public ExampleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        ENTITIES.register(modEventBus);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        int id = 0;

        // Register the SpawnVillagersPacket class with the channel
        CHANNEL.messageBuilder(SpawnVillagersPacket.class, id++)
                .encoder(SpawnVillagersPacket::encode)
                .decoder(SpawnVillagersPacket::decode)
                // consumerNetworkThread o consumerMainThread a seconda di dove vuoi gestire la logica
                .consumerMainThread((SpawnVillagersPacket::handle))
                .add();

        CHANNEL_ENTITIES.messageBuilder(SpawnEntitiesPacket.class, id++)
                .encoder(SpawnEntitiesPacket::encode)
                .decoder(SpawnEntitiesPacket::decode)
                .consumerMainThread(SpawnEntitiesPacket::handle)
                .add();

        CHANNEL_GUI.messageBuilder(OpenGuiPacket.class, id++)
                .encoder(OpenGuiPacket::encode)
                .decoder(OpenGuiPacket::decode)
                .consumerMainThread(OpenGuiPacket::handle)
                .add();

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        register(event.getServer().getCommands().getDispatcher());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            openGuiKey = new KeyMapping("key.mymod.opengui", GLFW.GLFW_KEY_C, "key.categories.mymod");
            //register the keybinding
            MinecraftForge.EVENT_BUS.register(openGuiKey);
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // Se il tasto definito viene premuto, apri la GUI
        if (openGuiKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new MyGuiScreen());
        }
    }
}
