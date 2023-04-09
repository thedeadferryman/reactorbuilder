package sonar.reactorbuilder.common.files.ncpf;

import com.google.common.collect.Lists;
import net.minecraft.util.math.BlockPos;
import simplelibrary.config2.Config;
import simplelibrary.config2.ConfigList;
import simplelibrary.config2.ConfigNumberList;
import sonar.reactorbuilder.common.dictionary.DynamicItemDictionary;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.files.AbstractFileReader;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.CasingAwareTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulFissionSFR;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulTurbine;

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

            StructureType type = parseTypeid(data.getInt("id"));

            if (type == null) {
                return null;
            }

            CasingAwareTemplate template = initializeTemplate(data, filename, type);

            return readStructure(template, data, registry);
        }

        return null;
    }

    private StructureType parseTypeid(int typeid) {
        switch (typeid) {
            case 1:
                return StructureType.OverhaulSFR;
            case 3:
                return StructureType.OverhaulTurbine;
            default:
                return null;
        }
    }

    private CasingAwareTemplate initializeTemplate(Config data, String filename, StructureType type) {
        BlockPos dims = readDimensionsFrom(data);

        if (dims == null) {
            return null;
        }

        switch (type) {
            case OverhaulSFR:
                return new CasingAwareOverhaulFissionSFR(
                        filename,
                        dims.getX(),
                        dims.getY(),
                        dims.getZ()
                );
            case OverhaulTurbine:
                return new CasingAwareOverhaulTurbine(
                        filename,
                        dims.getX(),
                        dims.getY(),
                        dims.getZ()
                );
            default:
                return null;
        }
    }

    private CasingAwareTemplate readStructure(
            CasingAwareTemplate template,
            Config data, BlockRegistry registry
    ) {

        if (!data.hasProperty("blocks")) {
            return null;
        }

        ConfigNumberList blockMap = data.getConfigNumberList("blocks");

        int current = 0;

        for (int x = 0; x <= (template.xSize + 1); x++) {
            for (int y = 0; y <= (template.ySize + 1); y++) {
                for (int z = 0; z <= (template.zSize + 1); z++) {
                    int blockId = Math.toIntExact(blockMap.get(current));

                    DictionaryEntry entry = registry.getEntryByIdAndType(
                            blockId, template.getStructureType()
                    );

                    if (entry != null) {
                        template.setComponentInfo(entry, x, y, z);
                    }

                    current += 1;
                }
            }
        }

        return template;
    }

    private BlockPos readDimensionsFrom(Config data) {
        if (!data.hasProperty("dimensions")) {
            return null;
        }

        ConfigNumberList dimensions = data.getConfigNumberList("dimensions");

        return new BlockPos(
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
            blockType = overhaulBlockTypeFromConfig(block, structureType);
        }

        if (blockType == null) {
            return null;
        }

        return DynamicItemDictionary.getOrCreateEntry(
                blockType, name
        );
    }

    private DictionaryEntryType overhaulBlockTypeFromConfig(Config block, StructureType type) {
        if (getFlag(block, "casing")) {
            if (getFlag(block, "casingEdge")) {
                return DictionaryEntryType.OVERHAUL_CASING_FRAME;
            }
            return DictionaryEntryType.OVERHAUL_CASING_FACE;
        }

        if (type == StructureType.OverhaulTurbine) {
            if (hasAnyOfFlags(block, "bearing", "inlet", "outlet")) {
                return DictionaryEntryType.OVERHAUL_CASING_FACE;
            }
            if (getFlag(block, "shaft")) {
                return DictionaryEntryType.OVERHAUL_TURBINE_SHAFT;
            }
            if (block.hasProperty("blade")) {
                return DictionaryEntryType.OVERHAUL_TURBINE_BLADE;
            }
            if (block.hasProperty("coil") || hasAnyOfFlags(block, "connector")) {
                return DictionaryEntryType.OVERHAUL_TURBINE_COIL;
            }
        }

        return DictionaryEntryType.OVERHAUL_COMPONENT;
    }

    private boolean getFlag(Config config, String name) {
        return config.hasProperty(name) && config.getBoolean(name);
    }

    private boolean hasAnyOfFlags(Config config, String... flags) {
        for (String prop : flags) {
            if (getFlag(config, prop)) {
                return true;
            }
        }

        return false;
    }
}
