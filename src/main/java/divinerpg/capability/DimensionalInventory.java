package divinerpg.capability;

import divinerpg.events.VetheaInventorySwapEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public class DimensionalInventory implements INBTSerializable<CompoundTag> {
    ListTag overworldInventory = new ListTag();
    ListTag vetheaInventory = new ListTag();

    public void transferTo(DimensionalInventory target) {
        target.overworldInventory = this.overworldInventory;
        target.vetheaInventory = this.vetheaInventory;
    }

    public void saveInventory(Player player, String inv) {
        if (VetheaInventorySwapEvent.VETHEA_INVENTORY.equals(inv)) {
            vetheaInventory = player.getInventory().save(new ListTag());
        } else {
            overworldInventory = player.getInventory().save(new ListTag());
        }
    }

    public void loadInventory(Player player, String inv) {
        if (VetheaInventorySwapEvent.VETHEA_INVENTORY.equals(inv)) {
            player.getInventory().clearContent();
            player.getInventory().load(vetheaInventory);
        } else {
            player.getInventory().clearContent();
            player.getInventory().load(overworldInventory);
        }
        player.inventoryMenu.broadcastChanges();
    }

    public void clearInventory(String inv) {
        if (VetheaInventorySwapEvent.VETHEA_INVENTORY.equals(inv)) {
            vetheaInventory = new ListTag();
        } else {
            overworldInventory = new ListTag();
        }
    }

    public void stateVetheanInventory() {
        System.err.println("Current Vethea Inventory: " + vetheaInventory);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("overworld_inventory", overworldInventory);
        tag.put("vethea_inventory", vetheaInventory);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        overworldInventory = tag.getList("overworld_inventory", 10);
        vetheaInventory = tag.getList("vethea_inventory", 10);
    }
}
