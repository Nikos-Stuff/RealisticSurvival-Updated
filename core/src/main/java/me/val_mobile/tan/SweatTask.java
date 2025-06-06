/*
    Copyright (C) 2025  Val_Mobile

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.val_mobile.tan;

import me.val_mobile.data.RSVPlayer;
import me.val_mobile.rsv.RSVPlugin;
import me.val_mobile.utils.RSVTask;
import me.val_mobile.utils.Utils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SweatTask extends BukkitRunnable implements RSVTask {

    private static final Map<UUID, SweatTask> tasks = new HashMap<>();
    private final TempManager manager;
    private final FileConfiguration config;
    private final RSVPlugin plugin;
    private final RSVPlayer player;
    private final UUID id;
    private final Collection<String> allowedWorlds;
    private final double chance;
    private final double minTemperature;
    private final int minCount;
    private final int maxCount;
    private final double xOffset;
    private final double yOffset;
    private final double zOffset;
    private final double extra;

    private final Particle particle;
    private Particle.DustOptions dust;


    public SweatTask(TanModule module, RSVPlugin plugin, RSVPlayer player) {
        this.plugin = plugin;
        this.manager = module.getTempManager();
        this.config = module.getUserConfig().getConfig();
        this.player = player;
        this.id = player.getPlayer().getUniqueId();
        this.allowedWorlds = module.getAllowedWorlds();
        this.chance = config.getDouble("Temperature.Sweating.Chance");
        this.minTemperature = config.getDouble("Temperature.Sweating.MinimumTemperature");
        this.minCount = config.getInt("Temperature.Sweating.MinCount");
        this.maxCount = config.getInt("Temperature.Sweating.MaxCount");
        this.xOffset = config.getDouble("Temperature.Sweating.x-Offset");
        this.yOffset = config.getDouble("Temperature.Sweating.y-Offset");
        this.zOffset = config.getDouble("Temperature.Sweating.z-Offset");
        this.extra = config.getDouble("Temperature.Sweating.Extra");
        this.particle = Particle.valueOf(config.getString("Temperature.Sweating.Particle"));

        if (particle == Particle.REDSTONE) {
            String color = config.getString("Temperature.Sweating.DustOptionColor");
            float size = (float) config.getDouble("Temperature.Sweating.DustOptionSize");

            if (color.contains("|")) {
                int first = color.indexOf("|");
                int second = color.lastIndexOf("|");

                int red = Integer.parseInt(color.substring(0, first));
                int green = Integer.parseInt(color.substring(first + 1, second));
                int blue = Integer.parseInt(color.substring(second + 1));

                dust = new Particle.DustOptions(Color.fromRGB(red, green, blue), size);
            }
            else {
                dust = new Particle.DustOptions(Utils.valueOfColor(color), size);
            }
        }

        tasks.put(id, this);
    }

    @Override
    public void run() {
        Player player = this.player.getPlayer();

        if (conditionsMet(player)) {
            if (!player.hasPermission("realisticsurvival.toughasnails.resistance.hot.sweat")) {
                if (Utils.roll(chance)) {
                    Vector dir = player.getLocation().clone().subtract(0, 0.5D, 0).getDirection().normalize().multiply(0.5D);
                    player.spawnParticle(particle, player.getEyeLocation().add(dir), Utils.getRandomNum(minCount, maxCount), xOffset, yOffset, zOffset, extra, dust);
                }
            }
        }
        else {
            stop();
        }
    }

    @Override
    public boolean conditionsMet(@Nullable Player player) {
        return globalConditionsMet(player) && !player.isDead() && allowedWorlds.contains(player.getWorld().getName()) && manager.getTemperature(player) > minTemperature;
    }

    @Override
    public void start() {
        int tickPeriod = config.getInt("Temperature.Sweating.TickPeriod"); // get the tick period
        this.runTaskTimer(plugin, 0L, tickPeriod);
    }

    @Override
    public void stop() {
        tasks.remove(id);
        cancel();
    }

    public static boolean hasTask(UUID id) {
        return tasks.containsKey(id) && tasks.get(id) != null;
    }

    public static Map<UUID, SweatTask> getTasks() {
        return tasks;
    }
}
