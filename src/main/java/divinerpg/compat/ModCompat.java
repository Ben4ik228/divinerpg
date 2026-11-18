package divinerpg.compat;

import divinerpg.DivineRPG;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModCompat {
    public static void initCommon(FMLCommonSetupEvent e) {
        if (ModList.get().isLoaded("aztech")) {
            DivineRPG.LOGGER.info("Together we thrive!");
        }
        if (ModList.get().isLoaded("aoa3")) {
            DivineRPG.LOGGER.info("The Divine Ascension");
        }
        if (ModList.get().isLoaded("journey")) {
            DivineRPG.LOGGER.info("Journey into the Divine");
        }
    }
}
