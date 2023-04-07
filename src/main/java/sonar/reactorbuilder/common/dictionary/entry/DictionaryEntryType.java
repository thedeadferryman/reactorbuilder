package sonar.reactorbuilder.common.dictionary.entry;

public enum DictionaryEntryType {
    UNDERHAUL_COMPONENT(0, false, "underhaul component"),
    UNDERHAUL_FUEL(1, false, "underhaul fuel"),
    UNDERHAUL_CASING_SOLID(2, false, "underhaul solid casing"),
    UNDERHAUL_CASING_GLASS(3, false, "underhaul glass casing"),
    UNDERHAUL_EDGES(4, false, "edges"), //not to be registered, only used within ReactorBuilderTE

    OVERHAUL_COMPONENT(11, true, "overhaul component"),
    OVERHAUL_FUEL(12, true, "overhaul fuel"),
    OVERHAUL_LIQUID_FUEL(13, true, "overhaul liquid fuel"),
    OVERHAUL_CASING_FRAME(14, true, "casing block suitable for frame"),
    OVERHAUL_CASING_FACE(15, true, "casing block not suitable for frame"),
    OVERHAUL_TURBINE_BLADE(16, true, "turbine blade"),
    OVERHAUL_TURBINE_SHAFT(17, true, "turbine shaft"),
    OVERHAUL_TURBINE_COIL(18, true, "turbine coil"),

    IRRADIATOR_RECIPE(20, true, "irradiator recipes");

    public byte id;
    public boolean isOverhaul;
    public String logName;

    DictionaryEntryType(int id, boolean isOverhaul, String s) {
        this.id = (byte) id;
        this.isOverhaul = isOverhaul;
        this.logName = s;
    }

    public static DictionaryEntryType getType(byte id) {
        for (DictionaryEntryType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

    public byte getID() {
        return id;
    }
}
