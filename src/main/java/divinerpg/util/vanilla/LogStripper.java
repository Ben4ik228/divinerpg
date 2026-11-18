package divinerpg.util.vanilla;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashMap;
import java.util.Map;

import static divinerpg.registries.BlockRegistry.*;

public class LogStripper {
    private static final Map<Block, Block> STRIPPABLES = new HashMap<>();

    public static void setup(final FMLCommonSetupEvent event) {
        STRIPPABLES.put(divineLog.get(), strippedDivineLog.get());
        STRIPPABLES.put(divineWood.get(), strippedDivineWood.get());
        STRIPPABLES.put(shiverspineLog.get(), strippedShiverspineLog.get());
        STRIPPABLES.put(shiverspineWood.get(), strippedShiverspineWood.get());
        STRIPPABLES.put(auroraoakLog.get(), strippedAuroraoakLog.get());
        STRIPPABLES.put(auroraoakWood.get(), strippedAuroraoakWood.get());
        STRIPPABLES.put(cozybarkLog.get(), strippedCozybarkLog.get());
        STRIPPABLES.put(cozybarkWood.get(), strippedCozybarkWood.get());
        STRIPPABLES.put(streamleafLog.get(), strippedStreamleafLog.get());
        STRIPPABLES.put(streamleafWood.get(), strippedStreamleafWood.get());
        STRIPPABLES.put(eucalyptusLog.get(), strippedEucalyptusLog.get());
        STRIPPABLES.put(eucalyptusWood.get(), strippedEucalyptusWood.get());
        STRIPPABLES.put(edenLog.get(), strippedEdenLog.get());
        STRIPPABLES.put(edenWood.get(), strippedEdenWood.get());
        STRIPPABLES.put(wildwoodLog.get(), strippedWildwoodLog.get());
        STRIPPABLES.put(wildwoodWood.get(), strippedWildwoodWood.get());
        STRIPPABLES.put(apalachiaLog.get(), strippedApalachiaLog.get());
        STRIPPABLES.put(apalachiaWood.get(), strippedApalachiaWood.get());
        STRIPPABLES.put(skythernLog.get(), strippedSkythernLog.get());
        STRIPPABLES.put(skythernWood.get(), strippedSkythernWood.get());
        STRIPPABLES.put(mortumLog.get(), strippedMortumLog.get());
        STRIPPABLES.put(mortumWood.get(), strippedMortumWood.get());
        STRIPPABLES.put(firewoodLog.get(), strippedFirewoodLog.get());
        STRIPPABLES.put(firewoodWood.get(), strippedFirewoodWood.get());
        STRIPPABLES.put(dreamwoodLog.get(), strippedDreamwoodLog.get());
        STRIPPABLES.put(dreamwoodWood.get(), strippedDreamwoodWood.get());
        STRIPPABLES.put(hyrewoodLog.get(), strippedHyrewoodLog.get());
        STRIPPABLES.put(hyrewoodWood.get(), strippedHyrewoodWood.get());
        STRIPPABLES.put(mintwoodLog.get(), strippedMintwoodLog.get());
        STRIPPABLES.put(mintwoodWood.get(), strippedMintwoodWood.get());
        MinecraftForge.EVENT_BUS.addListener(LogStripper::onBlockToolModification);
    }

    private static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.AXE_STRIP) {
            return;
        }
        BlockState state = event.getState();
        Block stripped = STRIPPABLES.get(state.getBlock());
        if (stripped != null) {
            event.setFinalState(stripped.withPropertiesOf(state));
        }
    }
}
