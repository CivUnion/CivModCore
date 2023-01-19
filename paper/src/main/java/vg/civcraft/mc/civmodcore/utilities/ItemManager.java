package vg.civcraft.mc.civmodcore.utilities;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import oshi.util.tuples.Pair;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemManager {
	private final CivModCorePlugin plugin;
	protected final Map<NamespacedKey, ItemStack> customItems = new HashMap<>();

	public static ItemManager getInstance() {
		return CivModCorePlugin.getInstance().getItemManager();
	}

	public static void register(NamespacedKey key, ItemStack item){
		getInstance().registerItem(key, item);
	}

	public static void unregister(NamespacedKey key){
		getInstance().unregisterItem(key);
	}

	public ItemManager(CivModCorePlugin plugin){
		this.plugin = plugin;
		Set<Material> mats = Arrays.stream(Material.values()).filter(Material::isItem).collect(Collectors.toSet());
		for(Material mat : mats){
			this.registerItem(mat.getKey(), new ItemStack(mat));
		}
	}

	public ImmutableList<Pair<NamespacedKey, ItemStack>> getItems(){
		return this.customItems.entrySet().stream()
			.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
			.collect(ImmutableList.toImmutableList());
	}

	public ImmutableList<Pair<NamespacedKey, ItemStack>> getItemsByPlugin(Plugin plugin){
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
		if(key == null){
			this.plugin.getLogger().warning("Attempted to register item, 'key' was null");
			return;
		}

		this.unregisterItem(key);

		if(!ItemUtils.isValidItemIgnoringAmount(item)){
			this.plugin.getLogger().warning("Attempted to register item, 'item' was invalid");
			return;
		}

		ItemStack clonedItem = item.clone();
		clonedItem.setAmount(1);


		customItems.put(key, clonedItem);
		this.plugin.getLogger().info(String.format("Registered item with key '%s'", key.asString()));
	}

	public void unregisterItem(NamespacedKey key){
		if(key == null){
			this.plugin.getLogger().warning("Attempted to unregister item, 'key' was null");
			return;
		}

		customItems.remove(key);
	}

}
