package sonar.reactorbuilder.common.files;

import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import simplelibrary.config2.Config;
import simplelibrary.config2.ConfigList;
import simplelibrary.config2.ConfigNumberList;
import sonar.reactorbuilder.common.dictionary.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.DictionaryEntryType;
import sonar.reactorbuilder.common.dictionary.GlobalDictionary;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.OverhaulFissionTemplate;
import sonar.reactorbuilder.util.MCUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ThizNewNCPFReader extends AbstractFileReader {

    public static final ThizNewNCPFReader INSTANCE = new ThizNewNCPFReader();

    public String error;

    @Override
    public AbstractTemplate readTemplate(File file) {
        String filename = file.getName();

        InputStream inStream;
        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            error = "Invalid input file! !";
            return null;
        }

        Config header = Config.newConfig(inStream).load();

        if (!header.hasProperty("version") || !header.hasProperty("count")) {
            error = "Invalid file format!";
            return null;
        }

        int count = header.getInt("count");

        if (header.getByte("version") != 11) {
            error = "Only version 11 is now supported!";
            return null;
        }

        BlockRegistry registry = readConfig(inStream);

        AbstractTemplate structure = readFirstSupportedStructure(
                inStream, filename,
                registry, count
        );

        if (structure == null) {
            error = "No valid structures in this data file!";
        }

        return structure;
    }

    private AbstractTemplate readFirstSupportedStructure(
            InputStream stream,
            String filename,
            BlockRegistry registry,
            int count
    ) {
        for (int idx = 0; idx < count; idx++) {
            Config data = Config.newConfig(stream).load();

            if (!data.hasProperty("id")) {
                continue;
            }

            if (data.getInt("id") == 1) {
                return readOverhaulSFRFrom(data, registry, filename);
            }
        }

        return null;
    }

    private AbstractTemplate readOverhaulSFRFrom(Config data, BlockRegistry blockRegistry, String filename) {
        Dim3D dims = readDimensionsFrom(data);

        if (dims == null) {
            return null;
        }

        if (!data.hasProperty("blocks")) {
            return null;
        }

        ConfigNumberList blockMap = data.getConfigNumberList("blocks");

        OverhaulFissionTemplate.SFR template = new OverhaulFissionTemplate.SFR(
                filename, Math.toIntExact(dims.x), Math.toIntExact(dims.y), Math.toIntExact(dims.z)
        );

        int current = 0;

        for (int x = 0; x <= (dims.x + 1); x++) {
            for (int y = 0; y <= (dims.y + 1); y++) {
                for (int z = 0; z <= (dims.z + 1); z++) {
                    if (x < 1 || y < 1 || z < 1 || x >= (dims.x + 1) || y >= (dims.y + 1) || z >= (dims.z + 1)) {
                        current += 1;
                        continue;
                    }

                    int blockId = Math.toIntExact(blockMap.get(current));

                    DictionaryEntry entry = blockRegistry.getEntryByIdAndType(blockId, BlockRegistry.Type.OverhaulSFR);

                    if (entry != null) {
                        template.setComponentInfo(entry, x - 1, y - 1, z - 1);
                    }

                    current += 1;
                }
            }
        }

        return template;
    }

    private Dim3D readDimensionsFrom(Config data) {
        if (!data.hasProperty("dimensions")) {
            return null;
        }

        ConfigNumberList dimensions = data.getConfigNumberList("dimensions");

        return new Dim3D(
                dimensions.get(0),
                dimensions.get(1),
                dimensions.get(2)
        );
    }

    private BlockRegistry readConfig(InputStream inStream) {
        Config config = Config.newConfig(inStream).load();

        return processConfig(config);
    }

    private <T> T fetchConfigPathSafe(
            Config config,
            String... path) {
        List<String> pathFragments = Lists.newArrayList(path);
        Object current = config;

        for (String fragment : pathFragments) {
            if (!(current instanceof Config)) {
                return null;
            }
            if (!((Config) current).hasProperty(fragment)) {
                return null;
            }
            current = ((Config) current).get(fragment);
        }

        return (T) current;
    }

    private BlockRegistry processConfig(Config config) {
        BlockRegistry blocks = new BlockRegistry();

        blocks.underhaulSFR = fetchBlocksIn(
                fetchConfigPathSafe(config, "underhaul", "fissionSFR")
        );
        blocks.overhaulSFR = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "fissionSFR")
        );
        blocks.overhaulMSR = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "fissionMSR")
        );
        blocks.overhaulTurbine = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "turbine")
        );

        if (!config.getBoolean("addon") && config.hasProperty("addons")) {
            ConfigList addons = config.getConfigList("addons");
            for (Config addon : addons.<Config>iterable()) {
                BlockRegistry addonBlocks = processConfig(addon);

                blocks.merge(addonBlocks);
            }
        }

        return blocks;
    }

    private List<String> fetchBlocksIn(Config config) {
        List<String> blocks = new ArrayList<>();

        if (config != null && config.hasProperty("blocks")) {
            ConfigList list = config.getConfigList("blocks");

            for (Config block : list.<Config>iterable()) {
                if (!block.hasProperty("name")) {
                    blocks.add(null);
                } else {
                    blocks.add(block.getString("name"));
                }
            }
        }

        return blocks;
    }

    private static class BlockRegistry {
        public List<String> underhaulSFR = new ArrayList<>();
        public List<String> overhaulSFR = new ArrayList<>();
        public List<String> overhaulMSR = new ArrayList<>();
        public List<String> overhaulTurbine = new ArrayList<>();

        void merge(BlockRegistry other) {
            underhaulSFR.addAll(other.underhaulSFR);
            overhaulSFR.addAll(other.overhaulSFR);
            overhaulMSR.addAll(other.overhaulMSR);
            overhaulTurbine.addAll(other.overhaulTurbine);
        }

        void eachBlock(Consumer<String> handler) {
            for (String item : underhaulSFR) {
                handler.accept(item);
            }
            for (String item : overhaulSFR) {
                handler.accept(item);
            }
            for (String item : overhaulMSR) {
                handler.accept(item);
            }
            for (String item : overhaulTurbine) {
                handler.accept(item);
            }
        }

        DictionaryEntry getEntryByIdAndType(int id, Type type) {
            String entryName = getEntryNameByIdAndType(id, type);
            if (entryName == null) {
                return null;
            }

            return getOrCreateEntry(entryName);
        }

        String getEntryNameByIdAndType(int id, Type type) {
            return getSafeFromList(
                    pickList(type),
                    id - 1
            );
        }

        private List<String> pickList(Type type) {
            switch (type) {
                case UnderhaulSFR:
                    return underhaulSFR;
                case OverhaulSFR:
                    return overhaulSFR;
                case OverhaulMSR:
                    return overhaulMSR;
                case OverhaulTurbine:
                    return overhaulTurbine;
                default:
                    throw new RuntimeException("invalid list type");
            }
        }

        private String getSafeFromList(List<String> list, int id) {
            if (id >= list.size() || id < 0) {
                return null;
            }

            return list.get(id);
        }

        private DictionaryEntry getOrCreateEntry(String entryId) {
            MCUtils.ItemLocator locator = MCUtils.parseItemLocator(entryId);

            DictionaryEntry entry = tryGetExistingEntry(locator);

            if (entry != null) {
                return entry;
            }

            DictionaryEntryType type;

            if (entryId.equals("nuclearcraft:fission_casing")) {
                type = DictionaryEntryType.OVERHAUL_CASING_SOLID;
            } else if (entryId.equals("nuclearcraft:fission_glass")) {
                type = DictionaryEntryType.OVERHAUL_CASING_GLASS;
            } else {
                type = DictionaryEntryType.OVERHAUL_COMPONENT;
            }

            ItemStack found = MCUtils.getItemStack(entryId);

            if (found == null) {
                return null;
            }


            return GlobalDictionary.addDictionaryItemEntry(
                    type, entryId, locator.modId, locator.itemId,
                    locator.meta
            );
        }

        private DictionaryEntry tryGetExistingEntry(MCUtils.ItemLocator locator) {
            DictionaryEntry entry = GlobalDictionary.getComponentInfo(locator.itemId);

            if (entry == null) {
                return GlobalDictionary.getComponentInfo(locator.toString());
            }

            return entry;
        }

        enum Type {
            UnderhaulSFR,
            OverhaulSFR,
            OverhaulMSR,
            OverhaulTurbine,
        }
    }

    private static class Dim3D {
        public long x;
        public long y;
        public long z;

        public Dim3D(long x, long y, long z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
