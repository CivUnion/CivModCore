package vg.civcraft.mc.civmodcore.utilities.creative;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

public class CreativeClicker extends Clickable {

	private final NamespacedKey key;
	private final ItemStack realItem;

	public CreativeClicker(NamespacedKey key, ItemStack item) {
		super(item);
		this.key = key;
		this.realItem = item.clone();
		ItemUtils.addComponentLore(this.item,
			Component.empty(),
			Component.text(String.format("Namespace: %s", key.namespace())),
			Component.text(String.format("Key: %s", key.value()))
		);
	}

	@Override
	protected void clicked(@NotNull Player clicker) {
	}

	@Override
	protected void onLeftClick(@NotNull Player clicker) {
		super.onLeftClick(clicker);
		ItemMap map = new ItemMap(this.realItem);
		map.addToInventory(clicker.getInventory());
	}

	@Override
	protected void onRightClick(@NotNull Player clicker) {
		super.onRightClick(clicker);
		ItemMap map = new ItemMap(this.realItem);
		map.multiplyContent(this.item.getMaxStackSize());
		map.addToInventory(clicker.getInventory());
	}

	@Override
	protected void onMiddleClick(@NotNull Player clicker) {
		super.onMiddleClick(clicker);
		// TODO: Implement middle click to select number of items to give
	}

	@Override
	protected void onShiftLeftClick(@NotNull Player clicker) {
		this.onLeftClick(clicker);
	}

	@Override
	protected void onShiftRightClick(@NotNull Player clicker) {
		this.onRightClick(clicker);
	}
}
