package sonar.reactorbuilder.common.reactors.templates.casingaware;

import net.minecraft.util.math.BlockPos;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.files.ncpf.StructureType;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.util.TernaryOperator;

import javax.annotation.Nullable;

public abstract class CasingAwareTemplate extends AbstractTemplate {
    private StructureType type;

    public CasingAwareTemplate(String fileName, int xSize, int ySize, int zSize, StructureType type) {
        super(fileName, xSize, ySize, zSize);
        this.type = type;
    }

    public CasingAwareTemplate() {
        super();
    }

    public StructureType getStructureType() {
        return type;
    }

    @Override
    public boolean isCasingAware() {
        return true;
    }

    @Nullable
    @Override
    public DictionaryEntry getComponent(int x, int y, int z) {
        return blocks[x + 1][y + 1][z + 1];
    }

    @Override
    public int getIndexSize() {
        return (xSize + 2) * (ySize + 2) * (zSize + 2);
    }

    @Override
    public BlockPos getRenderSize() {
        return new BlockPos(xSize + 2, ySize + 2, zSize + 2);
    }

    public int getIndexFromInternalPos(int xPos, int yPos, int zPos) {
        return (xPos * (ySize + 2) * (zSize + 2)) + (yPos * (zSize + 2)) + zPos;
    }

    @Override
    public void forEachPos(TernaryOperator<Integer, Integer, Integer, Boolean> handler) {
        for (int x = 0; x < (xSize + 2); x++) {
            for (int y = 0; y < (ySize + 2); y++) {
                for (int z = 0; z < (zSize + 2); z++) {
                    handler.consume(x, y, z);
                }
            }
        }
    }

    @Override
    public void updateAdditionalInfo() {
        required.clear();

        forEachPos((x, y, z) -> {
            DictionaryEntry entry = blocks[x][y][z];

            if (entry != null) {
                required.compute(entry, (k, v) -> v == null ? 1 : v + 1);
            }

            return true;
        });
    }

    @Override
    protected DictionaryEntry[][][] initializeBlockData() {
        return new DictionaryEntry[xSize + 2][ySize + 2][zSize + 2];
    }
}
