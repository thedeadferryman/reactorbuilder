package sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul;

import sonar.reactorbuilder.common.dictionary.DynamicItemDictionary;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.common.files.ncpf.StructureType;
import sonar.reactorbuilder.common.reactors.TemplateType;
import sonar.reactorbuilder.common.reactors.templates.casingaware.CasingAwareTemplate;
import sonar.reactorbuilder.util.Translate;

import java.util.Map;

public class CasingAwareOverhaulTurbine extends CasingAwareTemplate {
    private static final DictionaryEntry CASING_SOLID = DynamicItemDictionary.getOrCreateEntry(
            DictionaryEntryType.OVERHAUL_CASING_FRAME,
            "nuclearcraft:turbine_casing"
    );
    private static final DictionaryEntry CASING_GLASS = DynamicItemDictionary.getOrCreateEntry(
            DictionaryEntryType.OVERHAUL_CASING_FACE,
            "nuclearcraft:turbine_glass"
    );

    private int totalCoils;
    private int totalShafts;
    private int totalBlades;
    private int shaftDiameter;

    public CasingAwareOverhaulTurbine(String fileName, int xSize, int ySize, int zSize) {
        super(fileName, xSize, ySize, zSize, StructureType.OverhaulTurbine);
    }

    public CasingAwareOverhaulTurbine() {
        super();
    }

    @Override
    public TemplateType getTemplateType() {
        return TemplateType.CASINGAWARE_OVERHAUL_TURBINE;
    }

    @Override
    public int getBuildPasses() {
        return 4;
    }

    @Override
    public String[] getBuildPassNames() {
        return new String[]{
                Translate.PASS_PLACING_CASINGS.t(),
                Translate.PASS_PLACING_COILS.t(),
                Translate.PASS_PLACING_SHAFTS.t(),
                Translate.PASS_PLACING_BLADES.t(),
        };
    }

    @Override
    public int getBuildPassTotal(int buildPass) {
        switch (buildPass) {
            case 0:
                return totalFaceCasing + totalFrameCasing;
            case 1:
                return totalCoils;
            case 2:
                return totalShafts;
            case 3:
                return totalBlades;
            default:
                return 0;
        }
    }

    @Override
    public boolean canPlaceThisPass(int buildPass, int x, int y, int z, DictionaryEntry info) {
        switch (buildPass) {
            case 0:
                return info.entryType == DictionaryEntryType.OVERHAUL_CASING_FACE || info.entryType == DictionaryEntryType.OVERHAUL_CASING_FRAME;
            case 1:
                return info.entryType == DictionaryEntryType.OVERHAUL_TURBINE_COIL;
            case 2:
                return info.entryType == DictionaryEntryType.OVERHAUL_TURBINE_SHAFT;
            case 3:
                return info.entryType == DictionaryEntryType.OVERHAUL_TURBINE_BLADE;
            default:
                return false;
        }
    }

    @Override
    public DictionaryEntry getDefaultSolidCasing() {
        return CASING_SOLID;
    }

    @Override
    public DictionaryEntry getDefaultGlassCasing() {
        return CASING_GLASS;
    }

    @Override
    public void updateAdditionalInfo() {
        super.updateAdditionalInfo();

        totalFrameCasing = 0;
        totalFaceCasing = 0;
        totalBlades = 0;
        totalCoils = 0;
        totalShafts = 0;

        forEachPos((x, y, z) -> {
            DictionaryEntry entry = blocks[x][y][z];

            if (entry != null) {
                switch (entry.entryType) {
                    case OVERHAUL_CASING_FRAME:
                        totalFrameCasing += 1;
                        break;
                    case OVERHAUL_CASING_FACE:
                        totalFaceCasing += 1;
                        break;
                    case OVERHAUL_TURBINE_BLADE:
                        totalBlades += 1;
                        break;
                    case OVERHAUL_TURBINE_COIL:
                        totalCoils += 1;
                        break;
                    case OVERHAUL_TURBINE_SHAFT:
                        totalShafts += 1;
                }
            }
            return true;
        });

        int centerPos = xSize / 2 + (xSize % 2);

        for (int y = 0; y < (ySize + 2); y++) {
            DictionaryEntry entry = blocks[centerPos][y][1];
            if (entry != null && entry.entryType == DictionaryEntryType.OVERHAUL_TURBINE_SHAFT) {
                shaftDiameter += 1;
            }
        }

        totalSolidComponents = totalBlades + totalShafts;
    }

    @Override
    public void getStats(Map<String, String> statsMap) {
        statsMap.put(Translate.TEMPLATE_FILE_NAME.t(), fileName);
        statsMap.put(Translate.TEMPLATE_REACTOR_TYPE.t(), getTemplateType().fileType);

        statsMap.put(Translate.TEMPLATE_SHAFT_DIAMETER.t(), Integer.toString(shaftDiameter));
        statsMap.put(Translate.TEMPLATE_TURBINE_DIAMETER.t(), Integer.toString(xSize));
        statsMap.put(Translate.TEMPLATE_TURBINE_LENGTH.t(), Integer.toString(zSize));

        statsMap.put(Translate.TEMPLATE_NUM_COILS.t(), Integer.toString(totalCoils));
    }
}
