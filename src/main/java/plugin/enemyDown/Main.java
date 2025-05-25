package plugin.enemyDown;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.enemyDown.command.EnemyDownCommand;
import plugin.enemyDown.command.GetBedCommand;

public final class Main extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    // Plugin startup logic
    // イベントリスナーを登録
    Bukkit.getPluginManager().registerEvents(this, this);

    getCommand("enemyDown").setExecutor(new EnemyDownCommand());
    getCommand("getBed").setExecutor(new GetBedCommand());
  }
}