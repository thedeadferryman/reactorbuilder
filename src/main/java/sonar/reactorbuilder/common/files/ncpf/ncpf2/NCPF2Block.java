package sonar.reactorbuilder.common.files.ncpf.ncpf2;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.oredict.OreDictionary;
import sonar.reactorbuilder.common.dictionary.DynamicItemDictionary;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;
import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntryType;
import sonar.reactorbuilder.util.MCUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NCPF2Block {
    private final static String OSFR_CASING = "nuclearcraft:overhaul_sfr:casing";
    private final static String OSFR_PORT = "nuclearcraft:overhaul_sfr:port";
    private final static List<String> OSFR_COMPONENTS = Lists.newArrayList(
            "nuclearcraft:overhaul_sfr:fuel_cell",
            "nuclearcraft:overhaul_sfr:heat_sink",
            "nuclearcraft:overhaul_sfr:moderator",
            "nuclearcraft:overhaul_sfr:reflector",
            "nuclearcraft:overhaul_sfr:neutron_shield"
    );

    private final static String OTURB_CASING = "nuclearcraft:overhaul_turbine:casing";
    private final static List<String> OTURB_CASINGS_MISC = Lists.newArrayList(
            "nuclearcraft:overhaul_turbine:inlet",
            "nuclearcraft:overhaul_turbine:outlet",
            "nuclearcraft:overhaul_turbine:bearing"
    );
    private final static String OTURB_BLADE = "nuclearcraft:overhaul_turbine:blade";
    private final static String OTURB_SHAFT = "nuclearcraft:overhaul_turbine:shaft";
    private final static String OTURB_COIL = "nuclearcraft:overhaul_turbine:coil";
    private final static String OTURB_CONNECTOR = "nuclearcraft:overhaul_turbine:connector";


    final public String identifier;
    final public Map<String, JsonObject> modules;
    final public MCUtils.ItemLocator locator;
    final public String oredict;

    private static NCPF2Block getBlockBase(JsonObject raw, String idSuffix) {
        if (raw.has("type") && Objects.equals(raw.get("type").getAsString(), "oredict")) {
            String oredict = raw.get("oredict").getAsString();

            String identifier = "$$oredict$$:" + oredict;

            if (idSuffix != null) {
                identifier += ":" + idSuffix;
            }

            return new NCPF2Block(
                    identifier,
                    new HashMap<>(),
                    null,
                    oredict
            );
        }

        if (raw.has("name")) {
            String name = raw.get("name").getAsString();

            MCUtils.ItemLocator locator = MCUtils.parseItemLocator(name);

            if (raw.has("metadata")
                    && raw.get("metadata").isJsonPrimitive()
                    && raw.get("metadata").getAsJsonPrimitive().isNumber()
            ) {
                locator = locator.withMeta(raw.get("metadata").getAsInt());
            }

            String identifier = locator.toString();

            if (idSuffix != null) {
                identifier += ":" + idSuffix;
            }

            return new NCPF2Block(identifier, new HashMap<>(), locator, null);
        }

        throw new IllegalArgumentException("failed to parse block");
    }

    public static NCPF2Block parse(JsonObject raw) {
        Map<String, JsonObject> modules = new HashMap<>();

        JsonObject rawModules = raw.getAsJsonObject("modules");

        for (Map.Entry<String, JsonElement> entry : rawModules.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                modules.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }

        String suffix = null;

        if (raw.has("blockstate") && raw.get("blockstate").isJsonObject()) {
            JsonObject blockstate = raw.getAsJsonObject("blockstate");
            if (JsonUtils.isBoolean(blockstate, "active") && blockstate.get("active").getAsBoolean()) {
                suffix = "$$active$$";
            }
        }

        NCPF2Block block = getBlockBase(raw, suffix);

        block.modules.putAll(modules);

        return block;
    }

    public NCPF2Block(
            String identifier,
            Map<String, JsonObject> modules,
            MCUtils.ItemLocator locator,
            String oredict
    ) {
        this.identifier = identifier;
        this.modules = modules;
        this.locator = locator;
        this.oredict = oredict;
    }

    public NCPF2Block merge(NCPF2Block otherBlock) {
        if (!Objects.equals(identifier, otherBlock.identifier)) {
            throw new IllegalArgumentException("non-equal identifiers");
        }

        Map<String, JsonObject> newModules = new HashMap<>(modules);

        newModules.putAll(otherBlock.modules);

        return new NCPF2Block(identifier, newModules, locator, oredict);
    }

    public DictionaryEntry buildDictionaryEntry() {
        if (locator != null) {
            return DynamicItemDictionary.getOrCreateEntry(getDictionaryType(), locator);
        } else if (oredict != null) {
            List<ItemStack> stacks = OreDictionary.getOres(oredict);

            return DynamicItemDictionary.getOrCreateEntry(getDictionaryType(), identifier, stacks, false);
        } else {
            return null;
        }
    }

    public DictionaryEntryType getDictionaryType() {
        JsonObject module = getModule(OSFR_CASING);

        if (module != null) return mapCasingType(module);


        if (hasModule(OSFR_PORT)) return DictionaryEntryType.OVERHAUL_CASING_FACE;
        if (hasAnyModule(OSFR_COMPONENTS)) return DictionaryEntryType.OVERHAUL_COMPONENT;


        module = getModule(OTURB_CASING);

        if (module != null) return mapCasingType(module);

        if (hasAnyModule(OTURB_CASINGS_MISC)) return DictionaryEntryType.OVERHAUL_CASING_FACE;
        if (hasModule(OTURB_BLADE)) return DictionaryEntryType.OVERHAUL_TURBINE_BLADE;
        if (hasModule(OTURB_SHAFT)) return DictionaryEntryType.OVERHAUL_TURBINE_SHAFT;
        if (hasModule(OTURB_COIL)) return DictionaryEntryType.OVERHAUL_TURBINE_COIL;
        if (hasModule(OTURB_CONNECTOR)) return DictionaryEntryType.OVERHAUL_TURBINE_COIL;

        return DictionaryEntryType.INVALID;
    }

    private JsonObject getModule(String module) {
        return modules.get(module);
    }

    private boolean hasModule(String module) {
        return getModule(module) != null;
    }

    private boolean hasAnyModule(List<String> modules) {
        for (String module : modules) {
            if (hasModule(module)) return true;
        }

        return false;
    }

    private DictionaryEntryType mapCasingType(JsonObject module) {
        if (module.get("edge").getAsBoolean()) {
            return DictionaryEntryType.OVERHAUL_CASING_FRAME;
        } else {
            return DictionaryEntryType.OVERHAUL_CASING_FACE;
        }
    }

}
