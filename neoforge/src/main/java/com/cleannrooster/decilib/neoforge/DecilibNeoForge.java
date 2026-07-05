package com.cleannrooster.decilib.neoforge;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.DeciLibConfig;
import com.cleannrooster.decilib.builder.animation.AzurelibAnimationDispatcher;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.builder.loader.GlobalDatapackScanner;
import com.cleannrooster.decilib.entity.ModEntities;
import me.shedaniel.autoconfig.AutoConfig;
import com.cleannrooster.decilib.spawn.MobSpawnRegistrar;
import com.cleannrooster.decilib.spawn.MobSpawnReloadListener;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.world.Heightmap;
import net.minecraft.item.ItemGroups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod(Decilib.MOD_ID)
public class DecilibNeoForge {

    public DecilibNeoForge(IEventBus modBus) {
        Decilib.init();

        modBus.addListener(RegisterEvent.class, this::onRegister);
        modBus.addListener(EntityAttributeCreationEvent.class, this::onAttributeCreation);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, this::onCreativeTab);
        modBus.addListener(RegisterSpawnPlacementsEvent.class, this::onRegisterSpawnPlacements);
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, this::onAddReloadListener);

        if (ModList.get().isLoaded("azurelib")) {
            MobAnimationDispatcherRegistry.register(new AzurelibAnimationDispatcher());
        }

        Decilib.LOGGER.info("deci-lib initialized (NeoForge).");
    }

    private void onRegister(RegisterEvent event) {
        // ENTITY_TYPE — scan mod JARs and register all data-driven mobs.
        if (event.getRegistryKey().equals(RegistryKeys.ENTITY_TYPE)) {
            boolean includeExamples = AutoConfig.getConfigHolder(DeciLibConfig.class)
                    .getConfig().registerExampleMobs;
            List<Path> roots = new ArrayList<>(collectModRoots());
            // Also scan global datapack loader directories (Paxi, OpenLoader, Global Packs, etc.)
            // before the registry freezes — these are plain filesystem paths available at startup.
            try (GlobalDatapackScanner scanner = new GlobalDatapackScanner(FMLPaths.GAMEDIR.get())) {
                roots.addAll(scanner.collectPackRoots());
                ModEntities.registerEntityTypes(roots, id -> ModList.get().isLoaded(id), includeExamples);
            }
            // SpawnRestrictions are registered via RegisterSpawnPlacementsEvent (NeoForge mod bus).
        }

        // ITEM — register spawn eggs (entity types must exist first).
        if (event.getRegistryKey().equals(RegistryKeys.ITEM)) {
            ModEntities.registerSpawnEggs();
        }

        // BIOME_MODIFIER_SERIALIZERS — register the dynamic spawn modifier codec.
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS)) {
            event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    helper -> helper.register(
                            net.minecraft.util.Identifier.of(Decilib.MOD_ID, "dynamic_mob_spawn"),
                            DynamicMobSpawnModifier.CODEC));
        }
    }

    private void onAttributeCreation(EntityAttributeCreationEvent event) {
        ModEntities.getAttributeBuilders().forEach((type, builder) ->
                event.put(type, builder.build()));
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ItemGroups.SPAWN_EGGS) {
            ModEntities.getSpawnEggs().forEach(egg ->
                    event.add(new ItemStack(egg), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS));
        }
    }

    private void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        ModEntities.getDataDrivenMobs().forEach((mobId, type) ->
                event.register(type,
                        SpawnLocationTypes.ON_GROUND,
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        MobSpawnRegistrar.buildSpawnPredicate(mobId),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MobSpawnReloadListener());
    }

    private static List<Path> collectModRoots() {
        List<Path> roots = new ArrayList<>();
        for (var modInfo : ModList.get().getMods()) {
            try {
                Path root = modInfo.getOwningFile().getFile().findResource(".");
                roots.add(root);
            } catch (Exception e) {
                // Some synthetic/virtual mods have no real file — silently skip.
            }
        }
        return roots;
    }
}
