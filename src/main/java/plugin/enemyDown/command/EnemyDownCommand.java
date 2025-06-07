package plugin.enemyDown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import plugin.enemyDown.Main;
import plugin.enemyDown.data.PlayerScore;

/**
 * EnemyDownのゲームを実行するコマンドクラス。
 * プレイヤーがコマンドを実行すると、敵を出現させ、プレイヤーのスコアを管理します。
 * また、敵が倒された際にスコアを加算します。
 */
public class EnemyDownCommand implements CommandExecutor, Listener {
  // プラグインのメインクラスへの参照を保持するフィールドです。
  // このフィールドを通じて、Bukkitのスケジューラーやその他のプラグイン機能にアクセスできます。
  private Main main;
  // プレイヤーのスコア情報を管理するためのリストです。
  // このリストには、PlayerScoreオブジェクトが格納され、各プレイヤーの名前とスコアを追跡します。
  private List<PlayerScore> playerScoreList = new ArrayList<>();

  // Mainクラスのインスタンスを受け取り、mainフィールドに格納します。
  // このコンストラクタにより、EnemyDownCommandクラスはプラグインのメインクラスと連携できるようになります。
  public EnemyDownCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player){
        PlayerScore nowPlayer = getPlayerScore(player);
        nowPlayer.setGameTime(20);

        // ワールドの情報を変数に持つ。
        World world = player.getWorld();

        // getPlayerScore(player);

        initPlayerStatus(player);

        Location enemySpawnLocation = enemySpawnLocation(player, world);

        int randomIntEnemy = new SplittableRandom().nextInt(2);
        List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON);

        Bukkit.getScheduler().runTaskTimer(main, Runnable ->{
          if(nowPlayer.getGameTime() <= 0) {
            Runnable.cancel();
            player.sendTitle("ゲームが終了しました！", "あなたのスコアは" + nowPlayer.getScore() + "点です！", 0, 30, 0);
            nowPlayer.setScore(0);
            return;
          }
          world.spawnEntity(enemySpawnLocation, enemyList.get(randomIntEnemy));
          nowPlayer.setGameTime(nowPlayer.getGameTime() - 5);
        }, 0, 5*20);
      }
    return false;
  }

  private static void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);

    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
    inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));


  }

  private PlayerScore getPlayerScore(Player player) {
    if(playerScoreList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for(PlayerScore playerScore : playerScoreList ) {
        if(!playerScore.getPlayerName().equals(player.getName())){
          return addNewPlayer(player);
        } else {
          return playerScore;
        }
      }
    }
    return null;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player　コマンドを実行したプレイヤー
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore playerScore = new PlayerScore();
    playerScore.setPlayerName(player.getName());
    playerScoreList.add(playerScore);
    return playerScore;
  }

  /**
   * 敵を出現させる場所を生成するメソッド
   * 出現エリアはX軸とZ軸は自分の位置からプラスランダムで-10〜9の値が設定されます
   * Y軸はプレイヤーと同じ位置です
   * @param player コマンドを実行したプレイヤー
   * @param world コマンドを実行したプレイヤーが所属するワールド
   * @return ロケーション
   */

  @NotNull
  private Location enemySpawnLocation(Player player, World world) {
    Location playerLocation = player.getLocation();
    SplittableRandom random = new SplittableRandom();
    int randomIntX = random.nextInt(20) - 10;
    int randomIntZ = random.nextInt(20) - 10;
    double x = playerLocation.getX() + randomIntX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomIntZ;
    return new Location(world, x,y,z);
  }

  @EventHandler
  public void onEnemyDeath (EntityDeathEvent e) {
      Player player = e.getEntity().getKiller();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    for(PlayerScore playerScore : playerScoreList ) {
      if(playerScore.getPlayerName().equals(player.getName())){
        playerScore.setScore(playerScore.getScore() + 10);
        player.sendMessage("敵を倒した！現在のスコアは" + playerScore.getScore() + "点！");
      }
    }
  }
}
