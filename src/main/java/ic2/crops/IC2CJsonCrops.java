package ic2.crops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ic2.api.crops.BaseSeed;
import ic2.api.crops.ICrop;
import ic2.core.block.crops.CropRegistry;
import ic2.core.block.crops.soils.BaseSubSoil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static ic2.crops.CropUtils.*;

@Mod(IC2CJsonCrops.ID)
public class IC2CJsonCrops {
    public static final String ID = "ic2c_json_crops";

    public static final Logger LOGGER = LogManager.getLogger(ID);

    public IC2CJsonCrops() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCropRegister(FMLCommonSetupEvent event) {
        if (EffectiveSide.get().isClient()) {
            writeExampleConfig("crops", "example-crop.json5");
            writeExampleConfig("farmlands", "example-farmland.json5");
            writeExampleConfig("seeds", "example-seed.json5");
            writeExampleConfig("subsoils", "example-subsoil.json5");
        }

        readFromFile("crops", j -> {
            JsonCropData cropData = cropFromJsonObject(j);
            ResourceLocation id = cropData.id();
            ICrop crop = CropRegistry.REGISTRY.getCrop(id);
            if (crop != null) {
                throw new IllegalArgumentException("Crop " + crop.id() + " already exists");
            }
            crop = new JsonCrop(cropData);
            CropRegistry.REGISTRY.registerCrop(crop);
        });

        readFromFile("seeds", j -> {
            if (!j.has("item")) {
                throw new IllegalArgumentException("Seed JSON missing item element");
            }
            Item item = CraftingHelper.getItem(j.get("item").getAsString(), false);
            BaseSeed seed = seedFromJsonObject(j);
            CropRegistry.REGISTRY.registerBaseSeed(item, seed);
        });

        BiConsumer<JsonObject, Boolean> function = (j, isSoil) -> {
            String file = isSoil ? "subsoil" : "farmland";
            if (!j.has("humidity")) {
                throw new IllegalArgumentException(file + " JSON missing humidity element");
            }
            if (!j.has("nutrients")) {
                throw new IllegalArgumentException(file + " JSON missing nutrients element");
            }
            if (!j.has("blocks")) {
                throw new IllegalArgumentException(file + " JSON missing block element");
            }

            JsonArray array = j.getAsJsonArray("blocks");
            if (array.size() == 0) {
                throw new IllegalArgumentException("Blocks array is empty");
            }

            List<Block> blocks = new ArrayList<>();
            for (JsonElement element : array) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(element.getAsString()));
                if (block == Blocks.AIR) {
                    throw new IllegalArgumentException(file + " JSON defined an invalid block or air in blocks");
                }
                blocks.add(block);
            }

            if (isSoil) {
                BaseSubSoil soil = new BaseSubSoil(j.get("humidity").getAsInt(), j.get("nutrients").getAsInt());
                CropRegistry.REGISTRY.registerSubSoil(soil, blocks.toArray(new Block[0]));
            } else {
                JsonFarmland farmland = new JsonFarmland(
                        j.get("humidity").getAsInt(),
                        j.get("nutrients").getAsInt(),
                        j.has("canHydrate") && j.get("canHydrate").getAsBoolean()
                );
                CropRegistry.REGISTRY.registerFarmland(farmland, blocks.toArray(new Block[0]));
            }
        };

        readFromFile("farmlands", j -> function.accept(j, false));
        readFromFile("subsoils", j -> function.accept(j, true));
    }
}

