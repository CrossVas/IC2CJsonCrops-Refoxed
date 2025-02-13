package ic2.crops;

import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Predicate;

public record JsonCropRequirements(int growth, int minLightLevel, int maxLightLevel, int minHumidity, int maxHumidity, Predicate<Block> blocksBelow) {

    public JsonCropRequirements {
        Objects.requireNonNull(blocksBelow, "blocksBelow can be null, but it's recommended to check its usage.");
    }
}
