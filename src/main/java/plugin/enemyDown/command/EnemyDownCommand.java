package plugin.enemyDown.command;

import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
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

public class EnemyDownCommand implements CommandExecutor, Listener {
  private Player player;
  private int score;

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player){
        this.player = player;

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

        world.spawnEntity(enemySpawnLocation, enemyList.get(randomIntEnemy));
      }
    return false;
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
      if (Objects.isNull(player)){
        return;
      }
      if (Objects.isNull(this.player)){
        return;
      }
      if(this.player.getName().equals(player.getName())) {
        score += 10;
        player.sendMessage("敵を倒した！現在のスコアは" + score);
    }
  }
}
