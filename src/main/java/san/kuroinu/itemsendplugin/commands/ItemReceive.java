package san.kuroinu.itemsendplugin.commands;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Arrays;

import static san.kuroinu.itemsendplugin.ItemSendPlugin.plugin;
import static san.kuroinu.itemsendplugin.ItemSendPlugin.prefix;

public class ItemReceive implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //プレイヤーのみ
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(prefix+"プレイヤーのみ実行可能です");
            return true;
        }
        //受け取り
        try {
            Connection con = DriverManager.getConnection(
                    plugin.getConfig().getString("mysql.url"),
                    plugin.getConfig().getString("mysql.user"),
                    plugin.getConfig().getString("mysql.password")
            );
            //sendto_nameが自分の名前であるものを取得
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM items WHERE sendto_name = ?");
            pstmt.setString(1, commandSender.getName());
            ResultSet result = pstmt.executeQuery();
            int i = 0;
            //アイテムを受け取る
            while (result.next()) {
                if (result.getString("givedcheck").equals("true")) continue;
                //空きがあるか
                Player e = (Player) commandSender;
                if (e.getInventory().firstEmpty() == -1) {
                    commandSender.sendMessage("インベントリがいっぱいです");
                    return true;
                }
                //アイテムを受け取る
                String[] lore;
                //お届け元を追加
                lore = new String[]{"お届け元: " + result.getString("sender_name")};
                //アイテムを追加
                ItemStack item = createGuiItem(Material.IRON_HOE, "§aお届けBox", lore);
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.INTEGER, result.getInt("id"));
                item.setItemMeta(meta);
                e.getInventory().addItem(item);
                i++;
                //givedcheckをtrueにする
                PreparedStatement pstmt2;
                pstmt2 = con.prepareStatement("UPDATE items SET givedcheck = ? WHERE id = ?");
                pstmt2.setString(1, "true");
                pstmt2.setInt(2, result.getInt("id"));
                pstmt2.executeUpdate();
            }
            if (i == 0) {
                commandSender.sendMessage("お届けBoxにアイテムはありません");
                return true;
            }else{
                commandSender.sendMessage(i+"個のアイテムを受け取りました");
            }
            //データベースを閉じる
            result.close();
            pstmt.close();
            con.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        // Set the name of the item
        meta.setDisplayName(name);
        // Set the lore of the item
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
