package plugin.enemyDown.command;

import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EnemyDownCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player){
        player.setHealth(20);
        player.setFoodLevel(20);

        // ワールドの情報を変数に持つ。
        World world = player.getWorld();

        Location enemySpawnLocation = enemySpawnLocation(player, world);

        int randomIntEnemy = new SplittableRandom().nextInt(2);
        List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.CREEPER);

        world.spawnEntity(enemySpawnLocation, enemyList.get(randomIntEnemy));
      };
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
}
