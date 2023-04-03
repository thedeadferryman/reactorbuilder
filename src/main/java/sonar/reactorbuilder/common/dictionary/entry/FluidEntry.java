package sonar.reactorbuilder.common.dictionary.entry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class FluidEntry extends DictionaryEntry {

    public FluidStack fluidStack;

    public FluidEntry(int id, String globalName, DictionaryEntryType entryType, FluidStack fluidStack) {
        super(id, globalName, entryType);
        this.fluidStack = fluidStack;
    }

    public FluidStack getDefaultFluidStack() {
        return fluidStack;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("isItem", false);

        return compound;
    }

    @Override
    public String getDisplayName() {
        return fluidStack.getLocalizedName();
    }
}
