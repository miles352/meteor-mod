package com.stash.hunt.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.stash.hunt.Addon;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static com.stash.hunt.Utils.positionInDirection;
import static com.stash.hunt.Utils.setPressed;

public class GrimEfly extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> bounce = sgGeneral.add(new BoolSetting.Builder()
        .name("Bounce")
        .description("Automatically does bounce efly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> lockPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Lock Pitch")
        .description("Whether to lock your pitch when bounce is enabled.")
        .defaultValue(true)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("Pitch")
        .description("The pitch to set when bounce is enabled.")
        .defaultValue(90.0)
        .visible(() -> bounce.get() && lockPitch.get())
        .build()
    );

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("Lock Yaw")
        .description("Whether to lock your yaw when bounce is enabled.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
        .name("Yaw")
        .description("The yaw to set when bounce is enabled.")
        .defaultValue(0.0)
        .visible(() -> bounce.get() && lockYaw.get())
        .build()
    );

    private final Setting<Boolean> highwayObstaclePasser = sgGeneral.add(new BoolSetting.Builder()
        .name("Highway Obstacle Passer")
        .description("Uses baritone to pass obstacles. Make sure to set the YAW lock as it uses this value to calculate the block to go to.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("Distance")
        .description("The distance to set the baritone goal for path realignment.")
        .defaultValue(10.0)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Integer> targetY = sgGeneral.add(new IntSetting.Builder()
        .name("Y Level")
        .description("The Y level to bounce at.")
        .defaultValue(120)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Boolean> assumeHighwayDirs = sgGeneral.add(new BoolSetting.Builder()
        .name("Lock To Highway Directions")
        .description("Aligns you on the highways. Only works for the main 8 highway directions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> paused = sgGeneral.add(new BoolSetting.Builder()
        .name("paused")
        .description("paused")
        .defaultValue(false)
        .visible(() -> false)
        .build()
    );

    public GrimEfly() {
        super(
            Addon.CATEGORY,
            "Grim-Efly",
            "Vanilla efly using a chestplate so that elytra does not use durability. (Requires elytra in hotbar)"
        );
    }

    private boolean startSprinting;
    private boolean startForwards;

    @Override
    public void onActivate()
    {
        if (mc.player == null) return;
        startSprinting = mc.player.isSprinting();
        startForwards = Input.isPressed(mc.options.forwardKey);
        paused.set(false);

        if (bounce.get())
        {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
        }
    }

    @Override
    public void onDeactivate()
    {
        if (mc.player == null) return;

        if (bounce.get())
        {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
        }

        mc.player.setSprinting(startSprinting);
        setPressed(mc.options.forwardKey, startForwards);
    }


    @EventHandler
    private void onTick(TickEvent.Pre event)
    {
        if (mc.player == null) return;

        setPressed(mc.options.forwardKey, true);
        mc.player.setSprinting(true);

        if (bounce.get())
        {
            // if still pathing, wait for that to complete
            if (highwayObstaclePasser.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() != null)
            {
                paused.set(true);
                return;
            }

            if (highwayObstaclePasser.get() && (mc.player.getY() < targetY.get()
                || mc.player.getY() > targetY.get() + 2
                || mc.player.horizontalCollision))
            {
                paused.set(true);
                double targetYaw = lockYaw.get() ? yaw.get() : mc.player.getYaw();
                Vec3d pos;
                if (assumeHighwayDirs.get())
                {
                    Vec3d playerPos = normalizedPositionOnAxis(mc.player.getPos()).multiply(mc.player.getPos().multiply(1,0,1).length());
                    pos = positionInDirection(playerPos, targetYaw, distance.get());
                }
                else
                {
                    // TODO: Make this better
                    pos = positionInDirection(mc.player.getPos(), targetYaw, distance.get());
                }
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(new BlockPos((int)pos.x, targetY.get(), (int)pos.z)));
            }
            else
            {
                // keep jumping
                paused.set(false);
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                };

                // set yaw and pitch
                if (lockYaw.get())
                {
                    mc.player.setYaw(yaw.get().floatValue());
                }
                if (lockPitch.get())
                {
                    mc.player.setPitch(pitch.get().floatValue());
                }
            }
        }

        if (!paused.get())
        {
            doGrimEflyStuff();

        }
    }

    private void doGrimEflyStuff()
    {
        FindItemResult itemResult = InvUtils.findInHotbar(Items.ELYTRA);
        if (!itemResult.found()) return;

        swapToItem(itemResult.slot());

        sendStartFlyingPacket();

        swapToItem(itemResult.slot());
        // send packet
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    Vec3d normalizedPositionOnAxis(Vec3d pos) {
        double angle = -Math.atan2(pos.x, pos.z);
        double angleDeg = Math.toDegrees(angle);

        double ordinalAngle = Math.round(angleDeg / 45.0f) * 45;

        return positionInDirection(new Vec3d(0,0,0), ordinalAngle, 1);
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event)
    {
        List<Identifier> armorEquipSounds = List.of(
            Identifier.of("minecraft:item.armor.equip_generic"),
            Identifier.of("minecraft:item.armor.equip_netherite"),
            Identifier.of("minecraft:item.armor.equip_diamond"),
            Identifier.of("minecraft:item.armor.equip_gold"),
            Identifier.of("minecraft:item.armor.equip_iron"),
            Identifier.of("minecraft:item.armor.equip_chain"),
            Identifier.of("minecraft:item.armor.equip_leather"),
            Identifier.of("minecraft:item.elytra.flying")
        );
        for (Identifier identifier : armorEquipSounds) {
            if (identifier.equals(event.sound.getId())) {
                event.cancel();
                break;
            }
        }
    }

//    private void debugInventoryState() {
//        if (mc.player == null || mc.player.getInventory() == null) return;
//
//        ChatUtils.info("Debugging inventory state...");
//        for (int i = 0; i < mc.player.getInventory().size(); i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            String itemName = stack.isEmpty() ? "Empty" : stack.getItem().getName().getString();
//            int count = stack.isEmpty() ? 0 : stack.getCount();
//            ChatUtils.info(String.format("Slot %d: %s x%d", i, itemName, count));
//        }
//        ChatUtils.info("Finished debugging inventory state.");
//    }

    // 38 is the meteor mapping for chestplate
    // serverside uses default mappings: https://imgs.search.brave.com/cyvAxjIhLweeF1qeRXpC_8ESRlImhUmMGWbV_n2to_A/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9jNGsz/LmdpdGh1Yi5pby93/aWtpLnZnL2ltYWdl/cy8xLzEzL0ludmVu/dG9yeS1zbG90cy5w/bmc
    private void swapToItem(int slot) {
        ItemStack chestItem = mc.player.getInventory().getStack(38);
        ItemStack hotbarSwapItem = mc.player.getInventory().getStack(slot);

        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        changedSlots.put(6, hotbarSwapItem);
        changedSlots.put(slot + 36, chestItem);

        sendSwapPacket(changedSlots, slot);
    }

    private void sendStartFlyingPacket() {
        if (mc.player == null) return;
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
            mc.player,
            ClientCommandC2SPacket.Mode.START_FALL_FLYING
        ));
    }

    private void sendSwapPacket(Int2ObjectMap<ItemStack> changedSlots, int buttonNum) {
        int syncId  = mc.player.currentScreenHandler.syncId;
        int stateId = mc.player.currentScreenHandler.getRevision();

        // "slotNum = 6"
        // "buttonNum = 0"
        // "SlotActionType.SWAP"
        // "changedSlots" as built above
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            syncId,
            stateId,
            6,                 // slotNum
            buttonNum,                 // buttonNum: the slot number thats being swapped //TODO: Try numbers 9-39, might be possible to do it without needing in hotbar
            SlotActionType.SWAP,
            new ItemStack(Items.AIR), // clickedItem
            changedSlots
        ));

        // Also forcibly set local inventory so we "see" it right away
        // The changedSlots map has the final item arrangement
//        changedSlots.forEach((slotId, stack) -> {
//            if (slotId == 6) {
//                mc.player.getInventory().setStack(6, stack.copy());
//            }
//            else if (slotId == 36) {
//                // local index 0 => hotbar
//                mc.player.getInventory().setStack(0, stack.copy());
//            }
//            // If you had more variations, you'd match them here
//        });
    }
}
