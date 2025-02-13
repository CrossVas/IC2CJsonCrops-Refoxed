package ic2.crops;

import ic2.core.block.crops.soils.BaseFarmland;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class JsonFarmland extends BaseFarmland {
    private final boolean canHydrate;

    public JsonFarmland(int humidity, int nutrients, boolean canHydrate) {
        super(humidity, nutrients);
        this.canHydrate = canHydrate;
    }

    @Override
    public int getHumidity(BlockState state) {
        if (this.canHydrate) {
            if (state == null) return 0;
            if (!state.hasProperty(BlockStateProperties.MOISTURE) || state.getValue(BlockStateProperties.MOISTURE) == 7) {
                return super.getHumidity(state);
            }
            return 0;
        }
        return super.getHumidity(state);
    }
}
