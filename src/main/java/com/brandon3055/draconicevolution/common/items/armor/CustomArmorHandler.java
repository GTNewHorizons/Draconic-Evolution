package com.brandon3055.draconicevolution.common.items.armor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.network.ShieldHitPacket;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;
import com.brandon3055.draconicevolution.common.utils.LogHelper;
import com.brandon3055.draconicevolution.integration.ModHelper;

import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 13/11/2014.
 */
public final class CustomArmorHandler {

    private static final UUID WALK_SPEED_UUID = UUID.fromString("0ea6ce8e-d2e8-11e5-ab30-625662870761");
    private static final DamageSource ADMIN_KILL = new DamageSource("administrative.kill")
            .setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute();

    // Collection that will be populated with client and server players when playing on integrated server
    private final Set<EntityPlayer> playersWithFlight = Collections.synchronizedSet(new HashSet<>());
    private boolean clientPlayerHasHillStep;

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            clientPlayerHasHillStep = false;
        }
        if (!playersWithFlight.isEmpty()) {
            synchronized (playersWithFlight) {
                playersWithFlight.removeIf(p -> event.world == p.worldObj);
            }
        }
    }

    public void onServerStopped() {
        this.playersWithFlight.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer player)) return;

        final ArmorSummary summary = ArmorSummary.get(player);

        float hitAmount = ModHelper.applyModDamageAdjustments(summary, event);

        if (applyArmorDamageBlocking(event, summary)) {
            return;
        }
        if (summary == null || summary.protectionPoints <= 0 || event.source == ADMIN_KILL) {
            return;
        }
        event.setCanceled(true);
        // Ensure that the /kill command can still kill the player
        if (hitAmount == Float.MAX_VALUE && !event.source.damageType.equals(ADMIN_KILL.damageType)) {
            player.attackEntityFrom(ADMIN_KILL, Float.MAX_VALUE);
            return;
        }
        if ((float) player.hurtResistantTime > (float) player.maxHurtResistantTime / 2.0F) return;

        float newEntropy = Math.min(summary.entropy + 1 + (hitAmount / 20), 100F);

        // Divide the damage between the armor peaces based on how many of the protection points each peace has
        float totalAbsorbed = 0;
        float remainingPoints = 0;
        for (int i = 0; i < summary.allocation.length; i++) {
            if (summary.allocation[i] == 0) continue;
            ItemStack armorPeace = summary.armorStacks[i];

            float dmgShear = summary.allocation[i] / summary.protectionPoints;
            float dmg = dmgShear * hitAmount;

            float absorbed = Math.min(dmg, summary.allocation[i]);
            totalAbsorbed += absorbed;
            summary.allocation[i] -= absorbed;
            remainingPoints += summary.allocation[i];
            ItemNBTHelper.setFloat(armorPeace, "ProtectionPoints", summary.allocation[i]);
            ItemNBTHelper.setFloat(armorPeace, "ShieldEntropy", newEntropy);
        }

        if (summary.protectionPoints > 0) {
            DraconicEvolution.network.sendToAllAround(
                    new ShieldHitPacket(player, summary.protectionPoints / summary.maxProtectionPoints),
                    new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64));
            player.worldObj.playSoundEffect(
                    player.posX + 0.5D,
                    player.posY + 0.5D,
                    player.posZ + 0.5D,
                    "draconicevolution:shieldStrike",
                    0.9F,
                    player.worldObj.rand.nextFloat() * 0.1F + 1.055F);
        }

        if (remainingPoints > 0) {
            player.hurtResistantTime = 20;
        } else if (hitAmount - totalAbsorbed > 0) {
            player.attackEntityFrom(event.source, hitAmount - totalAbsorbed);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer player)) return;

        final ArmorSummary summary = ArmorSummary.get(player);

        if (summary == null || event.source == ADMIN_KILL) return;

        if (summary.protectionPoints > 500) {
            event.setCanceled(true);
            event.entityLiving.setHealth(10);
            if (DraconicEvolution.debug) {
                LogHelper.warn(
                        "Something is trying to bypass the draconic shield. [Culprit: {Damage Type="
                                + event.source.damageType
                                + ", Damage Class="
                                + event.source
                                + "]");
            }
            return;
        }

        if (!summary.hasDraconic) return;

        int[] charge = new int[summary.armorStacks.length];
        int totalCharge = 0;
        for (int i = 0; i < summary.armorStacks.length; i++) {
            if (summary.armorStacks[i] != null) {
                charge[i] = ((IEnergyContainerItem) summary.armorStacks[i].getItem())
                        .getEnergyStored(summary.armorStacks[i]);
                totalCharge += charge[i];
            }
        }

        if (totalCharge < BalanceConfigHandler.draconicArmorBaseStorage) return;

        for (int i = 0; i < summary.armorStacks.length; i++) {
            if (summary.armorStacks[i] != null) {
                ((IEnergyContainerItem) summary.armorStacks[i].getItem()).extractEnergy(
                        summary.armorStacks[i],
                        (int) ((charge[i] / (double) totalCharge) * BalanceConfigHandler.draconicArmorBaseStorage),
                        false);
            }
        }

        player.addChatComponentMessage(
                new ChatComponentTranslation("msg.de.shieldDepleted.txt")
                        .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_RED)));
        event.setCanceled(true);
        player.setHealth(1);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final EntityPlayer player = event.player;
            final ArmorSummary summary = ArmorSummary.get(player);
            tickShield(summary, player);
            tickArmorEffects(summary, player);
        }
    }

    private void tickShield(ArmorSummary summary, EntityPlayer player) {
        if (summary == null || summary.maxProtectionPoints - summary.protectionPoints < 0.01 && summary.entropy == 0
                || player.worldObj.isRemote) {
            return;
        }

        float totalPointsToAdd = Math
                .min(summary.maxProtectionPoints - summary.protectionPoints, summary.maxProtectionPoints / 60F);
        totalPointsToAdd *= (1F - (summary.entropy / 100F));
        totalPointsToAdd = Math.min(
                totalPointsToAdd,
                summary.totalEnergyStored
                        / (summary.hasDraconic ? BalanceConfigHandler.draconicArmorEnergyPerProtectionPoint
                                : BalanceConfigHandler.wyvernArmorEnergyPerProtectionPoint));
        if (totalPointsToAdd < 0F) totalPointsToAdd = 0F;

        summary.entropy -= (summary.meanRecoveryPoints * 0.01F);
        if (summary.entropy < 0) summary.entropy = 0;

        for (int i = 0; i < summary.armorStacks.length; i++) {
            ItemStack stack = summary.armorStacks[i];
            if (stack == null || summary.totalEnergyStored <= 0) continue;
            float maxForPeace = ((ICustomArmor) stack.getItem()).getProtectionPoints(stack);
            int energyAmount = ((ICustomArmor) summary.armorStacks[i].getItem()).getEnergyPerProtectionPoint();
            ((IEnergyContainerItem) stack.getItem()).extractEnergy(
                    stack,
                    (int) (((double) summary.energyAllocation[i] / (double) summary.totalEnergyStored)
                            * (totalPointsToAdd * energyAmount)),
                    false);
            float pointsForPeace = (summary.pointsDown[i]
                    / Math.max(1, summary.maxProtectionPoints - summary.protectionPoints)) * totalPointsToAdd;
            summary.allocation[i] += pointsForPeace;
            if (summary.allocation[i] > maxForPeace || maxForPeace - summary.allocation[i] < 0.1F)
                summary.allocation[i] = maxForPeace;
            ItemNBTHelper.setFloat(stack, "ProtectionPoints", summary.allocation[i]);
            if (player.hurtResistantTime <= 0) ItemNBTHelper.setFloat(stack, "ShieldEntropy", summary.entropy);
        }
    }

    private void tickArmorEffects(ArmorSummary summary, EntityPlayer player) {

        // region/*----------------- Flight ------------------*/
        if (ConfigHandler.enableFlight) {
            if (summary != null && summary.flight[0]) {
                playersWithFlight.add(player);
                player.capabilities.allowFlying = true;
                if (summary.flight[1]) {
                    player.capabilities.isFlying = true;
                }

                if (player.worldObj.isRemote) {
                    player.capabilities.setFlySpeed(0.05F + (0.05F * summary.flightSpeedModifier));
                }

                if (!player.onGround && player.capabilities.isFlying
                        && player.motionY != 0
                        && summary.flightVModifier > 0) {
                    // float percentIncrease = summary.flightVModifier;

                    if (DraconicEvolution.proxy.isSpaceDown() && !DraconicEvolution.proxy.isShiftDown()) {
                        // LogHelper.info(player.motionY);
                        player.motionY = 0.225F * summary.flightVModifier;
                    }

                    if (DraconicEvolution.proxy.isShiftDown() && !DraconicEvolution.proxy.isSpaceDown()) {
                        player.motionY = -0.225F * summary.flightVModifier;
                    }
                }

                if (summary.flight[2] && player.moveForward == 0
                        && player.moveStrafing == 0
                        && player.capabilities.isFlying) {
                    player.motionX *= 0.5;
                    player.motionZ *= 0.5;
                }

            } else if (playersWithFlight.remove(player)) {
                if (!player.capabilities.isCreativeMode) {
                    player.capabilities.allowFlying = false;
                    player.capabilities.isFlying = false;
                    if (!player.worldObj.isRemote) {
                        player.sendPlayerAbilities();
                    }
                }
                if (player.worldObj.isRemote) {
                    player.capabilities.setFlySpeed(0.05F);
                }
            }
        }
        // endregion

        // region/*---------------- Swiftness ----------------*/

        IAttribute speedAttr = SharedMonsterAttributes.movementSpeed;
        if (summary != null && summary.speedModifier > 0) {
            double value = summary.speedModifier;
            if (player.getEntityAttribute(speedAttr).getModifier(WALK_SPEED_UUID) == null) {
                player.getEntityAttribute(speedAttr).applyModifier(
                        new AttributeModifier(WALK_SPEED_UUID, speedAttr.getAttributeUnlocalizedName(), value, 1));
            } else if (player.getEntityAttribute(speedAttr).getModifier(WALK_SPEED_UUID).getAmount() != value) {
                player.getEntityAttribute(speedAttr)
                        .removeModifier(player.getEntityAttribute(speedAttr).getModifier(WALK_SPEED_UUID));
                player.getEntityAttribute(speedAttr).applyModifier(
                        new AttributeModifier(WALK_SPEED_UUID, speedAttr.getAttributeUnlocalizedName(), value, 1));
            }

            if (!player.onGround && player.ridingEntity == null) {
                player.jumpMovementFactor = 0.02F + (0.02F * summary.speedModifier);
            }
        } else if (player.getEntityAttribute(speedAttr).getModifier(WALK_SPEED_UUID) != null) {
            player.getEntityAttribute(speedAttr)
                    .removeModifier(player.getEntityAttribute(speedAttr).getModifier(WALK_SPEED_UUID));
        }

        // endregion

        // region/*---------------- HillStep -----------------*/
        if (player.worldObj.isRemote) {
            handleHillStepClient(summary, player);
        }
        // endregion
    }

    @SideOnly(value = Side.CLIENT)
    private void handleHillStepClient(ArmorSummary summary, EntityPlayer player) {
        if (player == Minecraft.getMinecraft().thePlayer) {
            final boolean hasHillStep = player.stepHeight >= 1f && clientPlayerHasHillStep;
            final boolean shouldHaveHillStep = summary != null && summary.hasHillStep;

            if (shouldHaveHillStep && !hasHillStep) {
                clientPlayerHasHillStep = true;
                player.stepHeight = 1f;
            }

            if (!shouldHaveHillStep && hasHillStep) {
                clientPlayerHasHillStep = false;
                player.stepHeight = 0.5F;
            }
        }
    }

    private static boolean applyArmorDamageBlocking(LivingAttackEvent event, ArmorSummary summary) {
        if (summary == null) return false;

        if (event.source.isFireDamage() && summary.fireResistance >= 1F) {
            event.setCanceled(true);
            event.entityLiving.extinguish();
            return true;
        }

        if (event.source.damageType.equals("fall") && summary.jumpModifier > 0F) {
            if (event.ammount < summary.jumpModifier * 5F) event.setCanceled(true);
            return true;
        }

        if ((event.source.damageType.equals("inWall") || event.source.damageType.equals("drown"))
                && summary.armorStacks[3] != null) {
            if (event.ammount <= 2f) event.setCanceled(true);
            return true;
        }

        return false;
    }
}
