package sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul;

import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.reactors.TemplateType;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.OverhaulFissionTemplate;
import sonar.reactorbuilder.util.TernaryOperator;
import sonar.reactorbuilder.util.Translate;

import javax.annotation.Nullable;
import java.util.Map;

public class CasingAwareOverhaulFissionSFR extends AbstractTemplate {
    public CasingAwareOverhaulFissionSFR(String fileName, int xSize, int ySize, int zSize) {
        super(fileName, xSize, ySize, zSize);
    }

    public CasingAwareOverhaulFissionSFR() {
        super();
    }

    @Override
    protected DictionaryEntry[][][] initializeBlockData() {
        return new DictionaryEntry[xSize + 2][ySize + 2][zSize + 2];
    }

    @Override
    public TemplateType getTemplateType() {
        return TemplateType.CASINGAWARE_OVERHAUL_SFR;
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
    public int getBuildPasses() {
        return 2;
    }

    @Override
    public String[] getBuildPassNames() {
        return new String[]{
                Translate.PASS_PLACING_COMPONENTS.t(),
                Translate.PASS_PLACING_CASINGS.t()
        };
    }

    @Override
    public int getBuildPassTotal(int buildPass) {
        switch (buildPass) {
            case 0:
                return totalSolidComponents;
            case 1:
                return totalFaceCasing + totalFrameCasing;
            default:
                return 0;
        }
    }

    @Override
    public boolean canPlaceThisPass(int buildPass, int x, int y, int z, DictionaryEntry info) {
        switch (buildPass) {
            case 0:
                return info.entryType == DictionaryEntryType.OVERHAUL_COMPONENT;
            case 1:
                return info.entryType == DictionaryEntryType.OVERHAUL_CASING_FRAME || info.entryType == DictionaryEntryType.OVERHAUL_CASING_FACE;
        }

        return false;
    }

    @Override
    public DictionaryEntry getDefaultSolidCasing() {
        return OverhaulFissionTemplate.casingSolid;
    }

    @Override
    public DictionaryEntry getDefaultGlassCasing() {
        return OverhaulFissionTemplate.casingGlass;
    }

    @Override
    public void getStats(Map<String, String> statsMap) {
        statsMap.put(Translate.TEMPLATE_FILE_NAME.t(), fileName);
        statsMap.put(Translate.TEMPLATE_REACTOR_TYPE.t(), getTemplateType().fileType);
        statsMap.put(Translate.TEMPLATE_DIMENSIONS.t(), xSize + " x " + ySize + " x " + zSize);

        statsMap.put(Translate.TEMPLATE_COMPONENTS.t(), String.valueOf(totalSolidComponents));
        statsMap.put(Translate.CASING_CONFIG.t(), String.valueOf(totalFrameCasing + totalFaceCasing + totalEdges));
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
}
