/*
    Copyright (C) 2025  Val_Mobile

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.val_mobile.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R5.event.CraftEventFactory;

public class EndermanTakeBlockGoal_v1_21_R9 extends Goal {
    private final EnderMan enderman;

    public EndermanTakeBlockGoal_v1_21_R9(EnderMan enderman) {
        this.enderman = enderman;
    }

    public boolean canUse() {
        return this.enderman.getCarriedBlock() == null && (getServerLevel(this.enderman).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.enderman.getRandom().nextInt(reducedTickDelay(20)) == 0);
    }

    public void tick() {
        RandomSource randomsource = enderman.getRandom();
        Level world = enderman.level();
        int i = Mth.floor(enderman.getX() - 2.0 + randomsource.nextDouble() * 4.0);
        int j = Mth.floor(enderman.getY() + randomsource.nextDouble() * 3.0);
        int k = Mth.floor(enderman.getZ() - 2.0 + randomsource.nextDouble() * 4.0);
        BlockPos blockposition = new BlockPos(i, j, k);
        BlockState iblockdata = world.getBlockState(blockposition);
        Vec3 vec3d = new Vec3((double)enderman.getBlockX() + 0.5, (double)j + 0.5, (double)enderman.getBlockZ() + 0.5);
        Vec3 vec3d1 = new Vec3((double)i + 0.5, (double)j + 0.5, (double)k + 0.5);
        BlockHitResult movingobjectpositionblock = world.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, enderman));
        boolean flag = movingobjectpositionblock.getBlockPos().equals(blockposition);
        if (iblockdata.is(BlockTags.ENDERMAN_HOLDABLE) && flag && !CraftEventFactory.callEntityChangeBlockEvent(enderman, blockposition, Blocks.AIR.defaultBlockState())) {
            world.removeBlock(blockposition, false);
            world.gameEvent(GameEvent.BLOCK_DESTROY, blockposition, GameEvent.Context.of(enderman, iblockdata));
            enderman.setCarriedBlock(iblockdata.getBlock().defaultBlockState());
        }

    }
}
