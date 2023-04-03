package sonar.reactorbuilder.common.dictionary;

import net.minecraft.item.ItemStack;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.util.MCUtils;

import java.util.List;

public class DynamicItemDictionary {
    public static DictionaryEntry getOrCreateEntry(String entryId) {
        return getOrCreateEntry(MCUtils.parseItemLocator(entryId));
    }

    public static DictionaryEntry getOrCreateEntry(MCUtils.ItemLocator locator) {
        DictionaryEntry entry = tryGetExistingEntry(locator);

        if (entry != null) {
            return entry;
        }

        DictionaryEntryType type;

        if (locator.itemId.equals("fission_casing")) {
            type = DictionaryEntryType.OVERHAUL_CASING_SOLID;
        } else if (locator.itemId.equals("fission_glass")) {
            type = DictionaryEntryType.OVERHAUL_CASING_GLASS;
        } else {
            type = DictionaryEntryType.OVERHAUL_COMPONENT;
        }

        ItemStack found = MCUtils.getItemStack(locator);

        if (found == null) {
            return null;
        }


        return GlobalDictionary.addDictionaryItemEntry(
                type, locator.toString(), locator.modId, locator.itemId,
                locator.meta
        );
    }

    public static DictionaryEntry getOrCreateEntry(
            DictionaryEntryType type,
            String globalName,
            List<ItemStack> stacks,
            boolean ignoreMeta
    ) {
        DictionaryEntry entry = GlobalDictionary.getComponentInfo(globalName);

        if (entry != null) {
            return entry;
        }

        return GlobalDictionary.addDictionaryItemEntry(type, globalName, stacks, ignoreMeta);
    }

    public static DictionaryEntry tryGetExistingEntry(MCUtils.ItemLocator locator) {
        DictionaryEntry entry = GlobalDictionary.getComponentInfo(locator.itemId);

        if (entry == null) {
            return GlobalDictionary.getComponentInfo(locator.toString());
        }

        return entry;
    }
}
