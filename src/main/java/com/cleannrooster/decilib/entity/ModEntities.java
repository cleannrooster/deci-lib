package com.cleannrooster.decilib.entity;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.builder.MobBuilder;
import com.cleannrooster.decilib.builder.MobDefinition;
import com.cleannrooster.decilib.builder.MobProfileException;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.loader.MobProfileLoader;
import com.cleannrooster.decilib.builder.visual.Form;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.cleannrooster.decilib.builder.visual.Form.*;

public final class ModEntities {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModEntities.class);

    private static final String MOBS_PATH = "data/deci-lib/mobs";



    // Populated during register() — keyed by mob id (matches the "id" field in each profile).
    private static final Map<String, EntityType<DataDrivenMob>> DATA_DRIVEN_MOBS        = new LinkedHashMap<>();
    private static final Map<String, MobDefinition>             DATA_DRIVEN_DEFINITIONS = new LinkedHashMap<>();

    /** Unmodifiable view of all auto-discovered entity types, keyed by mob id. */
    public static Map<String, EntityType<DataDrivenMob>> getDataDrivenMobs() {
        return Collections.unmodifiableMap(DATA_DRIVEN_MOBS);
    }

    /** Returns the {@link MobDefinition} for a registered data-driven mob id, or {@code null}. */
    public static MobDefinition getDefinition(String id) {
        return DATA_DRIVEN_DEFINITIONS.get(id);
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    public static void register() {
        discoverAndRegisterDataMobs();
    }

    private static void discoverAndRegisterDataMobs() {
        for (var container : FabricLoader.getInstance().getAllMods()) {
            for (Path root : container.getRootPaths()) {
                Path mobsDir = root.resolve(MOBS_PATH);
                if (!Files.exists(mobsDir)) continue;
                try (Stream<Path> paths = Files.walk(mobsDir)) {
                    paths.filter(p -> p.getFileName().toString().endsWith(".json"))
                         .forEach(ModEntities::registerFromPath);
                } catch (IOException e) {
                    LOGGER.error("[deci-lib] Failed to scan mob profiles in {}: {}", mobsDir, e.getMessage(), e);
                }
            }
        }
    }

    private static void registerFromPath(Path path) {
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

        if (def.form() == Form.AZURELIB
                && !FabricLoader.getInstance().isModLoaded("azurelib")) {
            LOGGER.warn("[deci-lib] Skipping mob '{}': form 'azurelib' requires the AzureLib mod to be installed.",
                    def.id());
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
                        .build()
        );

        FabricDefaultAttributeRegistry.register(
                type, DataDrivenMob.createDataDrivenAttributes()
                        .add(EntityAttributes.GENERIC_SCALE, scale.scale()));
        Item egg = new SpawnEggItem(type, Colors.GRAY,Colors.WHITE,new Item.Settings());
        Registry.register(Registries.ITEM, Identifier.of(Decilib.MOD_ID, def.id()+"_spawn_egg"), egg);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(fabricItemGroupEntries -> {
            fabricItemGroupEntries.add(new ItemStack(egg));
        });
        DATA_DRIVEN_MOBS.put(def.id(), type);
        DATA_DRIVEN_DEFINITIONS.put(def.id(), def);
        LOGGER.info("[deci-lib] Registered data-driven mob '{}'", def.id());
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
        // AZURELIB: geo defines visual shape; hitbox defaults to standard biped size
        t.put(AZURELIB,           DEFAULT_SCALE);
        return Collections.unmodifiableMap(t);
    }
}
