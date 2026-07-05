package com.cleannrooster.decilib.entity;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.builder.MobBuilder;
import com.cleannrooster.decilib.builder.MobDefinition;
import com.cleannrooster.decilib.builder.MobProfileException;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.loader.MobProfileLoader;
import com.cleannrooster.decilib.builder.visual.Form;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.cleannrooster.decilib.builder.visual.Form.*;

public final class ModEntities {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModEntities.class);

    private static final String MOBS_PATH         = "data/" + Decilib.MOD_ID + "/mobs";
    private static final String MOBS_EXAMPLE_PATH = "data/" + Decilib.MOD_ID + "/mobs_example";

    // Populated during registerEntityTypes() — keyed by mob id.
    private static final Map<String, EntityType<DataDrivenMob>> DATA_DRIVEN_MOBS        = new LinkedHashMap<>();
    private static final Map<String, MobDefinition>             DATA_DRIVEN_DEFINITIONS = new LinkedHashMap<>();

    // Tracks which mob IDs were loaded from the mobs_example folder.
    private static final Set<String> EXAMPLE_MOB_IDS = new LinkedHashSet<>();

    // Attribute builders — used by platform entrypoints to register attributes.
    // Fabric: FabricDefaultAttributeRegistry.register(type, builder)
    // NeoForge: EntityAttributeCreationEvent#put(type, builder.build())
    private static final Map<EntityType<DataDrivenMob>, DefaultAttributeContainer.Builder> ATTRIBUTE_BUILDERS = new LinkedHashMap<>();

    // Spawn eggs registered during registerSpawnEggs().
    // NeoForge: iterate in BuildCreativeModeTabContentsEvent.
    // Fabric: ItemGroupEvents in DecilibFabric.
    private static final List<Item> SPAWN_EGGS = new ArrayList<>();

    // -------------------------------------------------------------------------

    /** Unmodifiable view of all registered entity types, keyed by mob id. */
    public static Map<String, EntityType<DataDrivenMob>> getDataDrivenMobs() {
        return Collections.unmodifiableMap(DATA_DRIVEN_MOBS);
    }

    /** Returns the {@link MobDefinition} for a registered data-driven mob id, or {@code null}. */
    public static MobDefinition getDefinition(String id) {
        return DATA_DRIVEN_DEFINITIONS.get(id);
    }

    /**
     * Returns {@code true} if this mob ID was loaded from the {@code mobs_example} folder
     * rather than the standard {@code mobs} folder. Used by the spawn system to conditionally
     * suppress natural spawning based on the {@code exampleMobsNaturalSpawning} config flag.
     */
    public static boolean isExampleMob(String id) {
        return EXAMPLE_MOB_IDS.contains(id);
    }

    /**
     * Attribute builders for every registered entity type.
     * Fabric: FabricDefaultAttributeRegistry.register() in DecilibFabric.
     * NeoForge: EntityAttributeCreationEvent#put() in DecilibNeoForge.
     */
    public static Map<EntityType<DataDrivenMob>, DefaultAttributeContainer.Builder> getAttributeBuilders() {
        return Collections.unmodifiableMap(ATTRIBUTE_BUILDERS);
    }

    /**
     * Spawn eggs registered by registerSpawnEggs().
     * NeoForge: BuildCreativeModeTabContentsEvent in DecilibNeoForge.
     * Fabric: ItemGroupEvents in DecilibFabric.
     */
    public static List<Item> getSpawnEggs() {
        return Collections.unmodifiableList(SPAWN_EGGS);
    }

    // -------------------------------------------------------------------------

    /**
     * Phase 1 — Scan mod JAR roots and register entity types into the vanilla registry.
     * Must be called during entity type registration phase.
     * Fabric: before registry freeze in onInitialize.
     * NeoForge: inside RegisterEvent handler for RegistryKeys.ENTITY_TYPE.
     *
     * @param modJarRoots        root paths to scan (FabricLoader.getAllMods() or NeoForge ModList)
     * @param isModLoaded        platform predicate for mod-loaded checks (azurelib guard)
     * @param includeExampleMobs if true, also scan {@code data/decilib/mobs_example/} in each
     *                           root and track those mob IDs in {@link #EXAMPLE_MOB_IDS}
     */
    @SuppressWarnings("deprecation")
    public static void registerEntityTypes(Iterable<Path> modJarRoots,
                                           Predicate<String> isModLoaded,
                                           boolean includeExampleMobs) {
        for (Path root : modJarRoots) {
            // Standard mobs folder — always scanned.
            scanDirectory(root.resolve(MOBS_PATH), isModLoaded, false);

            // Example mobs folder — only scanned when the config flag is on.
            if (includeExampleMobs) {
                scanDirectory(root.resolve(MOBS_EXAMPLE_PATH), isModLoaded, true);
            }
        }
    }

    /** Walks {@code dir} (if it exists) and registers every {@code .json} file found. */
    private static void scanDirectory(Path dir, Predicate<String> isModLoaded, boolean markAsExample) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.getFileName().toString().endsWith(".json"))
                 .forEach(p -> registerFromPath(p, isModLoaded, markAsExample));
        } catch (IOException e) {
            LOGGER.error("[deci-lib] Failed to scan mob profiles in {}: {}", dir, e.getMessage(), e);
        }
    }

    /**
     * Phase 2 — Register spawn eggs into the ITEM registry.
     * Call after registerEntityTypes() during item registration.
     * Fabric: immediately after registerEntityTypes in DecilibFabric.
     * NeoForge: inside RegisterEvent handler for RegistryKeys.ITEM.
     */
    public static void registerSpawnEggs() {
        for (var entry : DATA_DRIVEN_MOBS.entrySet()) {
            String id   = entry.getKey();
            var    type = entry.getValue();
            Item egg = new SpawnEggItem(type, Colors.WHITE, Colors.WHITE, new Item.Settings());
            Registry.register(Registries.ITEM, Identifier.of(Decilib.MOD_ID, id + "_spawn_egg"), egg);
            SPAWN_EGGS.add(egg);
        }
    }

    // -------------------------------------------------------------------------

    private static void registerFromPath(Path path, Predicate<String> isModLoaded, boolean markAsExample) {
        com.cleannrooster.decilib.builder.MobProfile profile;
        try {
            profile = MobProfileLoader.load(path);
        } catch (MobProfileException e) {
            LOGGER.error("[deci-lib] Skipping malformed mob profile {}: {}", path.getFileName(), e.getMessage());
            return;
        }

        MobDefinition def;
        try {
            def = MobBuilder.build(profile);
        } catch (MobProfileException e) {
            LOGGER.error("[deci-lib] Skipping mob profile '{}': {}", profile.id(), e.getMessage());
            return;
        }

        if (def.form() == Form.AZURELIB && !isModLoaded.test("azurelib")) {
            LOGGER.warn("[deci-lib] Skipping mob '{}': form 'azurelib' requires the AzureLib mod.", def.id());
            return;
        }

        var scale = SCALE_TABLE.getOrDefault(def.form(), DEFAULT_SCALE);

        EntityType<DataDrivenMob> type = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(Decilib.MOD_ID, def.id()),
                EntityType.Builder.<DataDrivenMob>create(
                        (t, world) -> new DataDrivenMob(t, world, def),
                        SpawnGroup.MONSTER)
                        .dimensions(def.scaleProfile().width(), def.scaleProfile().height())
                        .build(Decilib.MOD_ID + ":" + def.id())
        );

        // Store the attribute builder for platform-specific attribute registration.
        ATTRIBUTE_BUILDERS.put(type,
                DataDrivenMob.createDataDrivenAttributes()
                        .add(EntityAttributes.GENERIC_SCALE, scale.scale()));

        DATA_DRIVEN_MOBS.put(def.id(), type);
        DATA_DRIVEN_DEFINITIONS.put(def.id(), def);
        if (markAsExample) EXAMPLE_MOB_IDS.add(def.id());
        LOGGER.info("[deci-lib] Registered data-driven mob '{}'{}", def.id(),
                markAsExample ? " (example)" : "");
    }

    // -------------------------------------------------------------------------

    private static final MobScale.Preset DEFAULT_SCALE = new MobScale.Preset(1f, 0.6f, 1.8f);

    private static final Map<Form, MobScale.Preset> SCALE_TABLE = buildScaleTable();

    private static Map<Form, MobScale.Preset> buildScaleTable() {
        var t = new EnumMap<Form, MobScale.Preset>(Form.class);
        t.put(AGILE_BIPED,        MobScale.AGILE_BIPED);
        t.put(ARMORED_BIPED,      MobScale.ARMORED_BIPED);
        t.put(ARCANE_BIPED,       MobScale.ARCANE_BIPED);
        t.put(STANDARD_BIPED,     MobScale.STANDARD_BIPED);
        t.put(HEAVY_BIPED,        MobScale.HEAVY_BIPED);
        t.put(ALIEN_BIPED,        MobScale.ALIEN_BIPED);
        t.put(HEAVY_QUADRUPED,    MobScale.HEAVY_QUADRUPED);
        t.put(STANDARD_QUADRUPED, MobScale.STANDARD_QUADRUPED);
        t.put(SPECTRAL_QUADRUPED, MobScale.SPECTRAL_QUADRUPED);
        t.put(STALKER_QUADRUPED,  MobScale.STALKER_QUADRUPED);
        t.put(SWIFT_QUADRUPED,    MobScale.SWIFT_QUADRUPED);
        t.put(SMALL_QUADRUPED,    MobScale.SMALL_QUADRUPED);
        t.put(AZURELIB,           DEFAULT_SCALE);
        return Collections.unmodifiableMap(t);
    }
}
