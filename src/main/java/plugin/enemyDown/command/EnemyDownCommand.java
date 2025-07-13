package plugin.enemyDown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
 * implementsはインターフェースを実装する際に使い、extendはクラスを継承（またはインターフェースを拡張）する際に使います。
 * implements：クラスがインターフェースのメソッドを実装する場合に使用します。多重実装が可能です。
 * extends：クラスが他のクラスを継承する場合、またはインターフェースが他のインターフェースを拡張する場合に使用します。クラスの継承は単一継承のみです。
 */
public class EnemyDownCommand extends BaseCommand implements CommandExecutor, Listener {

  public static final int GAME_TIME = 20;
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
  public boolean onExecutePlayerCommand(Player player) {
    // コマンドを実行したのがプレイヤーである場合、
    // PlayerScoreオブジェクトを取得し、ゲーム時間を20秒に設定します。
    PlayerScore nowPlayer = getPlayerScore(player);
    nowPlayer.setGameTime(GAME_TIME);

    // プレイヤーが所属するワールド情報を取得し、プレイヤーのステータスを初期化します。
    World world = player.getWorld();

    // この初期化では、プレイヤーの体力や空腹度を最大値に設定し、装備をダイヤモンド製のアイテムに変更しています。
    initPlayerStatus(player);

    // 一定間隔で敵を出現させるタスクをスケジュールします。
    // このタスクは、ゲーム時間が0以下になるとキャンセルされ、プレイヤーにゲーム終了のメッセージを表示します。
    gamePlay(player, nowPlayer, world);
    return true;
  }

  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   * @param player　コマンドを実行したプレイヤー
   * @param nowPlayer　プレイヤースコア情報
   * @param world　プレイヤーのいるワールド
   */
  private void gamePlay(Player player, PlayerScore nowPlayer, World world) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable ->{
      if(nowPlayer.getGameTime() <= 0) {
        // ゲーム時間が0以下になった場合、タスクをキャンセルし、プレイヤーにゲーム終了のメッセージを表示します。
        Runnable.cancel();
        player.sendTitle("ゲームが終了しました！", "あなたのスコアは" + nowPlayer.getScore() + "点です！", 0, 30, 0);
        nowPlayer.setScore(0);
        // プレイヤーの周囲にいる敵（スケルトン、ゾンビ、ウィッチ）を削除します。
        List<Entity> nearbyEnemies = player.getNearbyEntities(100, 100, 100);
        for (Entity enemy : nearbyEnemies) {
          switch (enemy.getType()) {
            case SKELETON -> enemy.remove();
            case ZOMBIE -> enemy.remove();
            case WITCH -> enemy.remove();
          }
        }
        return;
      }

      // 生成された座標にランダムな敵を出現させます。
      world.spawnEntity(getEnemySpawnLocation(player, world), getEnemy());
      // ゲーム時間を5秒減少させます。
      nowPlayer.setGameTime(nowPlayer.getGameTime() - 5);
    }, 0, 5*20);
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender) {
    return false;
  }

  /**
   * 敵の種類をランダムに選択するメソッド。
   * 敵の種類は、Zombie、Skeleton、Witchのいずれかです。
   * @return EntityType ランダムに選択された敵の種類
   */
  private static EntityType getEnemy() {
    // また、敵の種類をランダムに選択するために、EntityTypeのリストを作成しています。
    List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
    int randomIntEnemy = new SplittableRandom().nextInt(enemyList.size());
    return enemyList.get(randomIntEnemy);
  }

  /**
   * 敵の出現位置を取得するメソッド。
   * プレイヤーの位置を基準に、敵が出現するランダムな座標を生成します。
   * @param player コマンドを実行したプレイヤー
   * @param world コマンドを実行したプレイヤーが所属するワールド
   * @return Location 敵が出現する位置
   */
  private Location getEnemySpawnLocation(Player player, World world) {
    return enemySpawnLocation(player, world);
  }

  /**
   * プレイヤーのステータスを初期化するメソッド。
   * プレイヤーの体力と空腹度を最大値（20）に設定し、装備をダイヤモンド製のアイテムに変更します。
   * @param player コマンドを実行したプレイヤー
   */
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

  /**
   * プレイヤーのスコア情報を取得するメソッド。
   * プレイヤーが初めてコマンドを実行した場合は新規のPlayerScoreオブジェクトを作成し、リストに追加します。
   * 既存のプレイヤーの場合は、そのスコア情報を返します。
   * private修飾子が付いているため、このEnemyDownCommandクラス内でのみgetPlayerScoreメソッドを呼び出すことができます。
   * 戻り値の型はPlayerScoreなので、呼び出し元にはPlayerScore型のオブジェクトが返されます。
   * @param player コマンドを実行したプレイヤー
   * @return PlayerScore オブジェクト
   */
  private PlayerScore getPlayerScore(Player player) {
    if(playerScoreList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for(PlayerScore playerScore : playerScoreList )
        if (!playerScore.getPlayerName().equals(player.getName())) {
          return addNewPlayer(player);
        } else {
          return playerScore;
        }
    }
    return null;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player　コマンドを実行したプレイヤー
   * @return PlayerScore オブジェクト
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());
//    playerScore.setPlayerName(player.getName());
    playerScoreList.add(playerScore);
    return playerScore;
  }

  /**
   * 敵を出現させる場所を生成するメソッド
   * 出現エリアはX軸とZ軸は自分の位置からプラスランダムで-10〜9の値が設定されます
   * Y軸はプレイヤーと同じ位置です
   * @param player コマンドを実行したプレイヤー
   * @param world コマンドを実行したプレイヤーが所属するワールド
   * @return Location 敵が出現する位置
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

  /**
   * 敵が倒された際にスコアを加算するイベントハンドラー。
   * イベントハンドラーとは、特定のイベント（例：エンティティの死亡、プレイヤーの移動など）が発生したときに自動的に呼び出されるメソッドのことです。
   * Bukkitプラグインでは、@EventHandlerアノテーションを付けてイベントを受け取るメソッドを定義し、イベント発生時にそのメソッドが実行されます。
   * この仕組みにより、ゲーム内のさまざまな出来事に対して独自の処理を追加できます。
   * プレイヤーが敵を倒した場合、そのプレイヤーのスコアを10点加算し、メッセージを送信します。
   * @param e EntityDeathEvent イベントオブジェクト
   */
  @EventHandler
  public void onEnemyDeath (EntityDeathEvent e) {
    LivingEntity entity = e.getEntity();
    Player player = entity.getKiller();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    for(PlayerScore playerScore : playerScoreList ) {
      if(playerScore.getPlayerName().equals(player.getName())){
        int point = switch (entity.getType()) {
          case ZOMBIE -> 10;
          case SKELETON, WITCH -> 20;
          default -> 0;
        };

        playerScore.setScore(playerScore.getScore() + point);
        player.sendMessage("敵を倒した！現在のスコアは" + playerScore.getScore() + "点！");
      }
    }
  }
}
