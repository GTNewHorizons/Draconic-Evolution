package com.brandon3055.draconicevolution.common.tileentities;

import static com.brandon3055.draconicevolution.DraconicEvolution.isAutomagyLoaded;
import static com.brandon3055.draconicevolution.DraconicEvolution.isEioLoaded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeHooks;

import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;

import crazypants.enderio.machine.obelisk.xp.TileExperienceObelisk;
import crazypants.enderio.xp.ExperienceContainer;
import tuhljin.automagy.tiles.TileEntityJarXP;

/**
 * Created by Brandon on 27/06/2014.
 */
public class TileDissEnchanter extends TileEntity implements ISidedInventory {

    ItemStack[] items = new ItemStack[3];
    public boolean isValidRecipe = false;
    public int dissenchantCost = 0;
    public int timer = 0;
    public float bookPower = 0f;

    // ==============================================LOGIC=======================================================//

    @Override
    public void updateEntity() {
        timer++;
        if (worldObj.isRemote && isValidRecipe && worldObj.rand.nextFloat() > 0.5f) {
            worldObj.spawnParticle(
                    "enchantmenttable",
                    xCoord + 0.3 + (worldObj.rand.nextDouble() * 0.4),
                    yCoord + 0.7 + (worldObj.rand.nextDouble() * 0.5),
                    zCoord + 0.3 + (worldObj.rand.nextDouble() * 0.4),
                    0D,
                    0.3D,
                    0D);
        }
    }

    public void onInventoryChanged() {
        boolean flag = true;
        if (items[2] != null || items[1] == null || items[0] == null) {
            flag = false;
            dissenchantCost = 0;
        } else {
            if (items[1].getItem() != Items.book || EnchantmentHelper.getEnchantments(items[0]).size() == 0)
                flag = false;
        }
        if (items[0] != null) dissenchantCost = ItemNBTHelper.getInteger(items[0], "RepairCost", 0);
        // if (flag) dissenchantCost = ItemNBTHelper.getInteger(items[0], "RepairCost", 0);
        // else dissenchantCost = 0;
        isValidRecipe = flag;

        if (!this.worldObj.isRemote) {
            int j;
            bookPower = 0;

            for (j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    if ((j != 0 || k != 0) && this.worldObj.isAirBlock(this.xCoord + k, this.yCoord, this.zCoord + j)
                            && this.worldObj.isAirBlock(this.xCoord + k, this.yCoord + 1, this.zCoord + j)) {
                        bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k * 2, yCoord, zCoord + j * 2);
                        bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k * 2, yCoord + 1, zCoord + j * 2);

                        if (k != 0 && j != 0) {
                            bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k * 2, yCoord, zCoord + j);
                            bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k * 2, yCoord + 1, zCoord + j);
                            bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k, yCoord, zCoord + j * 2);
                            bookPower += ForgeHooks.getEnchantPower(worldObj, xCoord + k, yCoord + 1, zCoord + j * 2);
                        }
                    }
                }
            }
        }
        bookPower = bookPower * 2;
        if (bookPower > 40) bookPower = 40;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void buttonClick(EntityPlayer player) {
        if (!isValidRecipe) return;
        if (player != null) {
            if (player.experienceLevel < dissenchantCost && !player.capabilities.isCreativeMode) return;
        } else {
            ArrayList<TileEntityJarXP> jarsToEmpty = new ArrayList<>();
            ArrayList<ExperienceContainer> obelisksToEmpty = new ArrayList<>();
            int xpCost = dissenchantCost <= 16 ? dissenchantCost * (dissenchantCost + 6)
                    : dissenchantCost < 32 ? Math.round(dissenchantCost * (2.5f * dissenchantCost - 40.5f)) + 360
                            : Math.round(dissenchantCost * (4.5f * dissenchantCost - 162.5f)) + 2220;
            int xp;
            absorb: {
                if (isAutomagyLoaded) {
                    CubeIterator iter = new CubeIterator(8);
                    while (iter.hasNext()) {
                        iter.next();
                        if (worldObj.getTileEntity(
                                iter.n + xCoord,
                                iter.l + yCoord,
                                iter.m + zCoord) instanceof TileEntityJarXP jar) {
                            xp = jar.getXP();
                            if (xp >= xpCost) {
                                if (!worldObj.isRemote) {
                                    jarsToEmpty.forEach(j -> j.setXP(0));
                                    jar.setXP(xp - xpCost);
                                }
                                break absorb;
                            }
                            xpCost -= xp;
                            jarsToEmpty.add(jar);
                        }
                    }
                }
                if (isEioLoaded) {
                    CubeIterator iter = new CubeIterator(8);
                    while (iter.hasNext()) {
                        iter.next();
                        if (worldObj.getTileEntity(
                                iter.n + xCoord,
                                iter.l + yCoord,
                                iter.m + zCoord) instanceof TileExperienceObelisk obelisk) {
                            ExperienceContainer cont = obelisk.getContainer();
                            xp = cont.getExperienceTotal();
                            if (xp >= xpCost) {
                                if (!worldObj.isRemote) {
                                    jarsToEmpty.forEach(j -> j.setXP(0));
                                    obelisksToEmpty.forEach(o -> o.drain(null, Integer.MAX_VALUE, true));
                                    cont.drain(null, Integer.MAX_VALUE, true);
                                    cont.addExperience(Math.max(0, xp - xpCost));
                                }
                                break absorb;
                            }
                            xpCost -= xp;
                            obelisksToEmpty.add(cont);
                        }
                    }
                }
                return;
            }
        }
        ItemStack input = items[0];
        ItemStack enchantedBook = new ItemStack(Items.enchanted_book);
        Map enchants = EnchantmentHelper.getEnchantments(input);
        Object id = null;
        Object level = null;
        String tagName = "ench";
        if (input.getItem() == Items.enchanted_book) tagName = "StoredEnchantments";
        Iterator i = enchants.keySet().iterator();
        if (i.hasNext()) id = i.next();
        if (id == null) return;
        level = enchants.get(id);
        if (level == null) return;
        Map enchant = new HashMap();
        enchant.put(id, level);
        EnchantmentHelper.setEnchantments(enchant, enchantedBook);
        setInventorySlotContents(2, enchantedBook);
        NBTTagCompound compound = input.getTagCompound();
        NBTTagList list = new NBTTagList();
        if (compound.getTag(tagName) instanceof NBTTagList) list = (NBTTagList) compound.getTag(tagName);
        if (list == null) return;
        for (int j = 0; j < list.tagCount(); j++) {
            if (list.getCompoundTagAt(j).getShort("id") == id.hashCode()) {
                list.removeTag(j);
            }
        }

        compound.removeTag(tagName);
        if (list.tagCount() > 0) input.setTagInfo(tagName, list);
        if (input.getItem() == Items.enchanted_book && list.tagCount() == 0) setInventorySlotContents(0, null);
        if (!player.capabilities.isCreativeMode) player.addExperienceLevel(-dissenchantCost);
        if (items[0] != null && ItemNBTHelper.getInteger(items[0], "RepairCost", 0) > 0) ItemNBTHelper.setInteger(
                items[0],
                "RepairCost",
                ItemNBTHelper.getInteger(items[0], "RepairCost", 0)
                        - Math.min(2, ItemNBTHelper.getInteger(items[0], "RepairCost", 0)));
        if (!player.capabilities.isCreativeMode) decrStackSize(1, 1);
        int maxDamage = items[0] != null ? items[0].getMaxDamage() : 0;
        float damageF = (40f - bookPower) / 100f;
        int damage = (int) (damageF * (float) maxDamage);
        int damageResult = items[0] != null ? items[0].getItemDamage() + damage : 0;

        if (!player.capabilities.isCreativeMode && damageResult > maxDamage && maxDamage > 0) {
            setInventorySlotContents(0, null);
        } else if (!player.capabilities.isCreativeMode && maxDamage > 0 && items[0] != null) {
            items[0].setItemDamage(damageResult);
        }
        onInventoryChanged();
    }

    // ==========================================SYNCHRONIZATION==============================================e====//

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        this.writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    // ==============================================INVENTORY====================================================//

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return items[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int count) {
        ItemStack itemstack = getStackInSlot(i);

        if (itemstack != null) {
            if (itemstack.stackSize <= count) {
                setInventorySlotContents(i, null);
            } else {
                itemstack = itemstack.splitStack(count);
                if (itemstack.stackSize == 0) {
                    setInventorySlotContents(i, null);
                }
            }
        }
        return itemstack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        ItemStack item = getStackInSlot(i);
        if (item != null) setInventorySlotContents(i, null);
        return item;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        items[i] = itemstack;
        if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
            itemstack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.4) < 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return i == 1 ? itemstack.getItem().equals(Items.book)
                : i == 0 ? EnchantmentHelper.getEnchantments(itemstack).size() != 0 : false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return var1 == 0 ? new int[] { 0, 2 } : new int[] { 1 };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return true;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return slot != 0 || EnchantmentHelper.getEnchantments(item).size() == 0;
    }

    // ===========================================================================================================//

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagCompound[] tag = new NBTTagCompound[items.length];

        for (int i = 0; i < items.length; i++) {
            tag[i] = new NBTTagCompound();

            if (items[i] != null) {
                tag[i] = items[i].writeToNBT(tag[i]);
            }

            compound.setTag("Item" + i, tag[i]);
        }
        compound.setBoolean("IsValid", isValidRecipe);
        compound.setInteger("Cost", dissenchantCost);
        compound.setFloat("ServivalChance", bookPower);
        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        NBTTagCompound[] tag = new NBTTagCompound[items.length];

        for (int i = 0; i < items.length; i++) {
            tag[i] = compound.getCompoundTag("Item" + i);
            items[i] = ItemStack.loadItemStackFromNBT(tag[i]);
        }
        isValidRecipe = compound.getBoolean("IsValid");
        dissenchantCost = compound.getInteger("Cost");
        bookPower = compound.getFloat("ServivalChance");
        super.readFromNBT(compound);
    }

    public static final class CubeIterator {

        public int range = 0;

        // wow, it's just like electron orbitals. and the spin is the sign. how beautiful
        public int n = 0;
        public int l = 0;
        public int m = 0;

        CubeIterator(int range) {
            this.range = range;
        }

        // maybe i could put this in next() and make it an Iterator<Boolean>?
        public boolean hasNext() {
            return -n < range || -l < range || -m < range;
        }

        public void next() {
            // this shit looks like the decompile of an obfuscated assembly but i assure you it is hand written
            m = -m;
            if (m < 0) return;
            l = -l;
            if (l < 0) return;
            n = -n;
            if (n < 0) return;
            if (l >= n || m > n) {
                if (m >= l) {
                    if (n <= l) {
                        if (m > n) {
                            n ^= m;
                            m ^= n;
                            n ^= m;
                            if (l > m) {
                                ++m;
                                return;
                            }
                            m = 0;
                            ++l;
                            return;
                        }
                        l = 0;
                        m = 0;
                        ++n;
                        return;
                    }
                    n ^= l;
                    l ^= n;
                    n ^= l;
                    return;
                }
                if (n < m) {
                    n ^= m;
                    m ^= n;
                    n ^= m;
                    return;
                }
                m ^= l;
                l ^= m;
                m ^= l;
                return;
            }
            if (l > m) {
                m ^= l;
                l ^= m;
                m ^= l;
                return;
            }
            n ^= l;
            l ^= n;
            n ^= l;
            return;
        } // i have just found out that Java has a `when` statement, but primitive pattern matching is preview
          // and the syntax sucks (case boolean b when a>6)
          // i genuinely would rather have written this bytecode by bytecode but here we are
    }

}
