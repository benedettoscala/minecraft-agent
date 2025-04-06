package com.example.examplemod.goals;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.ai.control.LookControl;
import java.util.EnumSet;

public class DestroyBlockGoal extends Goal {
    private final Mob mob;
    private BlockPos targetBlock;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float areaSize;
    private int numBlocks = 5; // Raggio di ricerca per i blocchi da distruggere

    public DestroyBlockGoal(Mob mob, double speedModifier, float stopDistance, float areaSize) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.navigation = mob.getNavigation();
        this.stopDistance = stopDistance;
        this.areaSize = areaSize;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        if (!(mob.getNavigation() instanceof PathNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for DestroyBlockGoal");
        }
    }

    @Override
    public boolean canUse() {
        // Rileva un blocco da distruggere
        this.targetBlock = findBlockToDestroy();
        return targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetBlock != null && !this.navigation.isDone() &&
                this.mob.distanceToSqr(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ()) > (this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.targetBlock = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (this.targetBlock != null && !this.mob.isLeashed()) {
            this.mob.getLookControl().setLookAt(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ(), 10.0F, (float) this.mob.getMaxHeadXRot());

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);

                double distance = this.mob.distanceToSqr(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                if (distance > (this.stopDistance * this.stopDistance)) {
                    this.navigation.moveTo(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ(), this.speedModifier);
                } else {
                    // Distruggi il blocco quando raggiunto
                    BlockState blockState = this.mob.level().getBlockState(this.targetBlock);
                    this.mob.level().setBlock(this.targetBlock, Blocks.AIR.defaultBlockState(), 3);

                    // Fai cadere l'oggetto corrispondente al blocco distrutto
                    Block.popResource(this.mob.level(), this.targetBlock, blockState.getBlock().asItem().getDefaultInstance());

                    this.navigation.stop();
                }
            }
        }
    }

    private BlockPos findBlockToDestroy() {
        // Cerca blocchi da distruggere (esempio: blocchi di terra) in un'area intorno al mob
        BlockPos mobPos = this.mob.blockPosition();
        Level level = this.mob.level();

        for (int x = -numBlocks; x <= numBlocks; x++) {
            for (int y = -numBlocks; y <= numBlocks; y++) {
                for (int z = -numBlocks; z <= numBlocks; z++) {
                    BlockPos offset = mobPos.offset(x, y, z);
                    if (level.getBlockState(offset).getBlock() == Blocks.DIRT) { // Modifica questo per cercare altri blocchi
                        return offset; // Restituisce il primo blocco di terra trovato
                    }
                }
            }
        }
        return null; // Se non trova nessun blocco da distruggere
    }
}
