package sonar.reactorbuilder.common.files;

import com.google.common.collect.Lists;
import simplelibrary.config2.Config;
import simplelibrary.config2.ConfigList;
import simplelibrary.config2.ConfigNumberList;
import sonar.reactorbuilder.common.dictionary.DynamicItemDictionary;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulFissionSFR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

        AbstractTemplate template = new CasingAwareOverhaulFissionSFR(
                filename, Math.toIntExact(dims.x), Math.toIntExact(dims.y), Math.toIntExact(dims.z)
        );

        int current = 0;

        for (int x = 0; x <= (dims.x + 1); x++) {
            for (int y = 0; y <= (dims.y + 1); y++) {
                for (int z = 0; z <= (dims.z + 1); z++) {
                    int blockId = Math.toIntExact(blockMap.get(current));

                    DictionaryEntry entry = blockRegistry.getEntryByIdAndType(blockId, StructureType.OverhaulSFR);

                    if (entry != null) {
                        template.setComponentInfo(entry, x, y, z);
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
                fetchConfigPathSafe(config, "underhaul", "fissionSFR"),
                StructureType.UnderhaulSFR
        );
        blocks.overhaulSFR = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "fissionSFR"),
                StructureType.OverhaulSFR
        );
        blocks.overhaulMSR = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "fissionMSR"),
                StructureType.OverhaulMSR
        );
        blocks.overhaulTurbine = fetchBlocksIn(
                fetchConfigPathSafe(config, "overhaul", "turbine"),
                StructureType.OverhaulTurbine
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

    private List<DictionaryEntry> fetchBlocksIn(Config config, StructureType type) {
        List<DictionaryEntry> blocks = new ArrayList<>();

        if (config != null && config.hasProperty("blocks")) {
            ConfigList list = config.getConfigList("blocks");

            for (Config block : list.<Config>iterable()) {
                addBlockToList(blocks, block, type);
            }
        }

        return blocks;
    }

    private void addBlockToList(List<DictionaryEntry> blocks,
                                Config block,
                                StructureType type) {
        addBlockToList(blocks, block, type, null);
    }

    private void addBlockToList(
            List<DictionaryEntry> blocks,
            Config block,
            StructureType type,
            DictionaryEntryType forceType
    ) {
        if (!block.hasProperty("name")) {
            blocks.add(null);
        } else {
            blocks.add(blockEntryFromConfig(block, type, forceType));
        }
        if (block.hasProperty("port")) {
            addBlockToList(
                    blocks,
                    block.getConfig("port"),
                    type,
                    DictionaryEntryType.OVERHAUL_CASING_FACE
            );
        }
    }

    private DictionaryEntry blockEntryFromConfig(
            Config block,
            StructureType structureType,
            DictionaryEntryType forceType
    ) {
        if (!block.hasProperty("name")) {
            return null;
        }

        String name = block.getString("name");

        DictionaryEntryType blockType = forceType;

        if (structureType.isOverhaul && blockType == null) {
            blockType = overhaulSFRBlockTypeFromConfig(block, structureType);
        }

        if (blockType == null) {
            return null;
        }

        return DynamicItemDictionary.getOrCreateEntry(
                blockType, name
        );
    }

    private DictionaryEntryType overhaulSFRBlockTypeFromConfig(Config block, StructureType type) {
        if (type != StructureType.OverhaulSFR) {
            return null;
        }

        if (block.hasProperty("casing") && block.getBoolean("casing")) {
            if (block.hasProperty("casingEdge") && block.getBoolean("casingEdge")) {
                return DictionaryEntryType.OVERHAUL_CASING_FRAME;
            }
            return DictionaryEntryType.OVERHAUL_CASING_FACE;
        }

        return DictionaryEntryType.OVERHAUL_COMPONENT;
    }

    enum StructureType {
        UnderhaulSFR(false),
        OverhaulSFR(true),
        OverhaulMSR(true),
        OverhaulTurbine(true);

        public boolean isOverhaul;

        StructureType(boolean isOverhaul) {
            this.isOverhaul = isOverhaul;
        }
    }

    private static class BlockRegistry {
        public List<DictionaryEntry> underhaulSFR = new ArrayList<>();
        public List<DictionaryEntry> overhaulSFR = new ArrayList<>();
        public List<DictionaryEntry> overhaulMSR = new ArrayList<>();
        public List<DictionaryEntry> overhaulTurbine = new ArrayList<>();

        void merge(BlockRegistry other) {
            underhaulSFR.addAll(other.underhaulSFR);
            overhaulSFR.addAll(other.overhaulSFR);
            overhaulMSR.addAll(other.overhaulMSR);
            overhaulTurbine.addAll(other.overhaulTurbine);
        }

        DictionaryEntry getEntryByIdAndType(int id, StructureType type) {
            return getSafeFromList(
                    pickList(type),
                    id - 1
            );
        }

        private List<DictionaryEntry> pickList(StructureType type) {
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

        private DictionaryEntry getSafeFromList(List<DictionaryEntry> list, int id) {
            if (id >= list.size() || id < 0) {
                return null;
            }

            return list.get(id);
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
