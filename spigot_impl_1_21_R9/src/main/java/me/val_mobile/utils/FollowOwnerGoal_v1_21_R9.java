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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.event.entity.EntityTeleportEvent;

import java.util.EnumSet;

public class FollowOwnerGoal_v1_21_R9 extends Goal {
    private final RSVPet pet;
    private LivingEntity owner;
    private final LevelReader level;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;
    private final boolean canFly;

    // Temporary method to move the mob - Need testing
    public void moveMob(Mob mob, double x, double y, double z, float yaw, float pitch) {
        PathNavigation navigation = mob.getNavigation();
    
        navigation.moveTo(x, y, z, 1.0); 
    
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);
    }

    public FollowOwnerGoal_v1_21_R9(RSVPet pet, double d0, float f, float f1, boolean flag) {
        this.pet = pet;
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();
        this.level = mob.level();
        this.speedModifier = d0;
        this.navigation = mob.getNavigation();
        this.startDistance = f;
        this.stopDistance = f1;
        this.canFly = flag;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        if (!(mob.getNavigation() instanceof GroundPathNavigation) && !(mob.getNavigation() instanceof FlyingPathNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    public boolean canUse() {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();
        Player owner = ((CraftPlayer) pet.getOwner()).getHandle();
        if (owner == null) {
            return false;
        } else if (mob.distanceToSqr(owner) < (double)(startDistance * startDistance)) {
            return false;
        } else {
            this.owner = owner;
            return true;
        }
    }

    public boolean canContinueToUse() {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        return !navigation.isDone() && (mob.distanceToSqr(owner) > (double) (stopDistance * stopDistance));
    }

    public void start() {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        timeToRecalcPath = 0;
        oldWaterCost = mob.getPathfindingMalus(PathType.WATER);
        mob.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public void stop() {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        owner = null;
        navigation.stop();
        mob.setPathfindingMalus(PathType.WATER, oldWaterCost);
    }

    public void tick() {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        mob.getLookControl().setLookAt(owner, 10.0F, (float) mob.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = adjustedTickDelay(10);
            if (!mob.isLeashed() && !mob.isPassenger()) {
                if (mob.distanceToSqr(owner) >= 144.0) {
                    teleportToOwner();
                } else {
                    navigation.moveTo(owner, speedModifier);
                }
            }
        }

    }

    private void teleportToOwner() {
        BlockPos blockposition = owner.blockPosition();

        for(int i = 0; i < 10; ++i) {
            int j = randomIntInclusive(-3, 3);
            int k = randomIntInclusive(-1, 1);
            int l = randomIntInclusive(-3, 3);
            boolean flag = maybeTeleportTo(blockposition.getX() + j, blockposition.getY() + k, blockposition.getZ() + l);
            if (flag) {
                return;
            }
        }

    }

    private boolean maybeTeleportTo(int i, int j, int k) {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        if (Math.abs((double)i - owner.getX()) < 2.0 && Math.abs((double)k - owner.getZ()) < 2.0) {
            return false;
        } else if (!canTeleportTo(new BlockPos(i, j, k))) {
            return false;
        } else {
            CraftEntity entity = mob.getBukkitEntity();
            Location to = new Location(entity.getWorld(), (double)i + 0.5, j, (double)k + 0.5, mob.getYRot(), mob.getXRot());
            EntityTeleportEvent event = new EntityTeleportEvent(entity, entity.getLocation(), to);
            mob.level().getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            } else {
                to = event.getTo();
                // mob.moveTo(to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
                moveMob(mob, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
                navigation.stop();
                return true;
            }
        }
    }

    private boolean canTeleportTo(BlockPos blockposition) {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(mob, blockposition.mutable());
        if (pathtype != PathType.WALKABLE) {
            return false;
        } else {
            BlockState iblockdata = level.getBlockState(blockposition.below());
            if (!canFly && iblockdata.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockposition1 = blockposition.subtract(mob.blockPosition());
                return level.noCollision(mob, mob.getBoundingBox().move(blockposition1));
            }
        }
    }

    private int randomIntInclusive(int i, int j) {
        Mob mob = (Mob) ((CraftEntity) pet.getEntity()).getHandle();

        return mob.getRandom().nextInt(j - i + 1) + i;
    }
}
