package sonar.reactorbuilder.common.files.ncpf;

import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import org.apache.commons.io.IOUtils;
import sonar.reactorbuilder.ReactorBuilder;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.files.AbstractFileReader;
import sonar.reactorbuilder.common.files.ncpf.ncpf2.NCPF2Block;
import sonar.reactorbuilder.common.files.ncpf.ncpf2.NCPF2BlockRegistry;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.CasingAwareTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulFissionSFR;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulTurbine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NCPF2Reader extends AbstractFileReader {
    public static final NCPF2Reader INSTANCE = new NCPF2Reader();

    private final Gson gson = new Gson();

    private final Map<String, StructureType> stringMap = new HashMap<>();

    public String error = "";

    NCPF2Reader() {
        stringMap.put("nuclearcraft:underhaul_sfr", StructureType.UnderhaulSFR);
        stringMap.put("nuclearcraft:overhaul_sfr", StructureType.OverhaulSFR);
        stringMap.put("nuclearcraft:overhaul_msr", StructureType.OverhaulMSR);
        stringMap.put("nuclearcraft:overhaul_turbine", StructureType.OverhaulTurbine);
    }

    @Override
    public AbstractTemplate readTemplate(File file) {
        Path path = file.toPath();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject object = JsonUtils.gsonDeserialize(gson, IOUtils.toString(reader), JsonObject.class);

            if (object == null) {
                error = "invalid input file!";
                return null;
            }

            NCPF2BlockRegistry ncpf2registry = new NCPF2BlockRegistry();

            buildBlockRegistry(ncpf2registry, object);


            String v = String.join(", ", ncpf2registry.blockOrder.get(StructureType.OverhaulSFR));

            ReactorBuilder.logger.warn("Order SFR = " + v);

            BlockRegistry registry = ncpf2registry.build();

            if (!object.has("designs") || !object.get("designs").isJsonArray()) {
                error = "no designs in file!";
                return null;
            }

            CasingAwareTemplate template = getFirstSupportedTemplate(object.getAsJsonArray("designs"), registry, file.getName());

            if (template == null) {
                error = "no designs in file!";
                return null;
            }

            return template;
        } catch (IOException | JsonParseException e) {
            error = "invalid input file!";
            ReactorBuilder.logger.error("Error reading reactor file: " + path, e);
            return null;
        } catch (RuntimeException e) {
            error = "failed to parse file!";
            ReactorBuilder.logger.error("Error parsing design: " + path, e);
            return null;
        }
    }

    private void writeComponentInfo(CasingAwareTemplate template, BlockRegistry registry, JsonArray design) {
        StructureType structureType = template.getStructureType();
        List<DictionaryEntry> dict = registry.pickList(structureType);

        for (int x = 0; x <= (template.xSize + 1); x++) {
            for (int y = 0; y <= (template.ySize + 1); y++) {
                for (int z = 0; z <= (template.zSize + 1); z++) {
                    int reference = getPosInDesign(design, x, y, z);

                    if (reference < 0) continue;

                    DictionaryEntry entry = dict.get(reference);

                    template.setComponentInfo(entry, x, y, z);
                }
            }
        }
    }

    private Integer getPosInDesign(JsonArray design, int x, int y, int z) {
        return design.get(x).getAsJsonArray().get(y).getAsJsonArray().get(z).getAsInt();
    }

    private CasingAwareTemplate getFirstSupportedTemplate(JsonArray designs, BlockRegistry registry, String filename) {
        for (JsonElement design : designs) {
            if (design.isJsonObject()) {
                return tryBuildTemplate(design.getAsJsonObject(), registry, filename);
            }
        }

        return null;
    }

    private CasingAwareTemplate tryBuildTemplate(JsonObject design, BlockRegistry registry, String filename) {
        String designType = design.get("type").getAsString();

        CasingAwareTemplate template = null;

        switch (designType) {
            case "nuclearcraft:overhaul_sfr":
                template = tryBuildOSFR(design, filename);
                break;
            case "nuclearcraft:overhaul_turbine":
                template = tryBuildOTurb(design, filename);
                break;
        }

        if (template != null) {
            writeComponentInfo(template, registry, design.getAsJsonArray("design"));
        }

        return template;
    }

    private CasingAwareTemplate tryBuildOSFR(JsonObject design, String filename) {
        JsonArray array = design.getAsJsonArray("dimensions");

        return new CasingAwareOverhaulFissionSFR(filename, array.get(0).getAsInt() - 2, array.get(1).getAsInt() - 2, array.get(2).getAsInt() - 2);
    }

    private CasingAwareTemplate tryBuildOTurb(JsonObject design, String filename) {
        JsonArray array = design.getAsJsonArray("dimensions");

        return new CasingAwareOverhaulTurbine(filename, array.get(0).getAsInt() - 2, array.get(1).getAsInt() - 2, array.get(2).getAsInt() - 2);

    }

    private void buildBlockRegistry(NCPF2BlockRegistry registry, JsonObject object) {
        JsonObject configuration = object.getAsJsonObject("configuration");
        for (Map.Entry<String, StructureType> entry : stringMap.entrySet()) {
            if (configuration.has(entry.getKey())) {
                buildBlockRegistrySection(registry, configuration.getAsJsonObject(entry.getKey()), entry.getValue());
            }
        }

        if (object.has("addons") && object.get("addons").isJsonArray()) {
            JsonArray addons = object.getAsJsonArray("addons");

            for (JsonElement addon : addons) {
                if (addon.isJsonObject()) {
                    buildBlockRegistry(registry, addon.getAsJsonObject());
                }
            }
        }
    }

    private void buildBlockRegistrySection(NCPF2BlockRegistry registry, JsonObject sectionConfig, StructureType type) {
        if (sectionConfig.has("blocks") && sectionConfig.get("blocks").isJsonArray()) {
            JsonArray blocks = sectionConfig.getAsJsonArray("blocks");

            for (JsonElement block : blocks) {
                if (block.isJsonObject()) registry.add(NCPF2Block.parse(block.getAsJsonObject()), type);
            }
        }
    }
}
