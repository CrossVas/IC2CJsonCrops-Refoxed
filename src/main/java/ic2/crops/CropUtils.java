package ic2.crops;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ic2.api.crops.BaseSeed;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICrop;
import ic2.core.block.crops.CropRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CropUtils {

    public static JsonCropData cropFromJsonObject(JsonObject jsonObject) {
        if (!jsonObject.has("id")) {
            throw new IllegalArgumentException("Missing id element");
        }
        if (!jsonObject.has("name")) {
            throw new IllegalArgumentException("Missing name element");
        }
        if (!jsonObject.has("discoveredBy")) {
            throw new IllegalArgumentException("Missing discoveredBy element");
        }
        if (!jsonObject.has("displayItem")) {
            throw new IllegalArgumentException("Missing displayItem element");
        }
        if (!jsonObject.has("properties")) {
            throw new IllegalArgumentException("Missing properties element");
        }

        JsonObject propertiesObject = jsonObject.getAsJsonObject("properties");
        if (!propertiesObject.has("tier") || !propertiesObject.has("chemistry")
                || !propertiesObject.has("consumable") || !propertiesObject.has("defensive")
                || !propertiesObject.has("colorful") || !propertiesObject.has("weed")) {
            throw new IllegalArgumentException("Properties element missing required properties");
        }

        if (!jsonObject.has("attributes")) {
            throw new IllegalArgumentException("Missing attributes element");
        }
        if (!jsonObject.has("textures")) {
            throw new IllegalArgumentException("Missing textures element");
        }
        if (!jsonObject.has("growthSteps")) {
            throw new IllegalArgumentException("Missing growthSteps element");
        }
        if (!jsonObject.has("drops")) {
            throw new IllegalArgumentException("Missing drops element");
        }

        ResourceLocation id = new ResourceLocation(jsonObject.get("id").getAsString());
        String name = jsonObject.get("name").getAsString();
        String discoveredBy = jsonObject.get("discoveredBy").getAsString();
        JsonElement displayItemElement = jsonObject.get("displayItem");

        ItemStack displayItem = displayItemElement.isJsonObject()
                ? CraftingHelper.getItemStack(displayItemElement.getAsJsonObject(), true)
                : new ItemStack(CraftingHelper.getItem(displayItemElement.getAsString(), false));

        CropProperties properties = new CropProperties(
                propertiesObject.get("tier").getAsInt(),
                propertiesObject.get("chemistry").getAsInt(),
                propertiesObject.get("consumable").getAsInt(),
                propertiesObject.get("defensive").getAsInt(),
                propertiesObject.get("colorful").getAsInt(),
                propertiesObject.get("weed").getAsInt()
        );

        List<String> attributes = new ArrayList<>();
        for (JsonElement element : jsonObject.getAsJsonArray("attributes")) {
            attributes.add(element.getAsString());
        }

        List<String> textures = new ArrayList<>();
        for (JsonElement element : jsonObject.getAsJsonArray("textures")) {
            textures.add(element.getAsString());
        }

        int growthSteps = jsonObject.get("growthSteps").getAsInt();
        if (textures.size() != growthSteps) {
            throw new IllegalArgumentException("Textures array doesn't have enough elements!");
        }

        List<ItemStack> drops = new ArrayList<>();
        for (JsonElement element : jsonObject.getAsJsonArray("drops")) {
            drops.add(element.isJsonObject()
                    ? CraftingHelper.getItemStack(element.getAsJsonObject(), true)
                    : new ItemStack(CraftingHelper.getItem(element.getAsString(), false)));
        }

        ICrop.CropType cropType = jsonObject.has("cropType")
                ? ICrop.CropType.valueOf(jsonObject.get("cropType").getAsString())
                : ICrop.CropType.AIR;

        int optionalHarvestStep = jsonObject.has("harvestStep") ? jsonObject.get("harvestStep").getAsInt() : growthSteps;

        boolean droppingSeeds = jsonObject.has("droppingSeeds") && jsonObject.getAsJsonObject("droppingSeeds").getAsBoolean();

        List<ItemStack> seedDrops = new ArrayList<>();
        if (jsonObject.has("seedDrops")) {
            for (JsonElement element : jsonObject.getAsJsonArray("seedDrops")) {
                seedDrops.add(element.isJsonObject()
                        ? CraftingHelper.getItemStack(element.getAsJsonObject(), true)
                        : new ItemStack(CraftingHelper.getItem(element.getAsString(), false)));
            }
        }

        return new JsonCropData(id, name, discoveredBy, displayItem, properties, attributes, textures,
                growthSteps, drops, cropType, optionalHarvestStep, new ArrayList<>(), droppingSeeds, seedDrops);
    }

    public static BaseSeed seedFromJsonObject(JsonObject jsonObject) {
        if (!jsonObject.has("crop")) {
            throw new IllegalArgumentException("Seed JSON missing crop element");
        }

        String cropString = jsonObject.get("crop").getAsString();
        ICrop crop = CropRegistry.REGISTRY.getCrop(new ResourceLocation(cropString));

        if (crop == null) {
            throw new IllegalArgumentException("Crop " + cropString + " does not exist");
        }

        int stage = jsonObject.has("stage") ? jsonObject.get("stage").getAsInt() : 1;
        if (stage > crop.getGrowthSteps()) {
            throw new IllegalArgumentException("Seed defines a growth stage greater than the max");
        }

        int growth = jsonObject.has("growth") ? jsonObject.get("growth").getAsInt() : 1;
        int gain = jsonObject.has("gain") ? jsonObject.get("gain").getAsInt() : 1;
        int resistance = jsonObject.has("resistance") ? jsonObject.get("resistance").getAsInt() : 1;
        int stackSize = jsonObject.has("stackSize") ? jsonObject.get("stackSize").getAsInt() : 1;

        return new BaseSeed(crop, stage, growth, gain, resistance, stackSize);
    }

    public static void readFromFile(String path, Consumer<JsonObject> function) {
        Path cropJsonsPath = FMLPaths.CONFIGDIR.get().resolve("ic2c").resolve(path);

        if (!Files.exists(cropJsonsPath) || !Files.isDirectory(cropJsonsPath)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cropJsonsPath)) {
            for (Path filePath : stream) {
                if (Files.isRegularFile(filePath) && filePath.toString().endsWith(".json")) {
                    String additionalError = null;
                    try {
                        JsonObject parsed = JsonParser.parseReader(Files.newBufferedReader(filePath)).getAsJsonObject();
                        try {
                            function.accept(parsed);
                        } catch (Exception e) {
                            additionalError = parsed.toString();
                            throw e;
                        }
                    } catch (Exception e) {
                        if (additionalError != null) {
                            IC2CJsonCrops.LOGGER.error(additionalError);
                        }
                        IC2CJsonCrops.LOGGER.error("JSON not valid!", e);
                    }
                }
            }
        } catch (IOException e) {
            IC2CJsonCrops.LOGGER.error("Error reading JSON files", e);
        }
    }

    public static void writeExampleConfig(String subDir, String readName) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("ic2c").resolve(subDir);
        Path target = dir.resolve(readName);

        try {
            Files.createDirectories(dir);
            try (InputStream in = IC2CJsonCrops.class.getResourceAsStream("/" + readName)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + readName);
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
