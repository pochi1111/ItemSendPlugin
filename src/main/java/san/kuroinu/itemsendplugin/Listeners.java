package san.kuroinu.itemsendplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import static san.kuroinu.itemsendplugin.ItemSendPlugin.*;

public class Listeners implements Listener {

    @EventHandler
    public void onSendButtonClick(InventoryClickEvent event) throws SQLException {
        Player e = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null) return;
        //含まれるなら
        if (players.get(e.getName()) == null) return;
        if (event.getCurrentItem().equals(send_button)){
            event.setCancelled(true);
            //eからお金を100円引く
            if (econ.getBalance(e) < 100){
                e.sendMessage(prefix+ ChatColor.DARK_RED +"お金が足りません");
                for (int i = 0; i < event.getInventory().getSize()-1; i++) {
                    if (event.getInventory().getItem(i) != null){
                        e.getInventory().addItem(event.getInventory().getItem(i));
                    }
                }
                players.remove(e.getName());
                e.closeInventory();
                e.sendMessage(prefix+ ChatColor.DARK_RED +"アイテムを返却し、配送をキャンセルしました");
                return;
            }else{
                econ.withdrawPlayer(e,100);
                e.sendMessage("100円支払いました");
                //eにアイテムを輸送する
                //インベントリ内のアイテムを文字列化
                Connection con = DriverManager.getConnection(
                        plugin.getConfig().getString("mysql.url"),
                        plugin.getConfig().getString("mysql.user"),
                        plugin.getConfig().getString("mysql.password")
                );
                PreparedStatement pstmt;
                pstmt = con.prepareStatement("INSERT INTO items(sender_name,sendto_name,item1,item2,item3,item4,item5,item6,item7,item8,givedcheck) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)");
                pstmt.setString(1, e.getName());
                pstmt.setString(2, players.get(e.getName()));
                for (int i = 0; i < 8; i++) {
                    if (event.getInventory().getItem(i) == null){
                        pstmt.setString(i+3, "null");
                        continue;
                    }
                    pstmt.setString(i+3, encodeItem(event.getInventory().getItem(i)));
                }
                pstmt.setString(11, "false");
                pstmt.executeUpdate();
                pstmt.close();
                con.close();
                //インベントリを閉じる
                e.closeInventory();
                //相手がオンラインなら通知を行う
                OfflinePlayer sendtooff = Bukkit.getOfflinePlayer(players.get(e.getName()));
                if (sendtooff.isOnline()){
                    Player sendto = plugin.getServer().getPlayer(players.get(e.getName()));
                    sendto.sendMessage(prefix+ ChatColor.GREEN +e.getName()+"からアイテムが届きました");
                    sendto.sendMessage(prefix+ ChatColor.GREEN +"アイテムを受け取るには"+ChatColor.RED+"/itemreceive"+ChatColor.GREEN+"を実行してください");
                }
                //playersからeを削除
                players.remove(e.getName());
                //eにメッセージを送信
                e.sendMessage(prefix+ ChatColor.GREEN +"アイテムを配送しました");
            }
        }
    }

    //右クリックしたら
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) throws SQLException {
        if (event.getAction().isRightClick()){
            if (event.getItem().getItemMeta().getDisplayName().equals("§aお届けBox")){
                NamespacedKey key = new NamespacedKey(plugin, "item_id");
                if (event.getItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER) == null) return;
                int ID = event.getItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                //IDがない場合return
                Player e = event.getPlayer();
                e.sendMessage(prefix+"お届け物を確認しています...");
                //インベントリの空きをカウント
                int count = 0;
                for (int i = 0; i < event.getPlayer().getInventory().getSize(); i++) {
                    if (event.getPlayer().getInventory().getItem(i) == null){
                        count++;
                    }
                }
                if (count < 8){
                    event.getPlayer().sendMessage(prefix+ ChatColor.DARK_RED +"インベントリに空きがありません");
                    return;
                }
                Connection con = DriverManager.getConnection(
                        plugin.getConfig().getString("mysql.url"),
                        plugin.getConfig().getString("mysql.user"),
                        plugin.getConfig().getString("mysql.password")
                );
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM items WHERE id = ?");
                pstmt.setInt(1, ID);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    //アイテムをインベントリに入れる
                    for (int i = 0; i < 8; i++) {
                        if (rs.getString(i + 4).equals("null")) {
                            continue;
                        }
                        event.getPlayer().getInventory().addItem(decodeItem(rs.getString(i + 4)));
                    }
                    //アイテムを削除する
                    PreparedStatement pstmt2 = con.prepareStatement("DELETE FROM items WHERE id = ?");
                    pstmt2.setInt(1, ID);
                    pstmt2.executeUpdate();
                    pstmt2.close();
                    rs.close();
                    pstmt.close();
                    con.close();
                    e.sendMessage(prefix + ChatColor.GREEN + "アイテムを受け取りました");
                    e.getInventory().remove(event.getItem());
                }else{
                    e.sendMessage(prefix+ ChatColor.DARK_RED +"アイテムが見つかりませんでした");
                }
            }
        }
    }
    public static String encodeItem(ItemStack itemStack) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("i", itemStack);
        return DatatypeConverter.printBase64Binary(config.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack decodeItem(String string) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(new String(DatatypeConverter.parseBase64Binary(string), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | InvalidConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        return config.getItemStack("i", null);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        Player e = (Player) event.getPlayer();
        if (players.get(e.getName()) != null){
            new Thread( () -> {
                try {
                    Thread.sleep(500);
                    if (players.get(e.getName()) == null) return;
                    for (int i = 0; i < event.getInventory().getSize()-1; i++) {
                        if (event.getInventory().getItem(i) != null){
                            e.getInventory().addItem(event.getInventory().getItem(i));
                        }
                    }
                    players.remove(e.getName());
                    e.sendMessage(ItemSendPlugin.prefix+ ChatColor.DARK_RED +"アイテムを返却し、配送をキャンセルしました");
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } ).start();
        }
    }
}
