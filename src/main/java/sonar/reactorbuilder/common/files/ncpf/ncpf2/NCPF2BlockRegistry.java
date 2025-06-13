package sonar.reactorbuilder.common.files.ncpf.ncpf2;

import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.files.ncpf.BlockRegistry;
import sonar.reactorbuilder.common.files.ncpf.StructureType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NCPF2BlockRegistry {
    public final Map<StructureType, ArrayList<String>> blockOrder = new HashMap<>();
    public final Map<String, NCPF2Block> blocks = new HashMap<>();

    public NCPF2BlockRegistry() {
        for (StructureType type : StructureType.values()) {
            blockOrder.put(type, new ArrayList<>());
        }
    }

    public void add(NCPF2Block block, StructureType type) {
        blocks.merge(block.identifier, block, NCPF2Block::merge);

        if (!blockOrder.get(type).contains(block.identifier)) {
            blockOrder.get(type).add(block.identifier);
        }
    }

    public BlockRegistry build() {
        BlockRegistry registry = new BlockRegistry();

        for (Map.Entry<StructureType, ArrayList<String>> entry : blockOrder.entrySet()) {
            buildList(
                    registry.pickList(entry.getKey()),
                    entry.getValue()
            );
        }

        return registry;
    }

    private void buildList(List<DictionaryEntry> dictList, ArrayList<String> orderList) {
        for (String identifier : orderList) {
            NCPF2Block block = blocks.get(identifier);

            if (block == null) {
                dictList.add(null);
            } else {
                dictList.add(block.buildDictionaryEntry());
            }
        }
    }
}
