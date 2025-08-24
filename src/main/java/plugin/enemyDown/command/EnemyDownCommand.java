package plugin.enemyDown.command;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
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
import org.bukkit.potion.PotionEffect;
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
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String LIST = "list";
  // プラグインのメインクラスへの参照を保持するフィールドです。
  // このフィールドを通じて、Bukkitのスケジューラーやその他のプラグイン機能にアクセスできます。
  private Main main;
  // プレイヤーのスコア情報を管理するためのリストです。
  // このリストには、PlayerScoreオブジェクトが格納され、各プレイヤーの名前とスコアを追跡します。
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Entity> spawnEntityList = new ArrayList<>();

  // Mainクラスのインスタンスを受け取り、mainフィールドに格納します。
  // このコンストラクタにより、EnemyDownCommandクラスはプラグインのメインクラスと連携できるようになります。
  public EnemyDownCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {

    if (args.length == 1 && (LIST.equals(args[0])) ) {
      try(Connection con = DriverManager.getConnection(
          "jdbc:mysql://localhost:3306/spigot_server",
          "root",
          "amanuma"
      );
      Statement statement = con.createStatement();
      ResultSet resultset = statement.executeQuery("select * from player_score")){
        while(resultset.next()){
          int id = resultset.getInt("id");
          String name = resultset.getString("player_name");
          int score = resultset.getInt("score");
          String difficulty = resultset.getString("difficulty");

          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          LocalDateTime date = LocalDateTime.parse(resultset.getString("registered_at"), formatter);

          player.sendMessage(id + " | " + name + " | " + score + " | " + difficulty + " | " + date.format(formatter));
        }
      }catch (SQLException e){
        e.printStackTrace();;
      }
      return false;
    }

    String difficulty = getDifficulty(player, args);
    if (difficulty.equals(NONE)){
      return false;
    }
    // コマンドを実行したのがプレイヤーである場合、
    // PlayerScoreオブジェクトを取得し、ゲーム時間を20秒に設定します。
    PlayerScore nowPlayer = getPlayerScore(player);
    nowPlayer.setGameTime(GAME_TIME);
    nowPlayer.setScore(0);
    removePotionEffect(player);

    // プレイヤーが所属するワールド情報を取得し、プレイヤーのステータスを初期化します。
    World world = player.getWorld();

    // この初期化では、プレイヤーの体力や空腹度を最大値に設定し、装備をダイヤモンド製のアイテムに変更しています。
    initPlayerStatus(player);

    // 一定間隔で敵を出現させるタスクをスケジュールします。
    // このタスクは、ゲーム時間が0以下になるとキャンセルされ、プレイヤーにゲーム終了のメッセージを表示します。
    gamePlay(player, nowPlayer, world, difficulty);
    return true;
  }

  /**
   * 難易度をコマンド引数から取得します
   * @param player　コマンドを実行したプレイヤー
   * @param args　コマンド引数
   * @return　難易度
   */
  @NotNull
  private String getDifficulty(Player player, String[] args) {
    String difficulty;
    if (args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0])) ) {
      difficulty = args[0];
      return difficulty;
    } else {
      player.sendMessage(ChatColor.RED + "実行できません。コマンド引数の1つ目に難易度指定が必要です。[easy, normal, hard]");
      return NONE;
    }
  }


  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   * @param player　コマンドを実行したプレイヤー
   * @param nowPlayerScore　プレイヤースコア情報
   * @param world　プレイヤーのいるワールド
   * @param difficulty ゲームの難易度
   */
  private void gamePlay(Player player, PlayerScore nowPlayerScore, World world, String difficulty) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable ->{
      if(nowPlayerScore.getGameTime() <= 0) {

        // ゲーム時間が0以下になった場合、タスクをキャンセルし、プレイヤーにゲーム終了のメッセージを表示します。
        Runnable.cancel();
        player.sendTitle("ゲームが終了しました！", "あなたのスコアは" + nowPlayerScore.getScore() + "点です！", 0, 30, 0);

        try(Connection con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/spigot_server",
            "root",
            "amanuma");
            Statement statement = con.createStatement()){
          statement.executeUpdate(
              "insert player_score(Player_name, score, difficulty, registered_at)"
              + "values('" + nowPlayerScore.getPlayerName() + "'," + nowPlayerScore.getScore() + ", '" + difficulty + "', now());");
        } catch (SQLException e){
          e.printStackTrace();;
        }

        spawnEntityList.forEach(Entity::remove);
        spawnEntityList = new ArrayList<>();

        removePotionEffect(player);

        return;
      }

      // 生成された座標にランダムな敵を出現させます。
      Entity spawnEntity = world.spawnEntity(getEnemySpawnLocation(player, world), getEnemy(difficulty));
      spawnEntityList.add(spawnEntity);
      // ゲーム時間を5秒減少させます。
      nowPlayerScore.setGameTime(nowPlayerScore.getGameTime() - 5);
    }, 0, 5*20);
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label, String[] args) {
    return false;
  }

  /**
   * 敵の種類をランダムに選択するメソッド。
   * 敵の種類は、Zombie、Skeleton、Witchのいずれかです。
   * @param difficulty 難易度
   * @return EntityType ランダムに選択された敵の種類
   */
  private static EntityType getEnemy(String difficulty) {
    List<EntityType> enemyList = new ArrayList<>();
    switch (difficulty) {
      case NORMAL -> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON);
      case HARD -> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH); // ZOMBIE_VILLAGER
      default -> enemyList = List.of(EntityType.ZOMBIE); // デフォルトはeasy
    }


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
//    PlayerScore playerScore = new PlayerScore(player.getName());

    if(playerScoreList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for(PlayerScore score : playerScoreList )
        if (!score.getPlayerName().equals(player.getName())) {
          return addNewPlayer(player);
        } else {
          return score;
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
   * プレイヤーに設定されている特殊効果を除外します
   * @param player　コマンドを実行したプレイヤー
   */
  private void removePotionEffect(Player player) {
    for(PotionEffect effect : player.getActivePotionEffects()){
      player.removePotionEffect(effect.getType());
    }
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
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();




    if (Objects.isNull(player) || spawnEntityList.stream()
        .noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    playerScoreList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point = switch (enemy.getType()) {
            case ZOMBIE -> 10;
            case SKELETON, WITCH -> 20; // ZOMBIE_VILLAGER
            default -> 0;
          };

          p.setScore(p.getScore() + point);
          player.sendMessage("敵を倒した！現在のスコアは" + p.getScore() + "点！");
        });

  }
}
