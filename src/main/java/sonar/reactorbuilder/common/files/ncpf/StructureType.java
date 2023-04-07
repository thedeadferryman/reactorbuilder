package sonar.reactorbuilder.common.files.ncpf;

public enum StructureType {
    UnderhaulSFR(false),
    OverhaulSFR(true),
    OverhaulMSR(true),
    OverhaulTurbine(true);

    public boolean isOverhaul;

    StructureType(boolean isOverhaul) {
        this.isOverhaul = isOverhaul;
    }
}
