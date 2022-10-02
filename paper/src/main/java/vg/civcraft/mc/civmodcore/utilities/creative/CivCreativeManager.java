package vg.civcraft.mc.civmodcore.utilities.creative;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CivCreativeManager {
	private final CivModCorePlugin plugin;
	protected final Map<NamespacedKey, ItemStack> customItems = new HashMap<>();

	public CivCreativeManager(CivModCorePlugin plugin){
		this.plugin = plugin;
	}

	public ImmutableList<Pair<NamespacedKey, ItemStack>> getItems(){
		return this.customItems.entrySet().stream()
			.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
			.collect(ImmutableList.toImmutableList());
	}

	public ImmutableList<Pair<NamespacedKey, ItemStack>> getItemsByNamespace(Plugin plugin){
		if(plugin == null){
			this.plugin.getLogger().warning("Attempted to get custom items of namespace, 'plugin' was null");
			return ImmutableList.of();
		}
		String pluginName = plugin.getName().toLowerCase(Locale.ROOT);
		return this.getItemsByNamespace(pluginName);
	}

	public ImmutableList<Pair<NamespacedKey, ItemStack>> getItemsByNamespace(String namespace){
		if(StringUtils.isEmpty(namespace)){
			this.plugin.getLogger().warning("Attempted to get custom items of namespace, 'namespace' was null or empty");
			return ImmutableList.of();
		}

		return customItems.entrySet().stream()
			.filter((entry) -> entry.getKey().namespace().equalsIgnoreCase(namespace))
			.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
			.collect(ImmutableList.toImmutableList());
	}

	public void registerItem(NamespacedKey key, ItemStack item){
		this.registerItem(key, item, false);
	}

	public void registerItem(NamespacedKey key, ItemStack item, boolean overwrite){
		if(key == null){
			this.plugin.getLogger().warning("Attempted to register item, 'key' was null");
			return;
		}

		if(!ItemUtils.isValidItemIgnoringAmount(item)){
			this.plugin.getLogger().warning("Attempted to register item, 'item' was invalid");
			return;
		}

		if(customItems.containsKey(key) && !overwrite){
			this.plugin.getLogger().warning(String.format("Attempted to register item with key '%s' but it already existed", key.asString()));
			return;
		}

		ItemStack clonedItem = item.clone();
		clonedItem.setAmount(1);

		customItems.put(key, clonedItem);
	}

	public void unregisterItem(NamespacedKey key){
		if(key == null){
			this.plugin.getLogger().warning("Attempted to unregister item, 'key' was null");
			return;
		}

		customItems.remove(key);
	}

}
