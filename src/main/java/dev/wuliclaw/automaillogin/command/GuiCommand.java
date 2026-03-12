package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.gui.AuthMenuHolder;
import dev.wuliclaw.automaillogin.service.AuthService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class GuiCommand implements CommandExecutor {
    private final AuthService authService;

    public GuiCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return true;
        }
        Inventory inventory = Bukkit.createInventory(new AuthMenuHolder(), 27, "AutoMailLogin 菜单");
        inventory.setItem(10, createItem(Material.NAME_TAG, "§a绑定邮箱", List.of("§7命令: /mailregister <邮箱>", "§7先提交邮箱并发送验证码")));
        inventory.setItem(11, createItem(Material.PAPER, "§b输入验证码", List.of("§7命令: /mailcode <验证码>", "§7确认注册验证码")));
        inventory.setItem(12, createItem(Material.TRIPWIRE_HOOK, "§e设置密码", List.of("§7命令: /setpassword <密码> <确认密码>", "§7完成注册")));
        inventory.setItem(14, createItem(Material.IRON_DOOR, "§a密码登录", List.of("§7命令: /login <密码>", "§7使用密码登录")));
        inventory.setItem(15, createItem(Material.REDSTONE_TORCH, "§6二次验证", List.of("§7命令: /mail2fa <验证码>", "§7完成 2FA")));
        inventory.setItem(16, createItem(Material.BOOK, "§d重置密码", List.of("§7命令: /forgotpassword <邮箱>", "§7然后 /resetpassword <验证码> <新密码> <确认密码>")));
        inventory.setItem(22, createItem(Material.COMPASS, "§f当前状态", List.of(authService.describeStatus(player))));
        player.openInventory(inventory);
        return true;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
