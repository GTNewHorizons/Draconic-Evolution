package com.brandon3055.draconicevolution.common.items.tools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;

import com.brandon3055.brandonscore.common.utills.InfoHelper;
import com.brandon3055.brandonscore.common.utills.ItemNBTHelper;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.items.ItemDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.utills.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utills.InventoryUtils;
import com.brandon3055.draconicevolution.common.utills.ItemConfigField;
import com.brandon3055.draconicevolution.integration.ModHelper;
import com.gtnewhorizon.gtnhlib.GTNHLib;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by brandon3055 on 9/3/2016.
 */

@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class Magnet extends ItemDE implements IBauble, IConfigurableItem {

    private IIcon draconium;
    private IIcon awakened;

    // Normally, DW index 1 is used for air ticks (breath), but this is irrelevant for items
    // Avoids needing to mixin to EnitityItem#entityInit. Starting value is 300
    // Other indices in use (in vanilla) are 0 (catchall, stuff like on fire)
    // and 10 (the ItemStack held by the EntityItem)
    public static final int DATAWATCHER_MAGNET_INDEX = 1;
    public static final short DATAWATCHER_MAGNET_VALID = 299;

    public static final short SELF_PICKUP_ALWAYS = 0;
    public static final short SELF_PICKUP_DELAY = 1;
    public static final short SELF_PICKUP_NEVER = 2;

    public Magnet() {
        this.setUnlocalizedName("magnet");
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setMaxStackSize(1);
        ModItems.register(this);
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        draconium = iconRegister.registerIcon(References.RESOURCESPREFIX + "magnetWyvern");
        awakened = iconRegister.registerIcon(References.RESOURCESPREFIX + "magnetDraconic");
    }

    @Override
    public IIcon getIconFromDamage(int dmg) {
        return dmg == 0 ? draconium : awakened;
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item item, CreativeTabs p_150895_2_, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return super.getUnlocalizedName(itemStack) + (itemStack.getItemDamage() == 0 ? ".wyvern" : ".draconic");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack, int pass) {
        return isEnabled(stack);
    }

    // This method uses the same algorithm as the magnet from AE2 Fluid Crafting
    // if changes are ever made, they should be made on both
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean hotbar) {
        if (entity.ticksExisted % 5 != 0 || !isEnabled(stack)) {
            return;
        }
        if (IConfigurableItem.ProfileHelper.getBoolean(stack, References.MAGNET_SNEAK, true) && entity.isSneaking()) {
            return;
        }

        final boolean skipPlayerCheck = world.playerEntities.size() < 2;

        if (entity instanceof EntityPlayer player) {
            int range = stack.getItemDamage() == 0 ? 8 : 32;

            List<EntityItem> items = world.getEntitiesWithinAABB(
                    EntityItem.class,
                    AxisAlignedBB.getBoundingBox(
                            entity.posX,
                            entity.posY,
                            entity.posZ,
                            entity.posX,
                            entity.posY,
                            entity.posZ).expand(range, range, range));

            boolean playSound = false;
            boolean didPlayerDrop;
            // On server, entity.posY is feet position
            // On client, it is eye position, and getEyeHeight needs further correction of 1.5 to get feet
            final double feetPos = world.isRemote ? entity.posY - (1.5 + player.getEyeHeight()) : entity.posY;
            boolean doMove;
            DataWatcher dw;
            final short selfPickupStatus = getSelfPickupStatus(stack);

            for (EntityItem item : items) {

                if (item.getEntityItem() == null || ModHelper.isAE2EntityFloatingItem(item)
                        || TileDislocatorInhibitor.isBlockedByInhibitor(world, item)) {
                    continue;
                }

                String name = Item.itemRegistry.getNameForObject(item.getEntityItem().getItem());
                if (ConfigHandler.itemDislocatorBlacklistMap.containsKey(name)
                        && (ConfigHandler.itemDislocatorBlacklistMap.get(name) == -1
                                || ConfigHandler.itemDislocatorBlacklistMap.get(name)
                                        == item.getEntityItem().getItemDamage())) {
                    continue;
                }

                doMove = false;
                dw = item.getDataWatcher();
                // DataWatcher enables Server-Client syncing
                // Server will determine if item is valid to pull, and set the watchable
                // Then both client and server will teleport item, syncing server and render
                if (dw.getWatchableObjectShort(DATAWATCHER_MAGNET_INDEX) == (short) (DATAWATCHER_MAGNET_VALID - selfPickupStatus)) {
                    doMove = true;
                }

                if (!doMove) {
                    if (!world.isRemote) {
                        if (!skipPlayerCheck) {
                            EntityPlayer closestPlayer = world.getClosestPlayerToEntity(item, range);
                            if (closestPlayer != null && closestPlayer != player) {
                                continue;
                            }
                        }
                        if (selfPickupStatus != SELF_PICKUP_ALWAYS) {
                            didPlayerDrop = item.func_145800_j() != null
                                    && item.func_145800_j().equals(player.getCommandSenderName());
                            if (!didPlayerDrop) doMove = true;
                            else if (selfPickupStatus == SELF_PICKUP_DELAY && item.delayBeforeCanPickup <= 0)
                                doMove = true;
                        } else doMove = true;

                        if (doMove) {
                            dw.updateObject(DATAWATCHER_MAGNET_INDEX, (short) (DATAWATCHER_MAGNET_VALID - selfPickupStatus));
                        }
                    }
                    dw.setObjectWatched(DATAWATCHER_MAGNET_INDEX);
                }

                if (doMove) {
                    playSound = true;
                    item.delayBeforeCanPickup = 0;
                    item.motionX = 0;
                    item.motionY = 0;
                    item.motionZ = 0;
                    item.setLocationAndAngles(
                            entity.posX - 0.2 + (world.rand.nextDouble() * 0.4),
                            feetPos + item.height / 2,
                            entity.posZ - 0.2 + (world.rand.nextDouble() * 0.4),
                            0.0f,
                            0.0f);
                }
            }

            if (playSound && !ConfigHandler.itemDislocatorDisableSound) {
                world.playSoundAtEntity(
                        entity,
                        "random.orb",
                        0.1F,
                        0.5F * ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 2F));
            }

            if (!world.isRemote) {
                List<EntityXPOrb> xp = world.getEntitiesWithinAABB(
                        EntityXPOrb.class,
                        AxisAlignedBB.getBoundingBox(
                                entity.posX,
                                entity.posY,
                                entity.posZ,
                                entity.posX,
                                entity.posY,
                                entity.posZ).expand(range, range, range));
                for (EntityXPOrb orb : xp) {
                    if (!skipPlayerCheck) {
                        EntityPlayer closestPlayer = world.getClosestPlayerToEntity(orb, range);
                        if (closestPlayer == null || closestPlayer != player) continue;
                    }
                    if (orb.field_70532_c == 0 && orb.isEntityAlive()) {
                        if (MinecraftForge.EVENT_BUS.post(new PlayerPickupXpEvent(player, orb))) continue;
                        world.playSoundAtEntity(
                                player,
                                "random.orb",
                                0.1F,
                                0.5F * ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.8F));
                        player.onItemPickup(orb, 1);
                        player.addExperience(orb.xpValue);
                        orb.setDead();
                    }
                }
            }
        }
    }

    public static boolean isEnabled(ItemStack itemStack) {
        // For backward compatibility
        if (ItemNBTHelper.verifyExistance(itemStack, "MagnetEnabled")) {
            final NBTTagCompound nbt = itemStack.getTagCompound();
            final boolean enabled = nbt.getBoolean("MagnetEnabled");
            IConfigurableItem.ProfileHelper.setBoolean(itemStack, References.ENABLED, enabled);
            nbt.removeTag("MagnetEnabled");
            return enabled;
        }
        return IConfigurableItem.ProfileHelper.getBoolean(itemStack, References.ENABLED, false);
    }

    public static void toggle(ItemStack itemStack) {
        final boolean enabled = isEnabled(itemStack);
        IConfigurableItem.ProfileHelper.setBoolean(itemStack, References.ENABLED, !enabled);
    }

    public static void setStatus(ItemStack itemStack, boolean status) {
        IConfigurableItem.ProfileHelper.setBoolean(itemStack, References.ENABLED, status);
    }

    public static String getStatusString(ItemStack itemStack) {
        String status;
        if (isEnabled(itemStack)) {
            status = EnumChatFormatting.DARK_GREEN + StatCollector.translateToLocal("info.de.statusActive.txt");
        } else {
            status = EnumChatFormatting.RED + StatCollector.translateToLocal("info.de.statusInactive.txt");
        }
        return StatCollector.translateToLocal("info.de.status.txt") + ": " + status;
    }

    public static short getSelfPickupStatus(ItemStack itemStack) {
        return IConfigurableItem.ProfileHelper.getShort(itemStack, References.ENABLED_SELF_PICKUP, (short) 0);
    }

    public static void toggleSelfPickupStatus(ItemStack itemStack) {
        final short enabledSelfPickup = getSelfPickupStatus(itemStack);
        IConfigurableItem.ProfileHelper.setShort(
                itemStack,
                References.ENABLED_SELF_PICKUP,
                enabledSelfPickup == SELF_PICKUP_NEVER ? (short) 0 : (short) (enabledSelfPickup + 1));
    }

    public static void setSelfPickupStatus(ItemStack itemStack, short status) {
        IConfigurableItem.ProfileHelper.setShort(itemStack, References.ENABLED_SELF_PICKUP, status);
    }

    public static String getSelfPickupStatusString(ItemStack itemStack) {
        String status;
        switch (getSelfPickupStatus(itemStack)) {
            case SELF_PICKUP_DELAY -> status = EnumChatFormatting.YELLOW
                    + StatCollector.translateToLocal("info.de.selfPickupDelay.txt");
            case SELF_PICKUP_NEVER -> status = EnumChatFormatting.RED
                    + StatCollector.translateToLocal("info.de.selfPickupNever.txt");
            default -> status = EnumChatFormatting.DARK_GREEN
                    + StatCollector.translateToLocal("info.de.selfPickupAlways.txt");
        }
        return StatCollector.translateToLocal("info.de.selfPickup.txt") + ": " + status;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            toggle(stack);
        }
        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer p_77624_2_, List list, boolean p_77624_4_) {
        list.add(StatCollector.translateToLocal("info.de.shiftRightClickToActivate.txt"));
        int range = stack.getItemDamage() == 0 ? 8 : 32;
        list.add(
                InfoHelper.HITC() + range
                        + InfoHelper.ITC()
                        + " "
                        + StatCollector.translateToLocal("info.de.blockRange.txt"));
        list.add(getStatusString(stack));
        list.add(getSelfPickupStatusString(stack));
    }

    @Optional.Method(modid = "gtnhlib")
    @SideOnly(Side.CLIENT)
    public static void renderHUDStatusChange() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        java.util.Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);
        magnetOptional.ifPresent(
                itemStack -> GTNHLib.proxy
                        .printMessageAboveHotbar(EnumChatFormatting.GOLD + getStatusString(itemStack), 60, true, true));
    }

    @Optional.Method(modid = "gtnhlib")
    @SideOnly(Side.CLIENT)
    public static void renderHUDSelfPickupStatusChange() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        java.util.Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);
        magnetOptional.ifPresent(
                itemStack -> GTNHLib.proxy.printMessageAboveHotbar(
                        EnumChatFormatting.GOLD + getSelfPickupStatusString(itemStack),
                        60,
                        true,
                        true));
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.UNIVERSAL;
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        World world = player.worldObj;
        onUpdate(itemstack, world, player, 0, false);
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {

    }

    @Override
    @Optional.Method(modid = "Baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {

    }

    @Override
    @Optional.Method(modid = "Baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public List<ItemConfigField> getFields(ItemStack stack, int slot) {
        List<ItemConfigField> fields = new ArrayList<>();
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, References.ENABLED).readFromItem(stack, false));
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, References.MAGNET_SNEAK).readFromItem(stack, true));
        return fields;
    }

    @Override
    public boolean hasProfiles() {
        return false;
    }
}
