package vg.civcraft.mc.civmodcore.utilities.creative;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.chat.ChatUtils;
import vg.civcraft.mc.civmodcore.chat.dialog.Dialog;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.LClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.components.ComponableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.components.Scrollbar;
import vg.civcraft.mc.civmodcore.inventory.gui.components.SlotPredicates;
import vg.civcraft.mc.civmodcore.inventory.gui.components.StaticDisplaySection;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.utilities.ItemManager;
import vg.civcraft.mc.civmodcore.utilities.MoreMath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CivCreativeMenu {

	public enum ItemFilter {
		NAMESPACE,
		NAME,
		LORE
	}

	private final Player player;
	private final ItemManager creativeManager;

	public CivCreativeMenu(Player player, ItemManager creativeManager){
		this.player = Objects.requireNonNull(player);
		this.creativeManager = Objects.requireNonNull(creativeManager);
	}

	public void showCreativeMenu(@NotNull final Set<Pair<ItemFilter, String>> filters){
		ClickableInventory.forceCloseInventory(this.player);

		Bukkit.getScheduler().runTask(CivModCorePlugin.getInstance(), () -> {
			List<Pair<NamespacedKey, ItemStack>> items = this.creativeManager.getItems();

			ComponableInventory inv = new ComponableInventory("Civ Creative", 6, this.player);
			{
				StaticDisplaySection displaySection = new StaticDisplaySection(18);
				Set<Pair<ItemFilter, String>> tempFilters = filters;
				List<Pair<NamespacedKey, ItemStack>> tempItems = items;
				displaySection.set(new LClickable(Material.HOPPER, "Filter by namespace", ply -> {
					showNamespaceFilter(tempItems, tempFilters);
				}), 0);
				displaySection.set(new LClickable(Material.PAPER, "Filter by name", ply -> {
					new Dialog(this.player, "Filter by name:") {
						@Override
						public void onReply(String[] message) {
							if (message.length != 0) {
								String fullStr = String.join(" ", message);
								Pair<ItemFilter, String> filterPair = new Pair<>(ItemFilter.NAME, fullStr);
								tempFilters.add(filterPair);
							}

							showCreativeMenu(tempFilters);
						}

						@Override
						public List<String> onTabComplete(String lastWord, String[] fullMessage) {
							return new ArrayList<>();
						}
					};
				}), 1);
				displaySection.set(new LClickable(Material.PAPER, "Filter by lore", ply -> {
					new Dialog(this.player, "Filter by lore:") {
						@Override
						public void onReply(String[] message) {
							if (message.length != 0) {
								String fullStr = String.join(" ", message);
								Pair<ItemFilter, String> filterPair = new Pair<>(ItemFilter.LORE, fullStr);
								tempFilters.add(filterPair);
							}

							showCreativeMenu(tempFilters);
						}

						@Override
						public List<String> onTabComplete(String lastWord, String[] fullMessage) {
							return new ArrayList<>();
						}
					};
				}), 2);
				displaySection.set(new LClickable(Material.BARRIER, "Reset filters", (ply) -> {
					if (tempFilters.size() == 0) {
						return;
					}
					this.showCreativeMenu(new HashSet<>());
				}), 8);

				for (int i = 9; i <= 17; i++) {
					displaySection.set(new DecorationStack(Material.GRAY_STAINED_GLASS_PANE, " "), i);
				}

				inv.addComponent(displaySection, SlotPredicates.rectangle(2, 9));
			}

			{
				List<IClickable> clickables = new ArrayList<>();

				List<Pair<NamespacedKey, ItemStack>> filteredItems = new ArrayList<>(items);
				for (Pair<ItemFilter, String> filter : filters) {
					if (filter.getA() == ItemFilter.NAMESPACE) {
						filteredItems = items.stream()
							.filter(itemPair -> itemPair.getA().namespace().equalsIgnoreCase(filter.getB()))
							.collect(Collectors.toList());
					} else if (filter.getA() == ItemFilter.NAME) {
						filteredItems = items.stream()
							.filter(itemPair -> ChatUtils.stringify(itemPair.getB().displayName()).toLowerCase(Locale.ROOT).contains(filter.getB().toLowerCase(Locale.ROOT)))
							.collect(Collectors.toList());
					} else {
						filteredItems = items.stream().filter(itemPair -> {
							List<String> fullLore = ItemUtils.getComponentLore(itemPair.getB()).stream()
								.map(ChatUtils::stringify)
								.collect(Collectors.toList());
							for (String lore : fullLore) {
								if (lore.contains(filter.getB())) {
									return true;
								}
							}
							return false;
						}).collect(Collectors.toList());
					}
				}

				for (Pair<NamespacedKey, ItemStack> entry : filteredItems) {
					clickables.add(new CreativeClicker(entry.getA(), entry.getB().clone()));
				}

				clickables.sort((former, latter) -> {
					String formerName = ChatUtils.stringify(former.getItemStack().displayName());
					String latterName = ChatUtils.stringify(latter.getItemStack().displayName());
					return formerName.compareTo(latterName);
				});

				Scrollbar scrollbar = new Scrollbar(clickables, 36);
				inv.addComponent(scrollbar, SlotPredicates.offsetRectangle(4, 9, 2, 0));
			}

			inv.show();
		});
	}

	private void showNamespaceFilter(List<Pair<NamespacedKey, ItemStack>> items, Set<Pair<ItemFilter, String>> currentFilters){
		ClickableInventory.forceCloseInventory(this.player);

		List<String> uniqueNamespaces = new ArrayList<>();
		for(Pair<NamespacedKey, ItemStack> itemPair : items){
			if(!uniqueNamespaces.contains(itemPair.getA().namespace())){
				uniqueNamespaces.add(itemPair.getA().namespace());
			}
		}

		int rows = (int) Math.max(1, Math.ceil(uniqueNamespaces.size() / 9D));

		int namespaceRows = MoreMath.clamp(rows, 1, 6);
		ComponableInventory inv = new ComponableInventory("Choose a namespace", namespaceRows, this.player);

		List<IClickable> clickables = new ArrayList<>();
		for(String uniqueNamespace : uniqueNamespaces){
			clickables.add(new LClickable(Material.PAPER, uniqueNamespace, (ply) -> {
				currentFilters.add(new Pair<>(ItemFilter.NAMESPACE, uniqueNamespace));
				showCreativeMenu(currentFilters);
			}));
		}

		Scrollbar scrollbar = new Scrollbar(clickables, namespaceRows * 9);
		inv.addComponent(scrollbar, SlotPredicates.rectangle(namespaceRows, 9));
		inv.show();
	}

}
