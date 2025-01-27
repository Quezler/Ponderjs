package com.kotakotik.ponderjs.util;

import com.simibubi.create.foundation.ponder.ElementLink;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.Selection;
import com.simibubi.create.foundation.ponder.elements.EntityElement;
import dev.latvian.kubejs.KubeJSRegistries;
import dev.latvian.kubejs.bindings.BlockWrapper;
import dev.latvian.kubejs.block.predicate.BlockIDPredicate;
import dev.latvian.kubejs.entity.EntityJS;
import dev.latvian.kubejs.util.MapJS;
import dev.latvian.kubejs.util.UtilsJS;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.LazyValue;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class SceneBuilderJS implements ISceneBuilderJS {
    public final SceneBuilder internal;
//    public final PonderPaletteWrapper palette = new PonderPaletteWrapper();

    public SceneBuilderJS(SceneBuilder scene) {
        this.internal = scene;
    }

//    public EntityJS createEntityJS(World world, Entity entity) {
//        return new EntityJS(new WorldJS(world) {
//            @Override
//            public ScriptType getSide() {
//                return ScriptType.STARTUP;
//            }
//
//            @Override
//            public PlayerDataJS<?, ?> getPlayerData(PlayerEntity playerEntity) {
//                return null;
//            }
//        }, entity);
//    }

// aaaaaa i could have just used MapJS.of(nbt)
//    public JsonObject nbtToJson(CompoundNBT nbt) {
//        BuiltinKubeJSPlugin
//        JsonObject json = new JsonObject();
//        for(String key : nbt.getAllKeys()) {
//            INBT val = nbt.get(key);
//            if(val instanceof ByteNBT) {
//                json.addProperty(key, nbt.getBoolean(key));
//            } else if(val instanceof NumberNBT) {
//                json.addProperty(key, ((NumberNBT) val).getAsNumber());
//            } /* else if(val instanceof StringNBT) {
//                json.addProperty(key, val.toString());
//            } */ else {
//                json.addProperty(key, val.toString());
//            }
////            json.addProperty(key, );
//        }
//        return json;
//    }


    @Override
    public SceneBuilder getInternal() {
        return internal;
    }

    @Override
    public SceneBuilder.OverlayInstructions getOverlay() {
        return getInternal().overlay;
    }

    public LazyValue<WorldInstructionsJS> worldInstructionsJS = new LazyValue<>(() ->
            new WorldInstructionsJS(getInternal().world, this));

    @Override
    public WorldInstructionsJS getWorld() {
       return worldInstructionsJS.get();
    }

    @Override
    public SceneBuilder.DebugInstructions getDebug() {
        return getInternal().debug;
    }

    @Override
    public SceneBuilder.EffectInstructions getEffects() {
        return getInternal().effects;
    }

    public LazyValue<SpecialInstructionsJS> specialInstructionsJS = new LazyValue<>(() ->
            new SpecialInstructionsJS(getInternal().special));

    @Override
    public SpecialInstructionsJS getSpecial() {
        return specialInstructionsJS.get();
    }

    public static class SpecialInstructionsJS implements ISpecialInstructionsJS {

        private SceneBuilder.SpecialInstructions internal;

        public SpecialInstructionsJS(SceneBuilder.SpecialInstructions internal) {
            this.internal = internal;
        }

        @Override
        public SceneBuilder.SpecialInstructions getInternal() {
            return internal;
        }
    }

    public static class WorldInstructionsJS implements ISceneBuilderJS.IWorldInstructionsJS {

        private final SceneBuilder.WorldInstructions internal;
        private final SceneBuilderJS sceneBuilder;

        public WorldInstructionsJS(SceneBuilder.WorldInstructions internal, SceneBuilderJS sceneBuilder) {
            this.internal = internal;
            this.sceneBuilder = sceneBuilder;
        }

        public Consumer<CompoundNBT> mapJsConsumerToNBT(Object pos, UnaryOperator<MapJS> sup) {
            return nbt -> {
                Objects.requireNonNull(nbt, "Could not find NBT in selection " +
                        pos + ", your selection might include non-tiles!");
                CompoundNBT n = MapJS.nbt(
                        Objects.requireNonNull(sup.apply(Objects.requireNonNull(MapJS.of(nbt))),
                                "Null returned for tile NBT")
                );
                nbt.merge(n);
            };
        }

        public void updateTileNBT(Selection selection, UnaryOperator<MapJS> sup) {
            modifyTileNBT(selection, TileEntity.class, mapJsConsumerToNBT(selection, sup));
        }

        public void modifyTileNBT(Selection selection, MapJS obj) {
            updateTileNBT(selection, $ -> obj);
        }

        public void updateTileNBT(Selection selection, Class<? extends TileEntity> teType, UnaryOperator<MapJS> sup, boolean reDrawBlocks) {
//            TypeToken<?> t = TypeSetKubeJSRegistries.blockEntities().get(teType);
            modifyTileNBT(selection, teType, mapJsConsumerToNBT(selection, sup), reDrawBlocks);
        }

        public void updateTileNBT(Selection selection, UnaryOperator<MapJS> sup, boolean reDrawBlocks) {
            updateTileNBT(selection, TileEntity.class, sup, reDrawBlocks);
        }

        public void modifyTileNBT(Selection selection, Class<? extends TileEntity> teType, MapJS obj, boolean reDrawBlocks) {
            updateTileNBT(selection, teType, $ -> obj, reDrawBlocks);
        }

        public void modifyTileNBT(Selection selection, MapJS obj, boolean reDrawBlocks) {
            modifyTileNBT(selection, TileEntity.class, obj, reDrawBlocks);
        }

        @Override
        public SceneBuilder.WorldInstructions getInternal() {
            return internal;
        }

        public BlockIDPredicate getBlockStateJS(BlockState state) {
            BlockIDPredicate predicate = BlockWrapper.id(state.getBlock().getRegistryName());
            state.getValues().forEach((p, c) -> predicate.with(p.getName(), state.getValue(p).toString()));
            return predicate;
        }

        public void modifyBlock(BlockPos pos, boolean addParticles, Consumer<BlockIDPredicate> mod) {
            modifyBlock(pos, (state) -> {
                BlockIDPredicate predicate = getBlockStateJS(state);
                mod.accept(predicate);
                return predicate.getBlockState();
            }, addParticles);
        }

        public void modifyBlock(BlockPos pos, Consumer<BlockIDPredicate> mod) {
            modifyBlock(pos, false, mod);
        }

        public void modifyEntity(ElementLink<EntityElement> entity, Consumer<EntityJS> mod) {
            internal.modifyEntity(entity, (e) ->
                    mod.accept(new EntityJS(
                            UtilsJS.getWorld(e.level), e)));
        }

        public ElementLink<EntityElement> createEntity(ResourceLocation id, Vector3d pos, UnaryOperator<EntityJS> mod) {
            return internal.createEntity(w -> mod.apply(createEntityJS(w, id, pos)).minecraftEntity);
        }

        public ElementLink<EntityElement> createEntity(ResourceLocation id, Vector3d pos) {
            return createEntity(id, pos, e -> e);
        }

        public EntityJS createEntityJS(World world, ResourceLocation id, Vector3d pos) {
            Entity entity = getEntity(id).create(world);
            entity.setPos(pos.x, pos.y, pos.z);
            return new EntityJS(UtilsJS.getWorld(world), entity);
        }

        public EntityType<?> getEntity(ResourceLocation id) {
            return KubeJSRegistries.entityTypes().get(id);
        }
    }
}
