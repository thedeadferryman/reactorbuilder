package sonar.reactorbuilder.common.dictionary;

import net.minecraft.item.ItemStack;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.util.MCUtils;

import java.util.List;

public class DynamicItemDictionary {
    public static DictionaryEntry getOrCreateEntry(DictionaryEntryType type, String entryId) {
        return getOrCreateEntry(type, MCUtils.parseItemLocator(entryId));
    }

    public static DictionaryEntry getOrCreateEntry(DictionaryEntryType type, MCUtils.ItemLocator locator) {
        DictionaryEntry entry = tryGetExistingEntry(locator);

        if (entry != null) {
            return entry;
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
