package co.uk.shaypunter.minecraft.redeemer;

import co.uk.shaypunter.minecraft.redeemer.commands.RedeemCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Redeemer extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig("data");
        getConfig("messages");

        getCommand("redeem").setExecutor(new RedeemCommand());

        new Metrics(this);
    }

    @Override
    public void onDisable() {

    }

    public List<String> getRedeemableCommands(String redeemable) {
        return getConfig().getStringList("redeemables." + redeemable + ".commands");
    }


    /**
     * Gets the amount of time a redeemable can be redeemed
     * @param redeemable redeemable to use
     * @return time a redeemable can be used
     */
    public Integer getRedeemableTimes(String redeemable) {
        return Redeemer.getInstance().getConfig().getInt("redeemables." + redeemable + "timesCanRedeem");
    }

    /**
     * Check to see if the player has permission to use a redeemable
     * @param player player to check permission on
     * @param redeemable redeemable to check permission for
     * @return if the player has permission or not
     */
    public boolean hasRedeemablePermission(Player player, String redeemable) {
        return (Redeemer.getInstance().getConfig().getString("redeemables." + redeemable + ".permission") == null) || player.hasPermission(Redeemer.getInstance().getConfig().getString("redeemables." + redeemable + ".permission"));
    }

    /**
     * Gets the serialised string in the data config file of a redeemable player
     * @param redeemable redeemable to check
     * @param uuid player uuid to check for
     * @return serialised player string
     */
    public String getRedeemedUser(String redeemable, UUID uuid) {
        for (String redeemableUser:getConfig("data").getStringList(redeemable)) {
            String user = redeemableUser.split(":")[0];
            if (user.equalsIgnoreCase(uuid.toString()))
                return redeemableUser;
        }
        return uuid + ":0";
    }

    /**
     * Adds a user to the data file with the amount of times they have redeemed a redeemable
     * @param uuid player uuid
     * @param redeemable redeemable they used
     * @param timesRedeemed times they have redeemed
     */
    public void saveRedeemedUserToConfig(UUID uuid, String redeemable, int timesRedeemed) {
        String oldSerialise = uuid + ":" + (timesRedeemed - 1);
        String seralised = uuid + ":" + timesRedeemed;
        FileConfiguration config = getConfig("data.yml");

        List<String> stringList = config.getStringList(redeemable);
        stringList.remove(oldSerialise);
        stringList.add(seralised);
        config.set(redeemable, stringList);

        try {
            config.save(new File(getDataFolder() + "/" + "data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets all the redeemable commands (e.g. /redeem example)
     * @return list of redeemable commands
     */
    public List<String> getRedeemables() {
        return new ArrayList<>(getConfig().getConfigurationSection("redeemables").getKeys(false));
    }

    /**
     * Get an instance of this class
     * @return Redeemer instance
     */
    public static Redeemer getInstance() {
        return getPlugin(Redeemer.class);
    }

    /**
     * Mirrors bukkit's implementation of getConfig in JavaPlugin however takes any config name
     * If the name doesn't end in .yml, it will append .yml
     * It will then attempt to open the file in the data folder
     * If thats not found it will save the file from the plugin jar
     * Then it loads the saved file into a config
     * Then loads the config from the jar to setup the defaults.
     *
     * @param name name of the config file
     * @return the yml configuration
     */
    public final FileConfiguration getConfig(final String name) {
        String fileName = name;
        if (!fileName.endsWith(".yml"))
            fileName += ".yml";

        final File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            this.saveResource(fileName, true);
        }

        final FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        final Reader defaultConfigStream = new InputStreamReader(this.getResource(fileName), StandardCharsets.UTF_8);
        if (defaultConfigStream != null) {
            final YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
            config.setDefaults(defaultConfig);
        }

        return config;
    }

    /**
     * Checks if a config file exists in the plugin or in the data folder
     *
     * @param name config to look for
     * @return if it exists
     */
    public boolean hasConfig(final String name) {
        final String fileName = name + (name.endsWith(".yml") ? "" : ".yml");
        final File file = new File(getDataFolder(), fileName);
        return this.getResource(fileName) != null || (file.exists() && !file.isDirectory());
    }
}
