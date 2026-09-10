package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.entity.SquigglerEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid="sgjdestiny_dhd", bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class SquigglerNestEvents {
 private static final ResourceLocation JUNGLE=new ResourceLocation("sgjourney","jungle_planet");
 private static final Map<BlockPos,Long> NEXT_SPAWN=new HashMap<>();
 private static final Set<Long> POPULATED=new HashSet<>();
 private SquigglerNestEvents(){}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){
  if(e.phase!=TickEvent.Phase.END || !(e.level instanceof ServerLevel level) || !level.dimension().location().equals(JUNGLE)
    || level.getGameTime()%80!=0)return;
  for(var player:level.players())populate(level,player.getBlockX()>>4,player.getBlockZ()>>4);
  if(!level.isNight() || level.getDifficulty()==Difficulty.PEACEFUL)return;
  for(var player:level.players()){
   if(level.getEntitiesOfClass(SquigglerEntity.class,new AABB(player.blockPosition()).inflate(48)).size()>=16)continue;
   for(int a=0;a<64;a++){
    int x=player.getBlockX()+level.random.nextInt(49)-24,z=player.getBlockZ()+level.random.nextInt(49)-24;
    BlockPos nest=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z)).below();
    if(!level.getBlockState(nest).is(ModBlocks.SQUIGGLER_NEST.get()) || level.getMaxLocalRawBrightness(nest.above())>7
      || NEXT_SPAWN.getOrDefault(nest,0L)>level.getGameTime())continue;
    NEXT_SPAWN.put(nest.immutable(),level.getGameTime()+1200);
    for(int i=0,n=2+level.random.nextInt(3);i<n;i++){
     var mob=ModEntities.SQUIGGLER.get().create(level); if(mob==null)continue;
     mob.moveTo(nest.getX()+.5,nest.getY()+1.05,nest.getZ()+.5,level.random.nextFloat()*360,0);
     mob.finalizeSpawn(level,level.getCurrentDifficultyAt(nest),MobSpawnType.SPAWNER,null,null); level.addFreshEntity(mob);
    } break;
   }
  }
 }
 private static void populate(ServerLevel level,int centerX,int centerZ){
  for(int cx=centerX-2;cx<=centerX+2;cx++)for(int cz=centerZ-2;cz<=centerZ+2;cz++){
   long key=net.minecraft.world.level.ChunkPos.asLong(cx,cz); if(!POPULATED.add(key)||!level.hasChunk(cx,cz))continue;
   long h=level.getSeed()^(cx*341873128712L)^(cz*132897987541L); if(Math.floorMod(h,4)==0)continue;
   for(int i=0,count=1;i<count;i++){
    long n=h; int x=(cx<<4)+2+(int)Math.floorMod(n>>>8,12),z=(cz<<4)+2+(int)Math.floorMod(n>>>20,12);
    BlockPos p=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z)); var b=level.getBlockState(p.below());
    if(level.getBlockState(p).canBeReplaced()&&(b.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||b.is(net.minecraft.world.level.block.Blocks.DIRT)||b.is(net.minecraft.world.level.block.Blocks.PODZOL)))level.setBlock(p,ModBlocks.SQUIGGLER_NEST.get().defaultBlockState(),3);
   }
  }
 }
}
