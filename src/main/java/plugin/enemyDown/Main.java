package plugin.enemyDown;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.enemyDown.command.EnemyDownCommand;
import plugin.enemyDown.command.GetBedCommand;

/**
 * MainクラスがJavaPluginを拡張しており、Bukkitプラグインのエントリーポイントとして機能しています。
 * finalキーワードは、このクラスが変更されないことを示しています。
 * onEnableメソッドは、プラグインが有効化された際に呼び出されるメソッドで、プラグインの初期化ロジックを含んでいます。
 */
public final class Main extends JavaPlugin {
  @Override
  public void onEnable() {
    // EnemyDownCommandクラスのインスタンスを作成しています。
    // このクラスは、コマンドやイベントの処理を担当するカスタムクラスです
    EnemyDownCommand enemyDownCommand = new EnemyDownCommand(this);

    // EnemyDownCommandをイベントリスナーとして登録しています。
    // これにより、EnemyDownCommandがゲーム内のイベントを監視し、処理できるようになります。
    // イベントリスナーとは、特定のイベントが発生したときに呼び出されるメソッドを持つクラスのことです。
    Bukkit.getPluginManager().registerEvents(enemyDownCommand,this);

    // getCommandメソッドを使用して、プラグイン内で使用可能なコマンドを設定しています。
    // enemyDownコマンドはEnemyDownCommandに紐付けられ、getBedコマンドはGetBedCommandクラスに紐付けられています。
    getCommand("enemyDown").setExecutor(enemyDownCommand);
    getCommand("getBed").setExecutor(new GetBedCommand());
  }
}