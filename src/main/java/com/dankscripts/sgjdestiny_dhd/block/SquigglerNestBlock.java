package com.dankscripts.sgjdestiny_dhd.block;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos; import net.minecraft.core.particles.DustParticleOptions; import net.minecraft.core.particles.ParticleTypes; import net.minecraft.util.RandomSource; import net.minecraft.world.level.BlockGetter; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.Block; import net.minecraft.world.level.block.state.BlockBehaviour; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.phys.shapes.*;
public final class SquigglerNestBlock extends Block {
 private static final VoxelShape SHAPE=Shapes.or(box(1,0,1,15,5,15),box(2,5,2,6,13,14),box(10,5,2,14,11,14),box(6,5,2,10,9,6),box(6,5,10,10,12,14));
 public SquigglerNestBlock(){super(BlockBehaviour.Properties.of().strength(1.2F).noOcclusion());}
 @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
 @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
  if(r.nextInt(2)==0)l.addParticle(new DustParticleOptions(new Vector3f(.72F,.66F,.12F),.65F),p.getX()+.25D+r.nextDouble()*.5D,p.getY()+.65D+r.nextDouble()*.35D,p.getZ()+.25D+r.nextDouble()*.5D,(r.nextDouble()-.5D)*.012D,.018D+r.nextDouble()*.018D,(r.nextDouble()-.5D)*.012D);
  if(r.nextInt(5)==0)l.addParticle(ParticleTypes.SMOKE,p.getX()+.3D+r.nextDouble()*.4D,p.getY()+.7D,p.getZ()+.3D+r.nextDouble()*.4D,0,.025D,0);
 }
}
