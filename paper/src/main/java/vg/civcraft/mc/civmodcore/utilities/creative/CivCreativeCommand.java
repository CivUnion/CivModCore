package vg.civcraft.mc.civmodcore.utilities.creative;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import java.util.HashSet;

public class CivCreativeCommand extends BaseCommand {

	private final CivModCorePlugin plugin;

	public CivCreativeCommand(CivModCorePlugin plugin){
		this.plugin = plugin;
	}

	@CommandAlias("civcreative")
	@CommandPermission("cmc.debug")
	@Description("Opens the admin creative menu")
	public void execute(Player player){
		CivCreativeMenu menu = new CivCreativeMenu(player, plugin.getCreativeManager());
		menu.showCreativeMenu(new HashSet<>());
	}

}
