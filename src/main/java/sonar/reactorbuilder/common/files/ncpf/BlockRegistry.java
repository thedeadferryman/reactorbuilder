package sonar.reactorbuilder.common.files.ncpf;

import sonar.reactorbuilder.common.dictionary.entry.DictionaryEntry;

import java.util.ArrayList;
import java.util.List;

class BlockRegistry {
    public List<DictionaryEntry> underhaulSFR = new ArrayList<>();
    public List<DictionaryEntry> overhaulSFR = new ArrayList<>();
    public List<DictionaryEntry> overhaulMSR = new ArrayList<>();
    public List<DictionaryEntry> overhaulTurbine = new ArrayList<>();

    void merge(BlockRegistry other) {
        underhaulSFR.addAll(other.underhaulSFR);
        overhaulSFR.addAll(other.overhaulSFR);
        overhaulMSR.addAll(other.overhaulMSR);
        overhaulTurbine.addAll(other.overhaulTurbine);
    }

    void add(DictionaryEntry entry, StructureType type) {
        pickList(type).add(entry);
    }

    DictionaryEntry getEntryByIdAndType(int id, StructureType type) {
        return getSafeFromList(
                pickList(type),
                id - 1
        );
    }

    private List<DictionaryEntry> pickList(StructureType type) {
        switch (type) {
            case UnderhaulSFR:
                return underhaulSFR;
            case OverhaulSFR:
                return overhaulSFR;
            case OverhaulMSR:
                return overhaulMSR;
            case OverhaulTurbine:
                return overhaulTurbine;
            default:
                throw new RuntimeException("invalid list type");
        }
    }

    private DictionaryEntry getSafeFromList(List<DictionaryEntry> list, int id) {
        if (id >= list.size() || id < 0) {
            return null;
        }

        return list.get(id);
    }
}
