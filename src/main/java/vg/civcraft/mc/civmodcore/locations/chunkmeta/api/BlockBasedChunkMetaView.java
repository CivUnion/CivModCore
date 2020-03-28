package vg.civcraft.mc.civmodcore.locations.chunkmeta.api;

import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import vg.civcraft.mc.civmodcore.locations.chunkmeta.ChunkMeta;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.GlobalChunkMetaManager;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.XZWCoord;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.block.BlockBasedChunkMeta;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.block.BlockBasedStorageEngine;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.block.BlockDataObject;
import vg.civcraft.mc.civmodcore.locations.chunkmeta.block.fallback.SingleBlockTracker;

/**
 * API view for block based chunk metas, which adds convenience methods for
 * directly editing individual block data
 *
 * @param <T> BlockBasedChunkMeta subclass
 * @param <D> BlockDataObject subclass
 */
public class BlockBasedChunkMetaView<T extends BlockBasedChunkMeta<D, S>, D extends BlockDataObject<D>, S extends BlockBasedStorageEngine<D>>
		extends ChunkMetaView<T> {

	private Supplier<T> chunkProducer;
	private S storageEngine;
	private SingleBlockTracker<D> singleBlockTracker;

	BlockBasedChunkMetaView(JavaPlugin plugin, short pluginID, GlobalChunkMetaManager globalManager,
			Supplier<T> chunkProducer, boolean loadAll) {
		super(plugin, pluginID, globalManager, loadAll);
		this.chunkProducer = chunkProducer;
		if (loadAll) {
			loadAll();
		}
		singleBlockTracker = new SingleBlockTracker<>();
	}

	private void loadAll() {
		for (XZWCoord coord : storageEngine.getAllDataChunks()) {
			getOrCreateChunkMeta(globalManager.getWorldByInternalID(pluginID), coord.getX(), coord.getZ());
		}
	}

	/**
	 * Gets the data at the given location
	 * 
	 * @param block Block to get data for
	 * @return Data tied to the given block or null if no data exists there
	 */
	public D get(Block block) {
		T chunk = super.getChunkMeta(block.getLocation());
		if (chunk == null) {
			return null;
		}
		validateY(block.getY());
		return chunk.get(block);
	}

	/**
	 * Gets the data at the given location
	 * 
	 * @param location Location to get data for
	 * @return Data at the given location or null if no data exists there
	 */
	public D get(Location location) {
		validateY(location.getBlockY());
		T chunk = super.getChunkMeta(location);
		if (chunk == null) {
			short worldID = globalManager.getInternalWorldId(location.getWorld());
			D data = singleBlockTracker.getBlock(location, worldID);
			if (data == null) {
				return storageEngine.getForLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ(),
						worldID, pluginID);
			}
			return data;
		}
		return chunk.get(location);

	}

	@SuppressWarnings("unchecked")
	private T getOrCreateChunkMeta(World world, int x, int z) {
		return super.computeIfAbsent(world, x, z, (Supplier<ChunkMeta<?>>) (Supplier<?>) chunkProducer);
	}

	/**
	 * Inserts data into the cache
	 * 
	 * @param data Data to insert
	 */
	public void put(D data) {
		if (data == null) {
			throw new IllegalArgumentException("Data to insert can not be null");
		}
		Location loc = data.getLocation();
		validateY(loc.getBlockY());
		T chunk = super.getChunkMeta(loc.getWorld(), loc.getChunk().getX(), loc.getChunk().getZ());
		if (chunk != null) {
			chunk.put(loc, data);
			return;
		}
		singleBlockTracker.putBlock(data, globalManager.getInternalWorldId(loc.getWorld()));

	}

	/**
	 * Attempts to remove data tied to the given block from the cache, if any exists
	 * 
	 * @param block Block to remove data for
	 * @return Data removed, null if nothing was removed
	 */
	public D remove(Block block) {
		return remove(block.getLocation());
	}

	/**
	 * Removes the given data from the cache. Data must actually be in the cache
	 * 
	 * @param data Data to remove
	 */
	public void remove(D data) {
		D removed = remove(data.getLocation());
		if (removed != data) {
			throw new IllegalStateException("Removed data non-identical to the one supposed to be removed");
		}
	}

	/**
	 * Attempts to remove data at the given location from the cache, if any exists
	 * 
	 * @param location Location to remove data from
	 * @return Data removed, null if nothing was removed
	 */
	public D remove(Location location) {
		validateY(location.getBlockY());
		T chunk = super.getChunkMeta(location);
		if (chunk == null) {
			return singleBlockTracker.removeBlock(location, globalManager.getInternalWorldId(location.getWorld()));
		}
		return chunk.remove(location);
	}

	private static void validateY(int y) {
		if (y < 0) {
			throw new IllegalArgumentException("Y-level of data may not be less than 0");
		}
		if (y > 255) {
			throw new IllegalArgumentException("Y-level of data may not be more than 255");
		}
	}

	@Override
	public void postLoad(ChunkMeta<?> c) {
		@SuppressWarnings("unchecked")
		T chunk = (T) c;
		for (D data : singleBlockTracker.getAllForChunkAndRemove(chunk.getChunkCoord())) {
			chunk.put(data.getLocation().getBlockX(), data.getLocation().getBlockY(), data.getLocation().getBlockZ(),
					data, true);
		}
	}
	
	@Override
	public void disable() {
		for(D data : singleBlockTracker.getAll()) {
			storageEngine.persist(data, globalManager.getInternalWorldId(data.getLocation().getWorld()), pluginID);
		}
		super.disable();
	}

}
