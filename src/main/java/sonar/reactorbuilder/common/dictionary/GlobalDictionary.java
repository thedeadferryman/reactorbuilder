package sonar.reactorbuilder.common.dictionary;

import com.google.common.collect.Lists;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import sonar.reactorbuilder.ReactorBuilder;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.dictionary.entry.FluidEntry;
import sonar.reactorbuilder.common.dictionary.entry.ItemEntry;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlobalDictionary {

    ///maps globalName to component
    public static Map<String, DictionaryEntry> GLOBAL_DICTIONARY;

    private static int globalID;
    private static int[] componentTallies;

    public static void initDictionary(boolean isOverhaul) {
        GLOBAL_DICTIONARY = new LinkedHashMap<>();
        globalID = 0;
        componentTallies = new int[DictionaryEntryType.values().length];

        ///build dictionaries
        if (isOverhaul) {
            OverhaulDictionary.buildDictionary();
        } else {
            UnderhaulDictionary.buildDictionary();
        }

        for (DictionaryEntryType type : DictionaryEntryType.values()) {
            if (type.isOverhaul == isOverhaul) {
                ReactorBuilder.logger.info("Loaded {} {} types", componentTallies[type.ordinal()], type.logName);
            }
        }
    }

    public static DictionaryEntry addDictionaryItemEntry(DictionaryEntryType type, String globalName, String modid, String name, Integer meta) {
        int inMeta = meta == null ? 0 : meta;
        return addDictionaryItemEntry(type, globalName, modid, name, inMeta, meta == null);
    }

    public static DictionaryEntry addDictionaryItemEntry(DictionaryEntryType type, String globalName, String modid, String name, int meta) {
        return addDictionaryItemEntry(type, globalName, modid, name, meta, false);
    }

    public static DictionaryEntry addDictionaryItemEntry(DictionaryEntryType type, String globalName, String modid, String name, int meta, boolean ignoreMeta) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, name));

        if (item == null) {
            ReactorBuilder.logger.error("Dictionary Error: Missing {} {}, Item: {}:{}:{}", type, globalName, modid, name, meta);
            return null;
        }

        return addDictionaryItemEntry(
                type, globalName,
                Lists.newArrayList(new ItemStack(item, 1, meta)), ignoreMeta
        );
    }

    public static DictionaryEntry addDictionaryItemEntry(
            DictionaryEntryType type, String globalName,
            List<ItemStack> stacks,
            boolean ignoreMeta
    ) {
        DictionaryEntry entry = new ItemEntry(
                globalID++, globalName, type, stacks
        );
        GLOBAL_DICTIONARY.put(globalName, entry);
        componentTallies[type.ordinal()]++;

        if (ignoreMeta) {
            entry.ignoreMeta();
        }

        return entry;
    }

    public static void addDictionaryFluidEntry(DictionaryEntryType type, String globalName, String fluidName) {
        FluidStack fluid = FluidRegistry.getFluidStack(fluidName, 1000);

        if (fluid == null) {
            ReactorBuilder.logger.error("Dictionary Error: Missing {} {}, Fluid: {}", type, globalName, fluidName);
            return;
        }

        GLOBAL_DICTIONARY.put(globalName, new FluidEntry(globalID++, globalName, type, fluid));
        componentTallies[type.ordinal()]++;
    }

    @Nullable
    public static DictionaryEntry getComponentInfo(String globalName) {
        return GLOBAL_DICTIONARY.get(globalName);
    }

    /**
     * this method should not be used when saving to NBT, as globalIDs might not always match.
     *
     * @param globalID the temporary global id
     * @return the component
     */
    @Nullable
    public static DictionaryEntry getComponentInfoFromID(int globalID) {
        if (globalID == -1) {
            return null;
        }

        for (DictionaryEntry info : GLOBAL_DICTIONARY.values()) {
            if (info.globalID == globalID) {
                return info;
            }
        }
        return null;
    }

}
