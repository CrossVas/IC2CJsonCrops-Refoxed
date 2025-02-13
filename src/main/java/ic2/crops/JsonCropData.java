package ic2.crops;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICrop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public record JsonCropData(ResourceLocation id,
                           String name, String discoveredBy,
                           ItemStack displayItem,
                           CropProperties properties, List<String> attributes,
                           List<String> textures,
                           int growthSteps,
                           List<ItemStack> drops,
                           ICrop.CropType cropType,
                           int optimalHarvestStep,
                           List<JsonCropRequirements> stages,
                           boolean droppingSeeds,
                           List<ItemStack> seedDrops) {

    public JsonCropData {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(discoveredBy, "discoveredBy cannot be null");
        Objects.requireNonNull(displayItem, "displayItem cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(attributes, "attributes cannot be null");
        Objects.requireNonNull(textures, "textures cannot be null");
        Objects.requireNonNull(drops, "drops cannot be null");
        Objects.requireNonNull(cropType, "cropType cannot be null");
        Objects.requireNonNull(stages, "stages cannot be null");
        Objects.requireNonNull(seedDrops, "seedDrops cannot be null");
    }
}
