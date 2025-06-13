package sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul;

import sonar.reactorbuilder.ReactorBuilder;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.files.ncpf.StructureType;
import sonar.reactorbuilder.common.reactors.TemplateType;
import sonar.reactorbuilder.common.reactors.templates.OverhaulFissionTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.CasingAwareTemplate;
import sonar.reactorbuilder.util.Translate;

import java.util.Map;

public class CasingAwareOverhaulFissionSFR extends CasingAwareTemplate {
    public CasingAwareOverhaulFissionSFR(String fileName, int xSize, int ySize, int zSize) {
        super(fileName, xSize, ySize, zSize, StructureType.OverhaulSFR);
    }

    public CasingAwareOverhaulFissionSFR() {
        super();
    }


    @Override
    public TemplateType getTemplateType() {
        return TemplateType.CASINGAWARE_OVERHAUL_SFR;
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
    public void updateAdditionalInfo() {
        super.updateAdditionalInfo();
        totalFaceCasing = 0;
        totalFrameCasing = 0;
        totalSolidComponents = 0;

        forEachPos((x, y, z) -> {
            DictionaryEntry entry = blocks[x][y][z];

            ReactorBuilder.logger.warn("at pos " + x + ":" + y + ":" + z + " = " + entry);

            if (entry != null) {
                switch (entry.entryType) {
                    case OVERHAUL_CASING_FRAME:
                        totalFrameCasing += 1;
                        break;
                    case OVERHAUL_CASING_FACE:
                        totalFaceCasing += 1;
                        break;
                    case OVERHAUL_COMPONENT:
                        totalSolidComponents += 1;
                }
            }

            return true;
        });
    }

    @Override
    public void getStats(Map<String, String> statsMap) {
        statsMap.put(Translate.TEMPLATE_FILE_NAME.t(), fileName);
        statsMap.put(Translate.TEMPLATE_REACTOR_TYPE.t(), getTemplateType().fileType);
        statsMap.put(Translate.TEMPLATE_DIMENSIONS.t(), xSize + " x " + ySize + " x " + zSize);

        statsMap.put(Translate.TEMPLATE_COMPONENTS.t(), String.valueOf(totalSolidComponents));
        statsMap.put(Translate.CASING_CONFIG.t(), String.valueOf(totalFrameCasing + totalFaceCasing + totalEdges));
    }
}
