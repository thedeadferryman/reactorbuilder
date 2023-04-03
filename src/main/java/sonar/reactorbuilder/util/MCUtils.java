package sonar.reactorbuilder.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class MCUtils {

    public static ItemLocator parseItemLocator(String id) {
        String modid = "minecraft";

        if (id.indexOf(':') > -1) {
            modid = id.substring(0, id.indexOf(':'));
        }

        String itemId = id.substring(id.indexOf(':') + 1);

        String rawItemId = itemId;
        String meta = null;

        if (itemId.indexOf(':') > -1) {
            rawItemId = itemId.substring(0, itemId.indexOf(':'));
            meta = itemId.substring(itemId.indexOf(':') + 1);
        }


        try {
            Integer rawMeta = Integer.parseInt(meta, 10);
            return new ItemLocator(modid, rawItemId, rawMeta);
        } catch (NumberFormatException e) {
            return new ItemLocator(modid, rawItemId, null);
        }
    }

    public static ItemStack getItemStack(String id) {
        return getItemStack(parseItemLocator(id));
    }

    public static ItemStack getItemStack(ItemLocator locator) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(locator.modId, locator.itemId));

        if (item == null) {
            return null;
        }

        if (locator.meta != null) {
            return new ItemStack(item, 1, locator.meta);
        }

        return new ItemStack(item);
    }

    public static class ItemLocator {
        public final String modId;
        public final String itemId;

        public final Integer meta;

        public ItemLocator(String modId, String itemId, Integer meta) {
            this.modId = modId;
            this.itemId = itemId;
            this.meta = meta;
        }

        @Override
        public String toString() {
            String baseName = this.modId + ":" + this.itemId;

            if (meta == null) {
                return baseName;
            }

            return baseName + ":" + meta;
        }
    }
}
