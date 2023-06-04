package san.kuroinu.itemsendplugin;

import com.sun.tools.javac.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import san.kuroinu.itemsendplugin.commands.ItemReceive;
import san.kuroinu.itemsendplugin.commands.ItemSend;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class ItemSendPlugin extends JavaPlugin {
    public static JavaPlugin plugin;
    private Listeners listeners;
    public static Map<String,String> players = new HashMap<>();
    public static Economy econ = null;
    public static ItemStack send_button = new ItemStack(Material.FEATHER);
    public static String prefix = "§5[ItemSendPlugin]§r";

    @Override
    public void onEnable() {
        // Plugin startup logic
        //Itemmetaを定義
        ItemMeta send_button_meta = send_button.getItemMeta();
        send_button_meta.setDisplayName("§a配送する");
        send_button_meta.setLore(Arrays.asList("§7クリックすると100円を消費して配送します"));
        send_button.setItemMeta(send_button_meta);
        plugin = this;
        try {
            this.listeners = new Listeners();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Bukkit.getPluginManager().registerEvents(this.listeners, this);
        getCommand("itemsend").setExecutor(new ItemSend());
        getCommand("itemreceive").setExecutor(new ItemReceive());
        super.onEnable();
        plugin.saveDefaultConfig();
        if (!setupEconomy() ) {
            getServer().getConsoleSender().sendMessage(prefix + " Vault not found, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    private static Boolean setupEconomy() {
        if (getPlugin().getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getPlugin().getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null){
            return false;
        }else{
            econ = rsp.getProvider();
        }
        return econ != null;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        super.onDisable();
    }
    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
