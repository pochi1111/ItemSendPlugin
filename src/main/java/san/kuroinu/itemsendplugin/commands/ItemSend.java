package san.kuroinu.itemsendplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.events.SequenceEndEvent;

import static san.kuroinu.itemsendplugin.ItemSendPlugin.*;

public class ItemSend implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        //プレイヤーのみ
        if (args.length == 1 && args[0].equals("reload")) {
            if (!(commandSender instanceof Player)) {
                plugin.reloadConfig();
                commandSender.sendMessage(prefix+"configをリロードしました");
                return true;
            }
            Player p = (Player) commandSender;
            if(p.isOp()){
                plugin.reloadConfig();
                commandSender.sendMessage(prefix+"configをリロードしました");
                return true;
            }else{
                commandSender.sendMessage(prefix+"権限がありません");
                return true;
            }
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(prefix+"プレイヤーのみ実行可能です");
            return true;
        }
        if (args.length == 0){
            commandSender.sendMessage(prefix+"使い方: /itemsend [player]");
            return true;
        }else{
            //args[0]のプレイやーがログインしたことがあるかを判定
            if (!Bukkit.getOfflinePlayer(args[0]).hasPlayedBefore()){
                commandSender.sendMessage(prefix+"そのプレイヤーは存在しません");
                return true;
            }else{
                //args[0]のプレイヤーにアイテムを送る
                //プレイヤーの名前をリストに追加
                players.put(commandSender.getName(), args[0]);
                //インベントリを開く
                Inventory inv = Bukkit.createInventory(null, 9, "アイテムを送る");
                //右端にボタンを設置
                inv.setItem(8,send_button);
                //プレイヤーにインベントリを開かせる
                Player e = (Player) commandSender;
                e.openInventory(inv);
            }
        }
        return true;
    }
}
