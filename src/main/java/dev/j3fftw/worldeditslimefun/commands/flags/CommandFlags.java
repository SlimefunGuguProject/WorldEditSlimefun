package dev.j3fftw.worldeditslimefun.commands.flags;

import co.aikar.commands.BukkitCommandCompletionContext;
import dev.j3fftw.worldeditslimefun.utils.Utils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandFlags {
    public static final Map<String, CommandFlag<?>> FLAG_TYPES = Map.of(
            "--energy", new EnergyFlag(),
            "--inputs", new InputsFlag()
    );

    public static List<CommandFlag<?>> getFlags(List<String> args) {
        List<CommandFlag<?>> flags = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (FLAG_TYPES.containsKey(arg)) {
                CommandFlag<?> flag = CommandFlags.getFlag(arg, args.get(i + 1));
                if (flag != null) {
                    flags.add(flag);
                }
            }
        }
        return flags;
    }

    public static CommandFlag<?> getFlag(String type, String value) {
        CommandFlag<?> flag = FLAG_TYPES.get(type);
        if (flag != null) {
            return flag.ofValue(value);
        }
        return null;
    }

    public static class EnergyFlag extends CommandFlag<Boolean> {
        @Override
        public void apply(SlimefunItem sfItem, Block block) {
            BlockStorage.addBlockInfo(block, "energy-charge", String.valueOf(Integer.MAX_VALUE), false);
        }

        @Override
        public boolean canApply(SlimefunItem sfItem) {
            return this.value && sfItem instanceof EnergyNetComponent component && component.isChargeable();
        }

        @Override
        public Collection<String> getTabSuggestions(BukkitCommandCompletionContext context) {
            return List.of("true", "false");
        }

        @Override
        public EnergyFlag ofValue(String value) {
            return (EnergyFlag) new EnergyFlag().setValue(Boolean.parseBoolean(value));
        }
    }

    public static class InputsFlag extends CommandFlag<List<ItemStack>> {

        @Override
        public void apply(SlimefunItem sfItem, Block block) {
            BlockMenu menu = BlockStorage.getInventory(block);
            int[] slots = menu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.INSERT);
            for (ItemStack input : this.value) {
                if (menu.pushItem(input, slots) != null) {
                    break;
                }
            }
        }

        @Override
        public boolean canApply(SlimefunItem sfItem) {
            return this.value != null && !this.value.isEmpty() && Slimefun.getRegistry().getMenuPresets().containsKey(sfItem.getId());
        }

        @Override
        public Collection<String> getTabSuggestions(BukkitCommandCompletionContext context) {
            String input = context.getInput();
            if (input.isEmpty()) {
                return List.of("[");
            }

            char lastChar = input.charAt(input.length() - 1);
            if (lastChar == ']') {
                return Collections.emptyList();
            }

            List<String> inputs = new ArrayList<>();
            if (lastChar == ',') {
                inputs.addAll(generateBaseInputs(input, context.getPlayer().getWorld()));
            } else {
                String current;
                if (input.contains(",")) {
                    current = input.substring(input.lastIndexOf(",") + 1);
                } else {
                    current = input.substring(input.indexOf('[') + 1);
                }
                current = current.toUpperCase(Locale.ROOT);

                if (Utils.SLIMEFUN_ITEMS.contains(current) || Utils.MATERIALS.containsKey(current)) {
                    inputs.add(input + "]");
                    inputs.addAll(generateBaseInputs(input + ",", context.getPlayer().getWorld()));
                    return inputs;
                }

                String base = input.substring(0, input.length() - current.length());
                for (String slimefunItem : Utils.SLIMEFUN_ITEMS) {
                    if (slimefunItem.startsWith(current)) {
                        inputs.add(base + slimefunItem);
                    }
                }

                World world = context.getPlayer().getWorld();
                for (Material material : Utils.MATERIALS.values()) {
                    String name = material.name();
                    if (material.isEnabledByFeature(world) && name.startsWith(current)) {
                        inputs.add(base + name);
                    }
                }
            }

            return inputs;
        }

        private Collection<String> generateBaseInputs(String input, World world) {
            List<String> inputs = new ArrayList<>();
            for (String slimefunItem : Utils.SLIMEFUN_ITEMS) {
                inputs.add(input + slimefunItem + ",");
            }

            for (Material material : Utils.MATERIALS.values()) {
                if (material.isEnabledByFeature(world)) {
                    inputs.add(input + material.name() + ",");
                }
            }
            return inputs;
        }

        @Override
        public InputsFlag ofValue(String value) {
            String[] segments = value.substring(1, value.length() - 1).split(",");
            List<ItemStack> inputs = new ArrayList<>();
            for (String input : segments) {
                input = input.toUpperCase(Locale.ROOT);
                SlimefunItem slimefunItem = SlimefunItem.getById(input);
                Material material = Utils.MATERIALS.get(input);
                if (slimefunItem != null) {
                    inputs.add(new CustomItemStack(slimefunItem.getItem(), slimefunItem.getItem().getMaxStackSize()));
                } else if (material != null) {
                    inputs.add(new ItemStack(material, material.getMaxStackSize()));
                }
            }
            return (InputsFlag) new InputsFlag().setValue(inputs);
        }
    }
}
