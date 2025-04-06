package com.example.examplemod.aivillager;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.agent.MinecraftAIManager;
import com.example.examplemod.goals.DestroyBlockGoal;
import com.example.examplemod.gui.MyGuiScreen;
import com.example.examplemod.network.OpenGuiPacket;
import com.example.examplemod.network.ToggleFollowPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumSet;

public class CustomVillager extends Villager {
    private boolean followingPlayer = false;
    private Player targetPlayer;
    private int ticksUntilNextPathRecalculation = 0;
    private double pathedTargetX, pathedTargetY, pathedTargetZ;
    private final double speedModifier = 0.7D;

    private MinecraftAIManager aiManager;

    public CustomVillager(EntityType<? extends Villager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        //aggiungi cose a caso all'inventario
        this.inventory.addItem(new ItemStack(Blocks.BLUE_ICE.asItem(), 10));
    }

    public MinecraftAIManager getAiManager() {
        return aiManager;
    }

    public void setAiManager(MinecraftAIManager aiManager) {
        this.aiManager = aiManager;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createMobAttributes();
    }


    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            ExampleMod.CHANNEL_GUI.send(new OpenGuiPacket(this.getId()), PacketDistributor.PLAYER.with((ServerPlayer) pPlayer));
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Aggiungi il goal per distruggere blocchi
        this.goalSelector.addGoal(1, new DestroyBlockGoal(this, 1.0, 3.0f, 5.0f));  // velocità 1.0
    }


    /***
    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            toggleFollow(pPlayer);
        }
        return InteractionResult.SUCCESS;
    }***/

    public void toggleFollow(boolean pFollow) {
        if (this.level().isClientSide) {
            // Se siamo sul client, invia il pacchetto al server
            ExampleMod.CHANNEL_TOGGLEFOLLOW.send(new ToggleFollowPacket(this.getId(), pFollow), PacketDistributor.SERVER.noArg());
        } else {
            // Se siamo sul server, cambia direttamente il valore
            if (!pFollow) {
                followingPlayer = false;
                targetPlayer = null;
                //this.getNavigation().stop();  // FERMA il Villager quando smette di seguire
            } else {
                followingPlayer = true;
                targetPlayer = this.level().getNearestPlayer(this, 10);
            }
        }
    }

    public boolean isFollowingPlayer() {
        return followingPlayer;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    private Vec3 lastTargetPosition = null; // Memorizza l'ultima posizione del target
    private int moveCooldown = 40; // Tempo di attesa tra i calcoli di movimento (ad esempio, 5 ticks)

    @Override
    public void tick() {
        super.tick();

        //this.inventory.addItem(new ItemStack(Blocks.BLUE_ICE.asItem(), 10));
        if (targetPlayer == null) {
            targetPlayer = Minecraft.getInstance().player;
        }
        //Minecraft.getInstance().player.displayClientMessage(Component.literal(inventory.toString()), false);
        // Se non dobbiamo seguire il giocatore, fermiamo il movimento
        if (!followingPlayer) {
            //this.getNavigation().stop();
            return;
        }

        // Controlliamo la distanza tra il villager e il giocatore
        double distance = this.distanceTo(targetPlayer);

        // Controlliamo se dobbiamo ricalcolare il percorso
        if (moveCooldown <= 0 || lastTargetPosition == null || !lastTargetPosition.equals(targetPlayer.position())) {
            // Calcola un nuovo percorso solo se la posizione del giocatore è cambiata
            lastTargetPosition = targetPlayer.position();
            this.getNavigation().moveTo(targetPlayer, speedModifier);
            moveCooldown = 5; // Aggiungi un piccolo "cooldown" tra i calcoli del movimento (es. 5 ticks)
        } else {
            // Rimuovi il movimento se il villager è già in movimento verso il giocatore
            this.getNavigation().stop();
        }

        // Riduci il cooldown
        if (moveCooldown > 0) {
            moveCooldown--;
        }
    }


    private final SimpleContainer inventory = new SimpleContainer(9); // 9 slot, come un dispenser

    private void collectDroppedItems() {
        // Cerca gli oggetti nel raggio di 10 blocchi (modificabile)
        double searchRadius = 10.0;
        for (ItemEntity itemEntity : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(searchRadius))) {
            if (this.distanceTo(itemEntity) <= searchRadius) {
                ItemStack itemStack = itemEntity.getItem();

                // Prova a prendere l'oggetto e aggiungerlo all'inventario
                if (addItemToInventory(itemStack)) {
                    itemEntity.remove(RemovalReason.DISCARDED); // Rimuovi l'oggetto da terra
                }
            }
        }
    }

    public SimpleContainer getInventory() {

        return inventory;
    }

    public boolean addItemToInventory(ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) {
                inventory.setItem(i, stack);
                return true;
            } else if (ItemStack.isSameItem(slotStack, stack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
                int toAdd = Math.min(spaceLeft, stack.getCount());
                slotStack.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) return true;
            }
        }
        return false;
    }

    public boolean removeItemFromInventory(ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (ItemStack.isSameItem(slotStack, stack) && slotStack.getCount() >= stack.getCount()) {
                slotStack.shrink(stack.getCount());
                if (slotStack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    public void clearInventory() {
        inventory.clearContent();
    }

}
