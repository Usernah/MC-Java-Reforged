package net.jr.screens.crafting;

import net.jr.Java_reforged;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.neoforged.neoforge.common.Tags;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CraftingRecipeClassifier {
    private static final String TAG_ROOT = "crafting_categories/";
    private static final Map<CraftingCategory, TagKey<Item>> CATEGORY_TAGS = createCategoryTags();
    private static final List<TagKey<Item>> MATERIAL_TAGS = List.of(
            Tags.Items.BONES,
            Tags.Items.BRICKS,
            Tags.Items.CLUMPS,
            Tags.Items.CROPS,
            Tags.Items.DUSTS,
            Tags.Items.DYES,
            Tags.Items.ENDER_PEARLS,
            Tags.Items.FEATHERS,
            Tags.Items.FERTILIZERS,
            Tags.Items.GEMS,
            Tags.Items.GUNPOWDERS,
            Tags.Items.INGOTS,
            Tags.Items.LEATHERS,
            Tags.Items.NUGGETS,
            Tags.Items.RAW_MATERIALS,
            Tags.Items.RODS,
            Tags.Items.ROPES,
            Tags.Items.SEEDS,
            Tags.Items.SLIME_BALLS,
            Tags.Items.STRINGS,
            ItemTags.COALS,
            ItemTags.METAL_NUGGETS
    );
    private static final List<TagKey<Item>> DECORATION_TAGS = List.of(
            ItemTags.BANNERS,
            ItemTags.BEDS,
            ItemTags.CANDLES,
            ItemTags.HANGING_SIGNS,
            ItemTags.SIGNS,
            ItemTags.SKULLS,
            ItemTags.WOOL_CARPETS,
            Tags.Items.BARRELS,
            Tags.Items.BOOKSHELVES,
            Tags.Items.CHAINS,
            Tags.Items.CHESTS,
            Tags.Items.FROGLIGHTS,
            Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES,
            Tags.Items.PLAYER_WORKSTATIONS_FURNACES,
            Tags.Items.SHULKER_BOXES,
            Tags.Items.VILLAGER_JOB_SITES
    );

    private CraftingRecipeClassifier() {
    }

    public static CraftingCategory classify(RecipeDisplayEntry recipe, ItemStack result) {
        if (result.isEmpty()) {
            return CraftingCategory.RESOURCES_AND_FOOD;
        }

        String path = itemPath(result);

        if (isRedstoneTorch(path)) {
            return CraftingCategory.REDSTONE_AND_MECHANISMS;
        }
        if (isBuildingLight(path)) {
            return CraftingCategory.BUILDING_BLOCKS;
        }

        if (isDecoration(result)) {
            return CraftingCategory.DECORATION;
        }

        CraftingCategory explicitCategory = explicitCategory(result);
        if (explicitCategory != null) {
            return explicitCategory;
        }

        if (recipe.category() == RecipeBookCategories.CRAFTING_BUILDING_BLOCKS) {
            return CraftingCategory.BUILDING_BLOCKS;
        }
        if (recipe.category() == RecipeBookCategories.CRAFTING_EQUIPMENT) {
            return CraftingCategory.EQUIPMENT_AND_TOOLS;
        }
        if (recipe.category() == RecipeBookCategories.CRAFTING_REDSTONE) {
            return CraftingCategory.REDSTONE_AND_MECHANISMS;
        }

        if (isTransport(result)) {
            return CraftingCategory.REDSTONE_AND_MECHANISMS;
        }
        if (isCombatOrRanged(path) || isUtility(path)) {
            return CraftingCategory.EQUIPMENT_AND_TOOLS;
        }
        if (isFirework(result)) {
            return CraftingCategory.RESOURCES_AND_FOOD;
        }
        if (isFoodOrHealth(result)) {
            return CraftingCategory.RESOURCES_AND_FOOD;
        }
        if (isEquipment(result)) {
            return CraftingCategory.EQUIPMENT_AND_TOOLS;
        }
        if (isMaterial(result)) {
            return CraftingCategory.RESOURCES_AND_FOOD;
        }

        if (result.getItem() instanceof BlockItem) {
            return CraftingCategory.BUILDING_BLOCKS;
        }
        return CraftingCategory.RESOURCES_AND_FOOD;
    }

    public static EquipmentOrder equipmentOrder(ItemStack result) {
        String path = itemPath(result);
        int section;
        int tier;
        int type;

        if (isTool(result, path)) {
            section = 0;
            tier = materialTier(path);
            type = toolType(path);
        } else if (isArmor(result, path)) {
            section = 1;
            tier = materialTier(path);
            type = armorType(path);
        } else if (isCombatOrRanged(path)) {
            section = 2;
            tier = materialTier(path);
            type = combatType(path);
        } else if (isUtility(path)) {
            section = 3;
            tier = 0;
            type = utilityType(path);
        } else {
            section = 4;
            tier = 0;
            type = 0;
        }

        return new EquipmentOrder(section, tier, type, path);
    }

    public static boolean isWall(ItemStack result) {
        String path = itemPath(result);
        return result.typeHolder().is(ItemTags.WALLS)
                || path.endsWith("_wall");
    }

    public static BuildingOrder buildingOrder(ItemStack result) {
        String path = itemPath(result);
        return new BuildingOrder(
                buildingFamily(path),
                buildingMaterial(path),
                buildingSubtype(path),
                path
        );
    }

    public static DecorationOrder decorationOrder(ItemStack result) {
        String path = itemPath(result);
        return new DecorationOrder(
                decorationFamily(path),
                decorationSubtype(path),
                buildingMaterial(path),
                path
        );
    }

    public static RedstoneOrder redstoneOrder(ItemStack result) {
        String path = itemPath(result);
        return new RedstoneOrder(
                redstoneFamily(path),
                redstoneSubtype(path),
                buildingMaterial(path),
                path
        );
    }

    public static ResourcesFoodOrder resourcesFoodOrder(ItemStack result) {
        String path = itemPath(result);
        if (isFoodIngredient(result, path)) {
            return new ResourcesFoodOrder(1, foodIngredientFamily(path), 0, 0, path);
        }
        if (isCookedFood(result, path)) {
            return new ResourcesFoodOrder(3, foodFamily(path), 0, 0, path);
        }
        if (isMealOrHealth(path)) {
            return new ResourcesFoodOrder(4, mealFamily(path), 0, 0, path);
        }
        if (isFoodOrHealth(result)) {
            return new ResourcesFoodOrder(2, foodFamily(path), 0, 0, path);
        }

        return new ResourcesFoodOrder(
                0,
                materialFamily(result, path),
                materialTier(path),
                materialForm(path),
                path
        );
    }

    private static int buildingFamily(String path) {
        if (path.endsWith("_slab")) return 1;
        if (path.endsWith("_stairs")) return 2;
        if (path.endsWith("_wall")) return 3;
        if (path.endsWith("_fence") || path.endsWith("_fence_gate")) return 4;
        if (path.endsWith("_door") || path.endsWith("_trapdoor")) return 5;
        return 0;
    }

    private static int redstoneFamily(String path) {
        if (path.equals("redstone") || path.endsWith("redstone_torch")
                || path.equals("repeater") || path.equals("comparator")
                || path.equals("redstone_block") || path.equals("redstone_lamp")) return 0;
        if (path.equals("lever") || path.endsWith("_button")
                || path.endsWith("_pressure_plate") || path.equals("tripwire_hook")
                || path.equals("daylight_detector")) return 1;
        if (path.equals("piston") || path.equals("sticky_piston")
                || path.equals("observer") || path.equals("dispenser")
                || path.equals("dropper") || path.equals("hopper")
                || path.equals("crafter") || path.equals("tnt")
                || path.endsWith("_door") || path.endsWith("_trapdoor")
                || path.endsWith("_fence_gate") || path.endsWith("_bulb")
                || path.equals("trapped_chest") || path.equals("honey_block")
                || path.equals("slime_block")) return 2;
        if (path.equals("target") || path.equals("note_block")
                || path.contains("sculk_sensor") || path.equals("lectern")) return 3;
        if (path.endsWith("_rail") || path.equals("rail")) return 4;
        if (path.endsWith("_minecart") || path.equals("minecart")) return 5;
        if (path.endsWith("_boat") || path.endsWith("_raft")
                || path.endsWith("_chest_boat") || path.endsWith("_chest_raft")) return 6;
        return 7;
    }

    private static int redstoneSubtype(String path) {
        if (path.equals("redstone")) return 0;
        if (path.endsWith("redstone_torch")) return 1;
        if (path.equals("repeater")) return 2;
        if (path.equals("comparator")) return 3;
        if (path.equals("redstone_block")) return 4;
        if (path.equals("redstone_lamp")) return 5;
        if (path.equals("lever")) return 0;
        if (path.endsWith("_button")) return 1;
        if (path.endsWith("_pressure_plate")) return 2;
        if (path.equals("tripwire_hook")) return 3;
        if (path.equals("daylight_detector")) return 4;
        if (path.equals("piston")) return 0;
        if (path.equals("sticky_piston")) return 1;
        if (path.equals("observer")) return 2;
        if (path.equals("dispenser")) return 3;
        if (path.equals("dropper")) return 4;
        if (path.equals("hopper")) return 5;
        if (path.equals("crafter")) return 6;
        if (path.equals("tnt")) return 7;
        if (path.endsWith("_door")) return 8;
        if (path.endsWith("_fence_gate")) return 9;
        if (path.endsWith("_trapdoor")) return 10;
        if (path.endsWith("_bulb")) return 11;
        if (path.equals("trapped_chest")) return 12;
        if (path.equals("honey_block") || path.equals("slime_block")) return 13;
        if (path.equals("target")) return 0;
        if (path.equals("note_block")) return 1;
        if (path.contains("sculk_sensor")) return 2;
        if (path.equals("lectern")) return 3;
        if (path.equals("rail")) return 0;
        if (path.equals("powered_rail")) return 1;
        if (path.equals("detector_rail")) return 2;
        if (path.equals("activator_rail")) return 3;
        if (path.equals("minecart")) return 0;
        if (path.equals("chest_minecart")) return 1;
        if (path.equals("hopper_minecart")) return 2;
        if (path.equals("furnace_minecart")) return 3;
        if (path.equals("tnt_minecart")) return 4;
        if (path.equals("command_block_minecart")) return 5;
        return 50;
    }

    private static int buildingMaterial(String path) {
        if (path.contains("pale_oak")) return 9;
        if (path.contains("oak")) return 0;
        if (path.contains("spruce")) return 1;
        if (path.contains("birch")) return 2;
        if (path.contains("jungle")) return 3;
        if (path.contains("acacia")) return 4;
        if (path.contains("dark_oak")) return 5;
        if (path.contains("mangrove")) return 6;
        if (path.contains("cherry")) return 7;
        if (path.contains("bamboo")) return 8;
        if (path.contains("crimson")) return 9;
        if (path.contains("warped")) return 10;
        if (path.contains("stone") || path.contains("cobblestone")) return 20;
        if (path.contains("granite")) return 21;
        if (path.contains("diorite")) return 22;
        if (path.contains("andesite")) return 23;
        if (path.contains("deepslate")) return 24;
        if (path.contains("tuff")) return 25;
        if (path.contains("sandstone")) return 30;
        if (path.contains("brick")) return 40;
        if (path.contains("prismarine")) return 50;
        if (path.contains("quartz")) return 60;
        if (path.contains("nether")) return 70;
        if (path.contains("end")) return 80;
        if (path.contains("copper")) return 90;
        if (path.contains("iron")) return 91;
        if (path.contains("gold")) return 92;
        if (path.contains("diamond")) return 93;
        return 100;
    }

    private static int buildingSubtype(String path) {
        if (path.endsWith("_planks")) return 0;
        if (path.startsWith("stripped_") && (path.endsWith("_log")
                || path.endsWith("_wood") || path.endsWith("_stem")
                || path.endsWith("_hyphae"))) return 2;
        if (path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_stem") || path.endsWith("_hyphae")) return 1;
        if (path.endsWith("_leaves")) return 3;
        if (path.endsWith("_sapling")) return 4;
        if (path.endsWith("_ore") || path.contains("_ore_")) return 5;
        if (path.endsWith("_block")) return 6;
        if (path.contains("brick") || path.contains("tiles")) return 7;
        return 50;
    }

    private static int decorationFamily(String path) {
        if (isLight(path)) return 0;
        if (isFunctionalDecoration(path)) return 1;
        if (isWallDecoration(path)) return 2;
        if (isFurniture(path)) return 3;
        return 4;
    }

    private static int decorationSubtype(String path) {
        if (isLight(path)) {
            if (path.contains("torch")) return 0;
            if (path.contains("lantern")) return 1;
            if (path.contains("candle")) return 2;
            if (path.equals("end_rod")) return 3;
            if (path.contains("campfire")) return 4;
            if (path.contains("froglight")) return 5;
            return 6;
        }
        if (isFunctionalDecoration(path)) {
            if (path.contains("chest") || path.contains("barrel")
                    || path.contains("shulker_box")) return 0;
            if (path.contains("shelf") || path.contains("bookshelf")) return 1;
            if (path.contains("crafting_table") || path.contains("furnace")
                    || path.contains("smithing_table") || path.contains("stonecutter")
                    || path.contains("grindstone") || path.contains("loom")) return 2;
            if (path.contains("beacon") || path.contains("conduit")
                    || path.contains("lodestone") || path.contains("respawn_anchor")) return 3;
            return 4;
        }
        if (isWallDecoration(path)) {
            if (path.contains("sign")) return 0;
            if (path.contains("banner")) return 1;
            if (path.contains("painting")) return 2;
            if (path.contains("item_frame")) return 3;
            if (path.contains("skull") || path.contains("head")) return 4;
            return 5;
        }
        if (isFurniture(path)) {
            if (path.contains("bed")) return 0;
            if (path.contains("carpet")) return 1;
            if (path.contains("pot") || path.contains("flower")) return 2;
            if (path.contains("ladder")) return 3;
            if (path.contains("armor_stand")) return 4;
            return 5;
        }
        return 50;
    }

    private static boolean isLight(String path) {
        return path.contains("torch")
                || path.contains("lantern")
                || path.equals("end_rod")
                || path.contains("campfire")
                || path.contains("candle")
                || path.contains("froglight");
    }

    private static boolean isRedstoneTorch(String path) {
        return path.endsWith("redstone_torch");
    }

    private static boolean isBuildingLight(String path) {
        return path.equals("jack_o_lantern")
                || path.equals("sea_lantern");
    }

    private static boolean isFunctionalDecoration(String path) {
        return path.contains("beacon")
                || path.contains("enchanting_table")
                || path.contains("lightning_rod")
                || path.contains("conduit")
                || path.contains("scaffolding")
                || path.equals("lodestone")
                || path.equals("respawn_anchor")
                || path.equals("dried_ghast")
                || path.endsWith("_shelf")
                || path.contains("anvil")
                || path.contains("jukebox")
                || path.contains("bookshelf")
                || path.contains("lectern")
                || path.contains("grindstone")
                || path.contains("smithing_table")
                || path.contains("stonecutter")
                || path.contains("brewing_stand")
                || path.contains("cauldron")
                || path.contains("barrel")
                || path.contains("chest")
                || path.contains("shulker_box")
                || path.contains("golem");
    }

    private static boolean isWallDecoration(String path) {
        return path.contains("painting")
                || path.contains("sign")
                || path.contains("banner")
                || path.contains("skull")
                || path.contains("head")
                || path.contains("item_frame");
    }

    private static boolean isFurniture(String path) {
        return path.contains("pot")
                || path.contains("flower")
                || path.contains("armor_stand")
                || path.contains("bed")
                || path.contains("carpet")
                || path.contains("ladder");
    }

    private static boolean isFoodIngredient(ItemStack result, String path) {
        return result.typeHolder().is(Tags.Items.CROPS)
                || result.typeHolder().is(Tags.Items.SEEDS)
                || path.endsWith("_seeds")
                || path.equals("wheat")
                || path.equals("sugar")
                || path.contains("cocoa")
                || path.equals("egg")
                || path.equals("milk_bucket")
                || path.equals("honeycomb");
    }

    private static boolean isCookedFood(ItemStack result, String path) {
        return isFoodOrHealth(result)
                && (path.startsWith("cooked_")
                || path.endsWith("_cooked")
                || path.equals("steak"));
    }

    private static boolean isMealOrHealth(String path) {
        return path.contains("stew")
                || path.contains("soup")
                || path.contains("cake")
                || path.contains("pie")
                || path.contains("golden_apple")
                || path.contains("golden_carrot")
                || path.contains("suspicious")
                || path.contains("potion")
                || path.equals("honey_bottle")
                || path.equals("milk_bucket");
    }

    private static int materialFamily(ItemStack result, String path) {
        if (isBannerPattern(result, path)) return 4;
        if (isArmorTrim(result, path)) return 5;
        if (isMineralMaterial(result, path)) return 3;
        if (result.typeHolder().is(Tags.Items.DYES) || path.endsWith("_dye")) return 2;
        if (isSpecialMaterial(result, path)) return 6;
        if (isOrganicMaterial(result, path)) return 1;
        return 0;
    }

    private static boolean isBannerPattern(ItemStack result, String path) {
        return result.has(DataComponents.PROVIDES_BANNER_PATTERNS)
                || path.endsWith("_banner_pattern");
    }

    private static boolean isArmorTrim(ItemStack result, String path) {
        return result.getItem() instanceof SmithingTemplateItem
                || path.endsWith("_armor_trim_smithing_template");
    }

    private static boolean isMineralMaterial(ItemStack result, String path) {
        return result.typeHolder().is(Tags.Items.CLUMPS)
                || result.typeHolder().is(Tags.Items.DUSTS)
                || result.typeHolder().is(Tags.Items.GEMS)
                || result.typeHolder().is(Tags.Items.INGOTS)
                || result.typeHolder().is(Tags.Items.NUGGETS)
                || result.typeHolder().is(Tags.Items.RAW_MATERIALS)
                || result.typeHolder().is(ItemTags.COALS)
                || result.typeHolder().is(ItemTags.METAL_NUGGETS)
                || path.startsWith("raw_")
                || path.contains("_ingot")
                || path.contains("_nugget")
                || path.contains("_dust")
                || path.contains("_clump")
                || path.contains("_gem")
                || path.contains("coal")
                || path.contains("copper")
                || path.contains("iron")
                || path.contains("gold")
                || path.contains("diamond")
                || path.contains("emerald")
                || path.contains("amethyst")
                || path.contains("quartz");
    }

    private static boolean isSpecialMaterial(ItemStack result, String path) {
        return result.getItem() instanceof SmithingTemplateItem
                || result.typeHolder().is(Tags.Items.ENDER_PEARLS)
                || result.typeHolder().is(Tags.Items.GUNPOWDERS)
                || result.typeHolder().is(Tags.Items.RODS)
                || result.typeHolder().is(Tags.Items.FERTILIZERS)
                || path.contains("ender")
                || path.contains("blaze")
                || path.contains("firework")
                || path.contains("template")
                || path.contains("nether_star")
                || path.contains("echo_shard");
    }

    private static boolean isOrganicMaterial(ItemStack result, String path) {
        return result.typeHolder().is(Tags.Items.BONES)
                || result.typeHolder().is(Tags.Items.FEATHERS)
                || result.typeHolder().is(Tags.Items.LEATHERS)
                || result.typeHolder().is(Tags.Items.ROPES)
                || result.typeHolder().is(Tags.Items.SLIME_BALLS)
                || result.typeHolder().is(Tags.Items.STRINGS)
                || path.contains("bone")
                || path.contains("feather")
                || path.contains("leather")
                || path.contains("string")
                || path.contains("slime");
    }

    private static int foodIngredientFamily(String path) {
        if (path.endsWith("_seeds") || path.contains("wheat") || path.contains("crop")) return 0;
        if (path.contains("sugar") || path.contains("cocoa") || path.equals("egg")) return 1;
        if (path.contains("milk") || path.contains("honey")) return 2;
        return 3;
    }

    private static int foodFamily(String path) {
        if (path.contains("beef") || path.contains("pork") || path.contains("chicken")
                || path.contains("mutton") || path.contains("rabbit") || path.contains("cod")
                || path.contains("salmon")) return 1;
        if (path.contains("bread") || path.contains("cookie")) return 2;
        return 0;
    }

    private static int mealFamily(String path) {
        if (path.contains("stew") || path.contains("soup")) return 0;
        if (path.contains("cake") || path.contains("pie")) return 1;
        if (path.contains("golden") || path.contains("suspicious")) return 2;
        return 3;
    }

    private static int materialForm(String path) {
        if (path.startsWith("raw_")) return 0;
        if (path.contains("clump")) return 1;
        if (path.contains("dust")) return 2;
        if (path.contains("nugget")) return 3;
        if (path.contains("ingot")) return 4;
        if (path.contains("gem")) return 5;
        return 6;
    }

    private static boolean isTool(ItemStack result, String path) {
        return result.has(DataComponents.TOOL)
                || result.typeHolder().is(Tags.Items.TOOLS)
                || path.endsWith("_pickaxe")
                || path.endsWith("_axe")
                || path.endsWith("_shovel")
                || path.endsWith("_hoe")
                || path.equals("shears")
                || path.equals("flint_and_steel")
                || path.equals("brush");
    }

    private static boolean isArmor(ItemStack result, String path) {
        return result.typeHolder().is(Tags.Items.ARMORS)
                || path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots")
                || path.endsWith("_horse_armor")
                || path.endsWith("_wolf_armor")
                || path.equals("wolf_armor");
    }

    private static boolean isCombatOrRanged(String path) {
        return path.endsWith("_sword")
                || path.equals("bow")
                || path.equals("crossbow")
                || path.equals("trident")
                || path.endsWith("_arrow")
                || path.equals("saddle")
                || path.endsWith("_minecart")
                || path.endsWith("_boat")
                || path.endsWith("_chest_boat");
    }

    private static boolean isUtility(String path) {
        return path.contains("compass")
                || path.equals("clock")
                || path.equals("map")
                || path.endsWith("_map")
                || path.equals("torch")
                || path.endsWith("_torch")
                || path.equals("soul_torch")
                || path.equals("lantern")
                || path.endsWith("_lantern")
                || path.equals("fishing_rod")
                || path.equals("carrot_on_a_stick")
                || path.equals("warped_fungus_on_a_stick")
                || path.equals("bucket")
                || path.equals("glass_bottle")
                || path.equals("lead")
                || path.equals("name_tag");
    }

    private static int materialTier(String path) {
        if (path.contains("wood") || path.contains("oak") || path.contains("bamboo")
                || path.contains("cherry") || path.contains("crimson") || path.contains("warped")) return 0;
        if (path.contains("stone") || path.contains("cobble") || path.contains("chainmail")) return 1;
        if (path.contains("copper")) return 2;
        if (path.contains("iron") || path.contains("turtle")) return 3;
        if (path.contains("gold") || path.contains("golden")) return 4;
        if (path.contains("diamond")) return 5;
        if (path.contains("netherite")) return 6;
        if (path.contains("leather") || path.contains("rabbit")) return 0;
        return 50;
    }

    private static int toolType(String path) {
        if (path.endsWith("_pickaxe")) return 0;
        if (path.endsWith("_axe")) return 1;
        if (path.endsWith("_shovel")) return 2;
        if (path.endsWith("_hoe")) return 3;
        if (path.equals("shears")) return 4;
        if (path.equals("flint_and_steel")) return 5;
        if (path.equals("brush")) return 6;
        return 20;
    }

    private static int armorType(String path) {
        if (path.endsWith("_helmet")) return 0;
        if (path.endsWith("_chestplate")) return 1;
        if (path.endsWith("_leggings")) return 2;
        if (path.endsWith("_boots")) return 3;
        if (path.endsWith("_horse_armor")) return 4;
        return 5;
    }

    private static int combatType(String path) {
        if (path.endsWith("_sword")) return 0;
        if (path.equals("bow")) return 1;
        if (path.equals("crossbow")) return 2;
        if (path.equals("trident")) return 3;
        if (path.endsWith("_arrow")) return 4;
        if (path.equals("saddle")) return 5;
        if (path.endsWith("_boat") || path.endsWith("_chest_boat")) return 6;
        return 7;
    }

    private static int utilityType(String path) {
        if (path.equals("fishing_rod")
                || path.equals("carrot_on_a_stick")
                || path.equals("warped_fungus_on_a_stick")) return 0;
        if (path.equals("bucket") || path.equals("glass_bottle")) return 1;
        if (path.contains("compass")) return 2;
        if (path.equals("clock")) return 3;
        if (path.equals("map") || path.endsWith("_map")) return 4;
        if (path.equals("torch") || path.endsWith("_torch")) return 5;
        if (path.equals("lantern") || path.endsWith("_lantern")) return 6;
        if (path.equals("lead")) return 7;
        return 8;
    }

    private static String itemPath(ItemStack result) {
        Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    public record ResourcesFoodOrder(int section, int family, int tier, int form, String id)
            implements Comparable<ResourcesFoodOrder> {
        @Override
        public int compareTo(ResourcesFoodOrder other) {
            int result = Integer.compare(this.section, other.section);
            if (result != 0) return result;
            result = Integer.compare(this.family, other.family);
            if (result != 0) return result;
            result = Integer.compare(this.tier, other.tier);
            if (result != 0) return result;
            result = Integer.compare(this.form, other.form);
            if (result != 0) return result;
            return this.id.compareTo(other.id);
        }
    }

    public record EquipmentOrder(int section, int tier, int type, String id)
            implements Comparable<EquipmentOrder> {
        @Override
        public int compareTo(EquipmentOrder other) {
            int result = Integer.compare(this.section, other.section);
            if (result != 0) return result;
            result = Integer.compare(this.tier, other.tier);
            if (result != 0) return result;
            result = Integer.compare(this.type, other.type);
            if (result != 0) return result;
            return this.id.compareTo(other.id);
        }
    }

    public record BuildingOrder(int family, int material, int subtype, String id)
            implements Comparable<BuildingOrder> {
        @Override
        public int compareTo(BuildingOrder other) {
            int result = Integer.compare(this.family, other.family);
            if (result != 0) return result;
            result = Integer.compare(this.material, other.material);
            if (result != 0) return result;
            result = Integer.compare(this.subtype, other.subtype);
            if (result != 0) return result;
            return this.id.compareTo(other.id);
        }
    }

    public record DecorationOrder(int family, int subtype, int material, String id)
            implements Comparable<DecorationOrder> {
        @Override
        public int compareTo(DecorationOrder other) {
            int result = Integer.compare(this.family, other.family);
            if (result != 0) return result;
            result = Integer.compare(this.subtype, other.subtype);
            if (result != 0) return result;
            result = Integer.compare(this.material, other.material);
            if (result != 0) return result;
            return this.id.compareTo(other.id);
        }
    }

    public record RedstoneOrder(int family, int subtype, int material, String id)
            implements Comparable<RedstoneOrder> {
        @Override
        public int compareTo(RedstoneOrder other) {
            int result = Integer.compare(this.family, other.family);
            if (result != 0) return result;
            result = Integer.compare(this.subtype, other.subtype);
            if (result != 0) return result;
            result = Integer.compare(this.material, other.material);
            if (result != 0) return result;
            return this.id.compareTo(other.id);
        }
    }

    private static CraftingCategory explicitCategory(ItemStack result) {
        for (CraftingCategory category : CraftingCategory.values()) {
            if (result.typeHolder().is(CATEGORY_TAGS.get(category))) {
                return category;
            }
        }
        return null;
    }

    private static boolean isTransport(ItemStack result) {
        return result.getItem() instanceof BoatItem
                || result.getItem() instanceof MinecartItem
                || result.typeHolder().is(ItemTags.BOATS)
                || result.typeHolder().is(ItemTags.CHEST_BOATS)
                || result.typeHolder().is(ItemTags.RAILS);
    }

    private static boolean isFirework(ItemStack result) {
        return result.has(DataComponents.FIREWORKS)
                || result.has(DataComponents.FIREWORK_EXPLOSION);
    }

    private static boolean isFoodOrHealth(ItemStack result) {
        return result.has(DataComponents.FOOD)
                || result.has(DataComponents.CONSUMABLE)
                || result.typeHolder().is(Tags.Items.FOODS)
                || result.typeHolder().is(Tags.Items.DRINKS);
    }

    private static boolean isEquipment(ItemStack result) {
        return result.has(DataComponents.TOOL)
                || result.has(DataComponents.WEAPON)
                || result.has(DataComponents.EQUIPPABLE)
                || result.has(DataComponents.BLOCKS_ATTACKS)
                || result.has(DataComponents.PIERCING_WEAPON)
                || result.has(DataComponents.KINETIC_WEAPON)
                || result.typeHolder().is(Tags.Items.TOOLS)
                || result.typeHolder().is(Tags.Items.ARMORS);
    }

    private static boolean isMaterial(ItemStack result) {
        if (result.getItem() instanceof SmithingTemplateItem
                || result.has(DataComponents.PROVIDES_BANNER_PATTERNS)) {
            return true;
        }
        if (MATERIAL_TAGS.stream().anyMatch(result.typeHolder()::is)) {
            return true;
        }
        Item item = result.getItem();
        return item == Items.PAPER
                || item == Items.BOOK
                || item == Items.SUGAR
                || item == Items.BLAZE_POWDER
                || item == Items.MAGMA_CREAM
                || item == Items.FERMENTED_SPIDER_EYE
                || item == Items.GLISTERING_MELON_SLICE
                || item == Items.ENDER_EYE;
    }

    private static boolean isDecoration(ItemStack result) {
        if (DECORATION_TAGS.stream().anyMatch(result.typeHolder()::is)) {
            return true;
        }
        Item item = result.getItem();
        String path = itemPath(result);
        return item == Items.ARMOR_STAND
                || item == Items.DECORATED_POT
                || item == Items.FLOWER_POT
                || item == Items.GLOW_ITEM_FRAME
                || item == Items.ITEM_FRAME
                || item == Items.PAINTING
                || path.equals("beacon")
                || path.equals("enchanting_table")
                || path.equals("end_rod")
                || path.equals("campfire")
                || path.equals("soul_campfire")
                || path.equals("lodestone")
                || path.equals("respawn_anchor")
                || path.equals("dried_ghast")
                || path.equals("bookshelf")
                || path.equals("chiseled_bookshelf")
                || path.endsWith("_shelf")
                || path.equals("anvil")
                || path.equals("chipped_anvil")
                || path.equals("damaged_anvil")
                || path.equals("jukebox")
                || path.equals("lantern")
                || path.equals("soul_lantern")
                || path.equals("torch")
                || path.equals("soul_torch")
                || path.endsWith("_torch")
                || path.endsWith("_lantern")
                || path.equals("lightning_rod")
                || path.endsWith("_lightning_rod")
                || path.equals("ladder")
                || path.contains("copper_golem")
                || path.contains("golem_statue");
    }

    private static Map<CraftingCategory, TagKey<Item>> createCategoryTags() {
        Map<CraftingCategory, TagKey<Item>> tags = new EnumMap<>(CraftingCategory.class);
        for (CraftingCategory category : CraftingCategory.values()) {
            tags.put(
                    category,
                    TagKey.create(
                            Registries.ITEM,
                            Identifier.fromNamespaceAndPath(
                                    Java_reforged.MODID,
                                    TAG_ROOT + category.name().toLowerCase(Locale.ROOT)
                            )
                    )
            );
        }
        return Map.copyOf(tags);
    }
}
