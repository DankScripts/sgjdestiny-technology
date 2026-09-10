package com.dankscripts.sgjdestiny_dhd.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** Small pack-hunting predator native to the jungle planet. */
public final class SquigglerEntity extends Silverfish {
    private BlockPos retreatNest;
    public SquigglerEntity(EntityType<? extends SquigglerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 4;
        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
    }

    @Override protected PathNavigation createNavigation(Level level) { return new WallClimberNavigation(this, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    public void tick() {
        // Dead mobs must always reach LivingEntity's death tick. Returning from
        // here froze daylight casualties forever at zero health.
        if (this.isAlive() && !this.level().isClientSide && daylightIsApproaching()) {
            retreatToNest();
        }
        LivingEntity targetBeforeTick = this.getTarget();
        boolean inLeaves = this.level().getBlockState(this.blockPosition()).is(BlockTags.LEAVES)
                || this.level().getBlockState(this.blockPosition().above()).is(BlockTags.LEAVES);
        boolean leavesAhead = targetBeforeTick != null && targetBeforeTick.getY() > this.getY()
                && (this.level().getBlockState(this.blockPosition().above(2)).is(BlockTags.LEAVES)
                || this.level().getBlockState(BlockPos.containing(this.position().add(
                        targetBeforeTick.position().subtract(this.position()).normalize().scale(0.7D)))).is(BlockTags.LEAVES));
        this.noPhysics = inLeaves || leavesAhead;
        super.tick();
        LivingEntity target = this.getTarget();
        boolean pursuingUp = target != null && target.getY() > this.getY() + 0.7D;
        this.setNoGravity(inLeaves || (pursuingUp && this.horizontalCollision));
        if (target != null && (inLeaves || pursuingUp)) {
            double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
            double horizontal = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
            double climb = inLeaves || this.horizontalCollision ? 0.24D : this.getDeltaMovement().y;
            this.setDeltaMovement(dx / horizontal * 0.16D, climb, dz / horizontal * 0.16D);
            this.getNavigation().moveTo(target, 1.25D);
        } else if (this.horizontalCollision && target != null) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.24D, this.getDeltaMovement().z);
        } else if (!this.onGround() && target != null && this.getY() > target.getY() + 2.0D
                && this.distanceToSqr(target) < 36.0D) {
            var toward = target.position().subtract(this.position()).normalize().scale(0.18D);
            this.setDeltaMovement(toward.x, Math.min(this.getDeltaMovement().y, -0.12D), toward.z);
        }
    }

    private boolean daylightIsApproaching() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 22000L || time < 13000L;
    }

    private void retreatToNest() {
        this.setTarget(null);
        if ((retreatNest == null || !this.level().getBlockState(retreatNest)
                .is(com.dankscripts.sgjdestiny_dhd.registry.ModBlocks.SQUIGGLER_NEST.get()))
                && this.tickCount % 20 == 0) {
            retreatNest = BlockPos.findClosestMatch(this.blockPosition(), 64, 24,
                    pos -> this.level().getBlockState(pos)
                            .is(com.dankscripts.sgjdestiny_dhd.registry.ModBlocks.SQUIGGLER_NEST.get()))
                    .map(BlockPos::immutable).orElse(null);
        }
        if (retreatNest == null) return;
        this.getNavigation().moveTo(retreatNest.getX() + 0.5D, retreatNest.getY() + 0.6D,
                retreatNest.getZ() + 0.5D, 1.35D);
        if (this.distanceToSqr(retreatNest.getX() + 0.5D, retreatNest.getY() + 0.5D,
                retreatNest.getZ() + 0.5D) < 1.7D) this.discard();
    }

    @Override public boolean onClimbable() { return this.horizontalCollision || this.noPhysics; }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
        }
        return hurt;
    }
}
