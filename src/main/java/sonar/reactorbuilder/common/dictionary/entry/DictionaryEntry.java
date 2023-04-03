package sonar.reactorbuilder.common.dictionary.entry;

import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import sonar.reactorbuilder.ReactorBuilder;
import sonar.reactorbuilder.common.dictionary.GlobalDictionary;

import javax.annotation.Nullable;
import java.util.List;

public abstract class DictionaryEntry {

    public int globalID;
    public String globalName;
    public DictionaryEntryType entryType;
    public boolean ignoreMeta = false;

    public DictionaryEntry(int id, String globalName, DictionaryEntryType entryType) {
        this.globalID = id;
        this.globalName = globalName;
        this.entryType = entryType;
    }

    ///to write safely, we only need to write the refString and itemStack, as these should persist across world saves.
    @Nullable
    public static DictionaryEntry readFromNBTSafely(NBTTagCompound compound) {
        if (compound.hasKey("type")) {
            DictionaryEntryType type = DictionaryEntryType.getType(compound.getByte("type"));

            if (type == null) {
                return null;
            }

            if (type.isOverhaul == ReactorBuilder.isOverhaul()) {
                return loadOrCreateEntry(compound);
            }
        }
        return null;
    }

    private static DictionaryEntry loadOrCreateEntry(NBTTagCompound compound) {
        String globalName = compound.getString("gName");

        DictionaryEntry found = GlobalDictionary.getComponentInfo(globalName);

        if (found == null && compound.getBoolean("isItem")) {
            return ItemEntry.readFromNBT(compound);
        }

        return found;
    }

    public static NBTTagCompound writeToNBTSafely(NBTTagCompound compound, @Nullable DictionaryEntry component) {
        if (component != null) {
            component.writeToNBT(compound);
        }

        return compound;
    }

    public static ItemEntry makeEdgeComponent(ItemStack itemStack) {
        return new ItemEntry(-1, "edge", DictionaryEntryType.UNDERHAUL_EDGES, Lists.newArrayList(itemStack.copy()));
    }

    public static String toStringList(List<DictionaryEntry> entries) {
        StringBuilder builder = new StringBuilder();
        for (DictionaryEntry entry : entries) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(entry.getDisplayName());
        }
        return builder.toString();
    }

    public abstract String getDisplayName();

    public ItemStack getItemStack() {
        return ((ItemEntry) this).getDefaultItemStack();
    }

    public IBlockState getBlockState() {
        return ((ItemEntry) this).getDefaultBlockState();
    }

    public FluidStack getFluidStack() {
        return ((FluidEntry) this).getDefaultFluidStack();
    }

    public boolean canPlaceComponentAtPos(World world, BlockPos pos) {
        return false;
    }

    public boolean isMatchingComponentAtPos(World world, BlockPos pos) {
        return false;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setString("gName", globalName);
        compound.setByte("type", entryType.getID());

        return compound;
    }

    public void ignoreMeta() {
        ignoreMeta = true;
    }

    @Override
    public int hashCode() {
        return globalID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DictionaryEntry) {
            DictionaryEntry info = (DictionaryEntry) obj;
            return info.globalID == globalID && info.entryType == entryType && info.globalName.equals(globalName);
        }
        return super.equals(obj);
    }

}
