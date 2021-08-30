package com.kotakotik.ponderjs.kubejs;

import com.kotakotik.ponderjs.PonderJS;
import com.kotakotik.ponderjs.api.AbstractPonderBuilder;
import com.kotakotik.ponderjs.kubejs.util.SceneBuilderJS;
import com.kotakotik.ponderjs.kubejs.util.SceneBuildingUtilJS;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.repack.registrate.util.entry.ItemProviderEntry;
import dev.latvian.kubejs.util.ListJS;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class PonderBuilderJS extends
        AbstractPonderBuilder<ResourceLocation, PonderBuilderJS, PonderBuilderJS.SceneConsumer> {
    public PonderBuilderJS(String name, ListJS ids) {
        super(PonderJS.appendKubeToId(name), ids.stream()
                .map(Object::toString)
                .map(ResourceLocation::new)
                .collect(Collectors.toList()));
    }

//    public PonderBuilderJS<T> scene(List<ResourceLocation> items, List<List<Object>> storyBoards, BiConsumer<SceneBuilder, SceneBuildingUtil> scene) {
//        return step(i -> {
//           PonderRegistry.MultiSceneBuilder multiSceneBuilder = PonderRegistry.forComponents(items.stream().map((id) -> PonderRegistryEventJS.createItemProvider(RegistryObject.of(id, ForgeRegistries.ITEMS))).collect(Collectors.toList()));
//                        multiSceneBuilder.addStoryBoard("test", scene::accept);
//        });
//    }]

    public static HashMap<String, SceneConsumer> scenes = new HashMap<>();

    public PonderBuilderJS scene(String name, String displayName, String schematic, SceneConsumer scene) {
        String fullName = getName(name);
        SceneConsumer oldScene = scenes.get(fullName);
        if(oldScene != null) PonderJS.LOGGER.info("Overwriting scene " + fullName + " with new one");
        scenes.put(fullName, scene);
        for (ResourceLocation id : items)
            addNamedStoryBoard(getPathOnlyName(name), displayName, id, PonderJS.appendKubeToId(schematic), (b, u) -> programStoryBoard(scenes.get(fullName), b, u));
        return this;
    }

    @Override
    public PonderBuilderJS getSelf() {
        return this;
    }

    @Override
    protected ResourceLocation[] itemsToIdArray() {
        return items.toArray(new ResourceLocation[0]);
    }

    @Override
    protected ItemProviderEntry<?> getItemProviderEntry(ResourceLocation item) {
        return PonderJS.createItemProvider(RegistryObject.of(item, ForgeRegistries.ITEMS));
    }

    @Override
    protected void programStoryBoard(SceneConsumer scene, SceneBuilder builder, SceneBuildingUtil util) {
        scene.accept(new SceneBuilderJS(builder), new SceneBuildingUtilJS(util));
    }

    @Override
    protected String itemToString(ResourceLocation item) {
        return item.toString().replace(":", ".");
    }

    @Override
    protected PonderBuilderJS.SceneConsumer createConsumer(BiConsumer<SceneBuilder, SceneBuildingUtil> consumer) {
        return (b, u) -> consumer.accept(b.getInternal(), u.getInternal());
    }

    @Override // expose protected method
    public PonderBuilderJS tag(String... tags) {
        return super.tag(tags);
    }

    @FunctionalInterface
    public interface SceneConsumer extends BiConsumer<SceneBuilderJS, SceneBuildingUtilJS> {
    }
}
