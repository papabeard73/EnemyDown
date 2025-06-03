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

public class EnemyDownCommand implements CommandExecutor, Listener {
  private Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private int gameTime = 20;

  public EnemyDownCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    gameTime = 20;
      if (sender instanceof Player player){
        if(playerScoreList.isEmpty()) {
          addNewPlayer(player);
        } else {
          for(PlayerScore playerScore : playerScoreList ) {
            if(!playerScore.getPlayerName().equals(player.getName())){
              addNewPlayer(player);
            }
          }
        }

        player.setHealth(20);
        player.setFoodLevel(20);

        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

        // ワールドの情報を変数に持つ。
        World world = player.getWorld();

        Location enemySpawnLocation = enemySpawnLocation(player, world);

        int randomIntEnemy = new SplittableRandom().nextInt(2);
        List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON);

        Bukkit.getScheduler().runTaskTimer(main, Runnable ->{
          if(gameTime <= 0) {
            Runnable.cancel();
            player.sendMessage("ゲームが終了しました！");
            return;
          }
          world.spawnEntity(enemySpawnLocation, enemyList.get(randomIntEnemy));
          gameTime -= 10;
        }, 0, 5*20);
      }
    return false;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player　コマンドを実行したプレイヤー
   */
  private void addNewPlayer(Player player) {
    PlayerScore playerScore = new PlayerScore();
    playerScore.setPlayerName(player.getName());
    playerScoreList.add(playerScore);
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
