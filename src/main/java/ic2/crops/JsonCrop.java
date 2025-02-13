package ic2.crops;

import ic2.api.crops.ICropModifier;
import ic2.api.crops.ICropTile;
import ic2.api.crops.ISeedCrop;
import ic2.core.IC2;
import ic2.core.block.crops.crops.BaseCrop;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class JsonCrop extends BaseCrop implements ISeedCrop {
    private final JsonCropData data;

    public JsonCrop(JsonCropData data) {
        super(data.id(), data.properties(), data.attributes().toArray(new String[0]));
        this.data = data;
    }

    @Override
    public Component getName() {
        return this.translate(data.name());
    }

    @Override
    public Component discoveredBy() {
        return Component.literal(data.discoveredBy());
    }

    @Override
    public ItemStack getDisplayItem() {
        return data.displayItem();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<ResourceLocation> getTextures() {
        List<ResourceLocation> list = new ArrayList<>();
        for (String texture : data.textures()) {
            list.add(new ResourceLocation(texture));
        }
        return list;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public TextureAtlasSprite getTexture(int stage) {
        return null;
    }

    @Override
    public int getGrowthSteps() {
        return data.growthSteps();
    }

    @Override
    public ItemStack[] getDrops(ICropTile cropTile) {
        return data.drops().toArray(new ItemStack[0]);
    }

    @Override
    public CropType getCropType() {
        return data.cropType();
    }

    @Override
    public int getOptimalHarvestStep(ICropTile cropTile) {
        return data.optimalHarvestStep();
    }

    @Override
    public int getGrowthDuration(ICropTile cropTile) {
        int stage = cropTile.getGrowthStage();
        return getGrowthRequirement(stage).growth();
    }

    @Override
    public boolean canGrow(ICropTile cropTile) {
        boolean grow = super.canGrow(cropTile);
        int stage = cropTile.getGrowthStage();
        JsonCropRequirements growthRequirement = getGrowthRequirement(stage);

        if (growthRequirement.minLightLevel() > 0) {
            grow &= cropTile.getLightLevel() >= growthRequirement.minLightLevel();
        }
        if (growthRequirement.maxLightLevel() < 15) {
            grow &= cropTile.getLightLevel() <= growthRequirement.maxLightLevel();
        }
        if (growthRequirement.minHumidity() > 0) {
            grow &= cropTile.getHumidity() >= growthRequirement.minHumidity();
        }
        if (growthRequirement.maxHumidity() > 0) {
            grow &= cropTile.getHumidity() <= growthRequirement.maxHumidity();
        }
        if (growthRequirement.blocksBelow() != null) {
            boolean foundBlock = false;
            for (Block block : cropTile.getBlocksBelow()) {
                if (growthRequirement.blocksBelow().test(block)) {
                    foundBlock = true;
                    break;
                }
            }
            grow &= foundBlock;
        }
        return grow;
    }

    public JsonCropRequirements getGrowthRequirement(int stage) {
        int offset = stage - 1;
        if (data.stages().size() <= offset) return data.stages().get(data.stages().size() - 1);
        return data.stages().get(offset);
    }

    @Override
    public boolean onRightClick(ICropTile cropTile, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ICropModifier.canToggleSeedMode(stack) && data.droppingSeeds()) {
            boolean newSeed = !cropTile.getCustomData().getBoolean("seed");
            cropTile.getCustomData().putBoolean("seed", newSeed);
            if (IC2.PLATFORM.isSimulating()) {
                player.displayClientMessage(
                        this.translate(newSeed ? "info.crop.ic2.seed_mode.enable" : "info.crop.ic2.seed_mode.disable"),
                        false
                );
            }
            return true;
        }
        return super.onRightClick(cropTile, player, hand);
    }

    @Override
    public boolean isDroppingSeeds(ICropTile cropTile) {
        return data.droppingSeeds() && cropTile.getCustomData().getBoolean("seed");
    }

    @Override
    public ItemStack[] getSeedDrops(ICropTile cropTile) {
        return data.seedDrops().toArray(new ItemStack[0]);
    }
}
