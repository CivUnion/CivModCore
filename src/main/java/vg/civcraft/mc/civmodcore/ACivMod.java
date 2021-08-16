package vg.civcraft.mc.civmodcore;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.command.CommandHandler;
import vg.civcraft.mc.civmodcore.command.StandaloneCommandHandler;
import vg.civcraft.mc.civmodcore.serialization.NBTSerializable;
import vg.civcraft.mc.civmodcore.serialization.NBTSerialization;

@SuppressWarnings("deprecation")
public abstract class ACivMod extends JavaPlugin {

	private final List<Class<? extends NBTSerializable>> serializableClasses = Lists.newArrayList();

	@Deprecated
	protected CommandHandler handle = null;
	protected StandaloneCommandHandler newCommandHandler;
	protected boolean useNewCommandHandler = true;

	@Override
	public void onEnable() {
		// Allow plugins to disable the new command handler without breaking other plugins that
		// rely on this being set automatically.
		if (this.useNewCommandHandler) {
			this.newCommandHandler = new StandaloneCommandHandler(this);
		}
		// Self disable when a hard dependency is disabled
		registerListener(new Listener() {
			@EventHandler
			public void onPluginDisable(PluginDisableEvent event) {
				String pluginName = event.getPlugin().getName();
				if (getDescription().getDepend().contains(pluginName)) {
					warning("Plugin [" + pluginName + "] has been disabled, disabling this plugin.");
					disable();
				}
			}
		});
	}
	
	@Override
	public void onDisable() {
		this.useNewCommandHandler = true;
		if (this.newCommandHandler != null) {
			this.newCommandHandler.reset();
			this.newCommandHandler = null;
		}
		this.serializableClasses.forEach(NBTSerialization::unregisterNBTSerializable);
		this.serializableClasses.clear();
		HandlerList.unregisterAll(this);
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
		Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
		Bukkit.getScheduler().cancelTasks(this);
	}

	/**
	 * Registers a listener class with this plugin.
	 *
	 * @param listener The listener class to register.
	 */
	public void registerListener(Listener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Cannot register a listener if it's null, you dummy");
		}
		getServer().getPluginManager().registerEvents(listener, this);
	}

	/**
	 * <p>Registers a serializable.</p>
	 *
	 * <p>Note: This is a tracked single use registration. The given serializable will be de-registered when this
	 * plugin is disabled, thus you should call this within the plugin's onEnable() method.</p>
	 *
	 * @param <T> The type of the serializable.
	 * @param serializable The serializable class.
	 *
	 * @deprecated This is no longer necessary and was added to ease usage of serializables,
	 *     but {@link List#of(Object[])} and {@link java.util.Collection#forEach(Consumer)}
	 *     has made this largely redundant.
	 */
	@Deprecated
	public <T extends NBTSerializable> void registerSerializable(Class<T> serializable) {
		NBTSerialization.registerNBTSerializable(serializable);
		this.serializableClasses.add(serializable);
	}

	/**
	 * Determines whether this plugin is in debug mode, which is determined by a config value.
	 *
	 * @return Returns true if this plguin is in debug mode.
	 */
	public boolean isDebugEnabled() {
		return getConfig().getBoolean("debug", false);
	}

	/**
	 * Generates a file instance based on a file within this plugin's data folder.
	 *
	 * @param path The path of the file relative to the data folder.
	 * @return Returns a file instance of the generated path.
	 */
	public File getDataFile(String path) {
		return new File(getDataFolder(), path);
	}

	/**
	 * Saves a default resource to the plugin's data folder if the file does not already exist.
	 *
	 * @param path The path to the default resource <i>AND</i> the data file.
	 */
	public void saveDefaultResource(String path) {
		if (!getDataFile(path).exists()) {
			saveResource(path, false);
		}
	}

	/**
	 * Saves a particular default resource to a particular location.
	 *
	 * @param defaultPath The path of the file within the plugin's jar.
	 * @param dataPath The path the file should take within the plugin's data folder.
	 */
	public void saveDefaultResourceAs(String defaultPath, String dataPath) {
		Preconditions.checkNotNull(defaultPath, "defaultPath cannot be null.");
		Preconditions.checkNotNull(dataPath, "dataPath cannot be null.");
		if (getDataFile(defaultPath).exists()) {
			return;
		}
		defaultPath = defaultPath.replace('\\', '/');
		dataPath = dataPath.replace('\\', '/');
		final InputStream data = getResource(defaultPath);
		if (data == null) {
			throw new IllegalArgumentException("The embedded resource '" + defaultPath +
					"' cannot be found in " + getFile());
		}
		final File outFile = new File(getDataFolder(), dataPath);
		try {
			FileUtils.copyInputStreamToFile(data, outFile);
		}
		catch (IOException exception) {
			severe("Could not save " + outFile.getName() + " to " + outFile);
			exception.printStackTrace();
		}
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender,
							 @NotNull Command command,
							 @NotNull String label,
							 String[] arguments) {
		if (this.handle != null) {
			return this.handle.execute(sender, command, arguments);
		}
		if (this.newCommandHandler != null) {
			return this.newCommandHandler.executeCommand(sender, command, arguments);
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender,
									  @NotNull Command command,
									  @NotNull String label,
									  String[] arguments) {
		if (this.handle != null) {
			return this.handle.complete(sender, command, arguments);
		}
		if (this.newCommandHandler != null) {
			return this.newCommandHandler.tabCompleteCommand(sender, command, arguments);
		}
		return Collections.emptyList();
	}

	/**
	 * Retrieves this plugin's legacy command handler, if it has one.
	 *
	 * @return Returns this plugin's legacy command handler, or null.
	 */
	public CommandHandler getCommandHandler() {
		return this.handle;
	}

	/**
	 * Registers (or de-registers) a legacy command handler with this plugin.
	 *
	 * @param handler The legacy command handler to set. Null will cause de-registration.
	 */
	public void setCommandHandler(CommandHandler handler) {
		this.handle = handler;
	}

	/**
	 * <p>Retrieves this plugin's standalone command handler, if it has one.</p>
	 *
	 * <p>Note: You can use {@code this.useNewCommandHandler = false;} within your plugin's onEnable() method prior to
	 * the super call to disable the automatic generation of a standalone command handler.</p>
	 *
	 * @return Returns this plugin's standalone command handler, or null.
	 */
	public StandaloneCommandHandler getStandaloneCommandHandler() {
		return this.newCommandHandler;
	}

	/**
	 * Registers (or de-registers) a standalone command handler with this plugin.
	 *
	 * @param handler The standalone command handler to set. Null will cause de-registration.
	 */
	public void setStandaloneCommandHandler(StandaloneCommandHandler handler) {
		this.newCommandHandler = handler;
	}

	/**
	 * Disables this plugin.
	 */
	public void disable() {
		getPluginLoader().disablePlugin(this);
	}

	/**
	 * Simple SEVERE level logging.
	 */
	public void severe(String message) {
		getLogger().log(Level.SEVERE, message);
	}

	/**
	 * Simple SEVERE level logging with Throwable record.
	 */
	public void severe(String message, Throwable error) {
		getLogger().log(Level.SEVERE, message, error);
	}

	/**
	 * Simple WARNING level logging.
	 */
	public void warning(String message) {
		getLogger().log(Level.WARNING, message);
	}

	/**
	 * Simple WARNING level logging with Throwable record.
	 */
	public void warning(String message, Throwable error) {
		getLogger().log(Level.WARNING, message, error);
	}

	/**
	 * Simple WARNING level logging with ellipsis notation shortcut for defered
	 * injection argument array.
	 */
	public void warning(String message, Object... vars) {
		getLogger().log(Level.WARNING, message, vars);
	}

	/**
	 * Simple INFO level logging
	 */
	public void info(String message) {
		getLogger().log(Level.INFO, message);
	}

	/**
	 * Simple INFO level logging with ellipsis notation shortcut for defered
	 * injection argument array.
	 */
	public void info(String message, Object... vars) {
		getLogger().log(Level.INFO, message, vars);
	}

	/**
	 * Live activatable debug message (using plugin's config.yml top level debug tag to decide) at
	 * INFO level.
	 *
	 * Skipped if DebugLog is false.
	 */
	public void debug(String message) {
		if (isDebugEnabled()) {
			getLogger().log(Level.INFO, message);
		}
	}

	/**
	 * Live activatable debug message (using plugin's config.yml top level debug tag to decide) at
	 * INFO level with ellipsis notation shorcut for defered injection argument
	 * array.
	 *
	 * Skipped if DebugLog is false.
	 */
	public void debug(String message, Object... vars) {
		if (isDebugEnabled()) {
			getLogger().log(Level.INFO, message, vars);
		}
	}

	/**
	 * <p>Attempts to retrieve a plugin's instance through several known means.</p>
	 *
	 * <ol>
	 *     <li>
	 *         If there's an instance of the class currently enabled. (Don't request ACivMod.class, or you'll just get
	 *         the the first result.
	 *     </li>
	 *     <li>If there's a public static .getInstance() method.</li>
	 *     <li>If there's a static instance field.</li>
	 * </ol>
	 *
	 * @param <T> The type of the plugin.
	 * @param clazz The class object of the plugin.
	 * @return Returns the first found instance of the plugin, or null. Nulls don't necessarily mean there isn't an
	 *     instance of the plugin in existence. It could just be that it's located some unexpected place. Additionally,
	 *     just because an instance has been returned does not mean that instance is enabled.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JavaPlugin> T getInstance(final Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		try {
			return JavaPlugin.getPlugin(clazz);
		}
		catch (final IllegalArgumentException | IllegalStateException ignored) { }
		for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			if (clazz.equals(plugin.getClass())) {
				return (T) plugin;
			}
		}
		for (final String methodName : Arrays.asList("getInstance", "getPlugin")) {
			try {
				final Method method = clazz.getDeclaredMethod(methodName);
				if (Modifier.isPublic(method.getModifiers())
						&& Modifier.isStatic(method.getModifiers())
						&& clazz.isAssignableFrom(method.getReturnType())) {
					return (T) method.invoke(null);
				}
			}
			catch (final Exception ignored) { }
		}
		for (final String fieldName : Arrays.asList("instance", "plugin")) {
			try {
				final Field field = clazz.getField(fieldName);
				if (Modifier.isStatic(field.getModifiers())
						&& clazz.isAssignableFrom(field.getType())) {
					return (T) field.get(null);
				}
			}
			catch (final Exception ignored) { }
		}
		// Otherwise there's no instance of the plugin, or it's stored in an unusual way
		return null;
	}

}
