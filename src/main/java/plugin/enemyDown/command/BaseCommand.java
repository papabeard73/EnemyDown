package plugin.enemyDown.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * コマンドを実行して動かすプラグイン処理の基底クラスです。
 * 「implements」は「インターフェースを実装する」
 */
public abstract class BaseCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String s, @NotNull String[] strings) {
    if (sender instanceof Player player){
      return onExecutePlayerCommand(player);
    } else {
      return onExecuteNPCCommand(sender);
    }
  }

  /**
   * コマンド実行者がプレイヤーだった場合に実行します
   * @param player　コマンドを実行したプレイヤー
   * @return 処理の実行有無
   */
  public abstract boolean onExecutePlayerCommand(Player player);

  /**
   * コマンド実行者がプレイヤー以外だった場合に実行します
   * @param sender　コマンド実行車
   * @return 処理の実行有無
   */
  public abstract boolean onExecuteNPCCommand(CommandSender sender);
}
