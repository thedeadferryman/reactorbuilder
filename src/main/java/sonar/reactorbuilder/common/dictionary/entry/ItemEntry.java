package sonar.reactorbuilder.common.dictionary.entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.IFluidBlock;
import sonar.reactorbuilder.common.dictionary.DynamicItemDictionary;

import java.util.ArrayList;
import java.util.List;

public class ItemEntry extends DictionaryEntry {

    public List<ItemStack> validStacks;

    public ItemEntry(int id, String globalName, DictionaryEntryType entryType, List<ItemStack> itemStack) {
        super(id, globalName, entryType);
        this.validStacks = itemStack;
    }

    public static DictionaryEntry readFromNBT(NBTTagCompound compound) {
        String globalName = compound.getString("gName");
        byte typeId = compound.getByte("type");

        DictionaryEntryType type = DictionaryEntryType.getType(typeId);

        if (globalName == null || type == null) {
            return null;
        }

        boolean ignoreMeta = compound.getBoolean("ignoreMeta");

        NBTTagList stacksNBT = compound.getTagList("validStacks", Constants.NBT.TAG_COMPOUND);

        List<ItemStack> validStacks = new ArrayList<>();

        for (int i = 0; i < stacksNBT.tagCount(); i++) {
            validStacks.add(new ItemStack(stacksNBT.getCompoundTagAt(i)));
        }

        return DynamicItemDictionary.getOrCreateEntry(
                type, globalName, validStacks, ignoreMeta
        );
    }

    public ItemStack getDefaultItemStack() {
        return validStacks.get(0);
    }

    public IBlockState getDefaultBlockState() {
        Block block = Block.getBlockFromItem(getDefaultItemStack().getItem());
        int metadata = getDefaultItemStack().getMetadata();

        if (entryType == DictionaryEntryType.OVERHAUL_TURBINE_BLADE) {
            metadata = 1; //turbine blades are invisible with a metadata of 0
        }

        return block.getStateFromMeta(metadata);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        NBTTagList stacksNBT = new NBTTagList();

        for (ItemStack stack : validStacks) {
            NBTTagCompound stackNBT = new NBTTagCompound();
            stack.writeToNBT(stackNBT);

            stacksNBT.appendTag(stackNBT);
        }

        compound.setBoolean("isItem", true);
        compound.setTag("validStacks", stacksNBT);
        compound.setBoolean("ignoreMeta", ignoreMeta);

        return compound;
    }

    public boolean isMatchingComponentAtPos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        IBlockState place = getBlockState();
        return place != null && state.getBlock() == place.getBlock() && (ignoreMeta || state.getBlock().getMetaFromState(state) == place.getBlock().getMetaFromState(place));
    }

    public boolean canPlaceComponentAtPos(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return world.isAirBlock(pos) || block.isReplaceable(world, pos) || block instanceof BlockLiquid || block instanceof IFluidBlock;
    }

    @Override
    public String getDisplayName() {
        return getDefaultItemStack().getDisplayName();
    }
}
