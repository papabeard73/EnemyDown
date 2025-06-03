package plugin.enemyDown;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.enemyDown.command.EnemyDownCommand;
import plugin.enemyDown.command.GetBedCommand;

public final class Main extends JavaPlugin {
  @Override
  public void onEnable() {
    // Plugin startup logic

    EnemyDownCommand enemyDownCommand = new EnemyDownCommand(this);

    // イベントリスナーを登録
    Bukkit.getPluginManager().registerEvents(enemyDownCommand,this);

    getCommand("enemyDown").setExecutor(enemyDownCommand);
    getCommand("getBed").setExecutor(new GetBedCommand());
  }
}