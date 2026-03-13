package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.gui.AuthMenuHolder;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.MessageService;
import net.kyori.adventure.text.Component;
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
    private final MessageService messageService;

    public GuiCommand(AuthService authService, MessageService messageService) {
        this.authService = authService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.get("player-only", "该命令只能由玩家执行。"));
            return true;
        }
        AuthMenuHolder holder = new AuthMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("AutoMailLogin 菜单"));
        holder.bind(inventory);
        inventory.setItem(10, createItem(Material.NAME_TAG, "§a绑定邮箱", List.of("§7点击后在聊天栏输入邮箱", "§7将自动发送注册验证码")));
        inventory.setItem(11, createItem(Material.PAPER, "§b输入验证码", List.of("§7点击后在聊天栏输入验证码", "§7确认注册验证码")));
        inventory.setItem(12, createItem(Material.TRIPWIRE_HOOK, "§e设置密码", List.of("§7点击后输入：密码 空格 确认密码", "§7完成注册")));
        inventory.setItem(14, createItem(Material.IRON_DOOR, "§a密码登录", List.of("§7点击后在聊天栏输入密码", "§7使用密码登录")));
        inventory.setItem(15, createItem(Material.REDSTONE_TORCH, "§6二次验证", List.of("§7点击后在聊天栏输入验证码", "§7完成 2FA")));
        inventory.setItem(16, createItem(Material.BOOK, "§d重置密码", List.of("§7点击后输入邮箱获取验证码", "§7然后再输入：验证码 空格 新密码 空格 确认密码")));
        inventory.setItem(22, createItem(Material.COMPASS, "§f当前状态", List.of(authService.describeStatus(player))));
        inventory.setItem(26, createItem(Material.BARRIER, "§c取消输入", List.of("§7清除当前 GUI 输入状态")));
        player.openInventory(inventory);
        return true;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
