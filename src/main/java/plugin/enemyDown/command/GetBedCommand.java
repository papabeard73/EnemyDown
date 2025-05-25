package plugin.enemyDown.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GetBedCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (!player.getInventory().contains(Material.WHITE_BED)) {
        player.getInventory().addItem(new ItemStack(Material.WHITE_BED, 1));
        player.sendMessage("ベッドをインベントリに追加しました！");
      } else {
        player.sendMessage("すでにベッドを持っています！");
      }
      return true;
    } else {
      sender.sendMessage("このコマンドはプレイヤーから実行してください！");
      return false;
    }
  }
}
