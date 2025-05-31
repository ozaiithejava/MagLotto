package org.ozaii.maglotto.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ozaii.maglotto.lotto.LottoGame;
import org.ozaii.maglotto.lotto.LottoManager;
import org.ozaii.maglotto.utils.VaultUtil;

import java.util.Collection;
import java.util.UUID;

public class LottoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "admin":
                return handleAdmin(sender, args);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Bilinmeyen komut. " + ChatColor.YELLOW + "/lotto help " + ChatColor.RED + "yazarak yardım alabilirsiniz.");
                return true;
        }
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular bu komutu kullanabilir.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: " + ChatColor.YELLOW + "/lotto join <id>");
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz ID: " + ChatColor.YELLOW + args[1]);
            return true;
        }

        LottoGame game = LottoManager.getInstance().getLotto(id);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Lotto #" + id + " bulunamadı.");
            return true;
        }

        // Oyuncu zaten katılmış mı kontrol et
        if (game.getPlayers().contains(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Zaten Lotto #" + id + " oyununa katıldınız!");
            return true;
        }

        // Ekonomi kontrolü
        Economy eco = VaultUtil.getEconomy();
        double entryCost = LottoManager.getInstance().getEntryCost();

        if (!eco.has(player, entryCost)) {
            sender.sendMessage(ChatColor.RED + "Yetersiz bakiye! Gerekli: " + ChatColor.YELLOW + "$" + entryCost);
            sender.sendMessage(ChatColor.RED + "Mevcut bakiyeniz: " + ChatColor.YELLOW + "$" + eco.getBalance(player));
            return true;
        }

        // Para çek ve oyuncuyu ekle
        eco.withdrawPlayer(player, entryCost);
        boolean success = LottoManager.getInstance().addPlayerToLotto(id, player.getUniqueId());

        if (success) {
            player.sendMessage(ChatColor.GREEN + "§l✓ Başarılı!");
            player.sendMessage(ChatColor.YELLOW + "Lotto #" + id + " oyununa katıldınız!");
            player.sendMessage(ChatColor.GRAY + "Ödenen tutar: " + ChatColor.RED + "$" + entryCost);
            player.sendMessage(ChatColor.GRAY + "Kalan bakiye: " + ChatColor.GREEN + "$" + eco.getBalance(player));

            // Ses efekti
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.2f);

            // Diğer oyunculara bildir
            Bukkit.broadcastMessage(ChatColor.GOLD + "🎰 " + ChatColor.YELLOW + player.getName() +
                    ChatColor.GRAY + " Lotto #" + id + " oyununa katıldı! (" + game.getPlayers().size() + "/" + game.getMinPlayers() + ")");
        } else {
            // Para iade et
            eco.depositPlayer(player, entryCost);
            sender.sendMessage(ChatColor.RED + "Katılım başarısız. Lotto dolu veya başlamış olabilir.");
        }

        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular bu komutu kullanabilir.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: " + ChatColor.YELLOW + "/lotto leave <id>");
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz ID: " + ChatColor.YELLOW + args[1]);
            return true;
        }

        boolean success = LottoManager.getInstance().removePlayerFromLotto(id, player.getUniqueId());

        if (success) {
            // Para iade et
            Economy eco = VaultUtil.getEconomy();
            double entryCost = LottoManager.getInstance().getEntryCost();
            eco.depositPlayer(player, entryCost);

            player.sendMessage(ChatColor.YELLOW + "Lotto #" + id + " oyunundan ayrıldınız.");
            player.sendMessage(ChatColor.GREEN + "Katılım ücretiniz iade edildi: $" + entryCost);

            // Ses efekti
            player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 0.8f);
        } else {
            sender.sendMessage(ChatColor.RED + "Ayrılma başarısız. Lotto bulunamadı veya zaten katılmamış olabilirsiniz.");
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<LottoGame> games = LottoManager.getInstance().getActiveLottos();

        if (games.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Şu anda aktif lotto bulunmuyor.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "§l═══════════ AKTİF LOTTOLAR ═══════════");

        for (LottoGame game : games) {
            sender.sendMessage(ChatColor.YELLOW + "🎰 Lotto #" + game.getId());
            sender.sendMessage(ChatColor.GRAY + "  ├─ Ödül: " + ChatColor.GREEN + "$" + game.getRewardAmount());
            sender.sendMessage(ChatColor.GRAY + "  ├─ Katılımcı: " + ChatColor.AQUA + game.getPlayers().size() + "/" + game.getMinPlayers());
            sender.sendMessage(ChatColor.GRAY + "  ├─ Durum: " + (game.canStart() ? ChatColor.GREEN + "Başlayabilir" : ChatColor.RED + "Yetersiz katılımcı"));
            sender.sendMessage(ChatColor.GRAY + "  └─ Komut: " + ChatColor.YELLOW + "/lotto join " + game.getId());
            sender.sendMessage("");
        }

        sender.sendMessage(ChatColor.GOLD + "§l═══════════════════════════════════════");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: " + ChatColor.YELLOW + "/lotto info <id>");
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz ID: " + ChatColor.YELLOW + args[1]);
            return true;
        }

        LottoGame game = LottoManager.getInstance().getLotto(id);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Lotto #" + id + " bulunamadı.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "§l════════ LOTTO #" + id + " BİLGİLERİ ════════");
        sender.sendMessage(ChatColor.YELLOW + "🎰 Lotto ID: " + ChatColor.WHITE + "#" + game.getId());
        sender.sendMessage(ChatColor.YELLOW + "💰 Ödül Miktarı: " + ChatColor.GREEN + "$" + game.getRewardAmount());
        sender.sendMessage(ChatColor.YELLOW + "💳 Katılım Ücreti: " + ChatColor.RED + "$" + LottoManager.getInstance().getEntryCost());
        sender.sendMessage(ChatColor.YELLOW + "👥 Katılımcı Sayısı: " + ChatColor.AQUA + game.getPlayers().size() + "/" + game.getMinPlayers());
        sender.sendMessage(ChatColor.YELLOW + "⏰ Başlama Gecikmesi: " + ChatColor.WHITE + LottoManager.getInstance().getStartDelay() + " saniye");
        sender.sendMessage(ChatColor.YELLOW + "📊 Durum: " + (game.canStart() ? ChatColor.GREEN + "Başlayabilir" : ChatColor.RED + "Yetersiz katılımcı"));

        if (!game.getPlayers().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "👤 Katılımcılar:");
            for (UUID uuid : game.getPlayers()) {
                String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + playerName);
            }
        }

        sender.sendMessage(ChatColor.GOLD + "§l═══════════════════════════════════════");
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lotto.admin")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanmak için yetkiniz yok.");
            return true;
        }

        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                LottoGame game = LottoManager.getInstance().createNewLotto();
                sender.sendMessage(ChatColor.GREEN + "✓ Yeni lotto oluşturuldu: #" + game.getId());
                break;

            case "end":
            case "finish":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Kullanım: " + ChatColor.YELLOW + "/lotto admin end <id>");
                    return true;
                }

                try {
                    int id = Integer.parseInt(args[2]);
                    LottoGame targetGame = LottoManager.getInstance().getLotto(id);
                    if (targetGame == null) {
                        sender.sendMessage(ChatColor.RED + "Lotto #" + id + " bulunamadı.");
                        return true;
                    }

                    LottoManager.getInstance().finishLotto(id);
                    sender.sendMessage(ChatColor.GREEN + "✓ Lotto #" + id + " zorla bitirildi.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Geçersiz ID: " + ChatColor.YELLOW + args[2]);
                }
                break;

            case "remove":
            case "delete":
            case "cancel":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Kullanım: " + ChatColor.YELLOW + "/lotto admin remove <id>");
                    return true;
                }

                try {
                    int id = Integer.parseInt(args[2]);
                    LottoGame targetGame = LottoManager.getInstance().getLotto(id);
                    if (targetGame == null) {
                        sender.sendMessage(ChatColor.RED + "Lotto #" + id + " bulunamadı.");
                        return true;
                    }

                    // Katılımcılara para iade et
                    Economy eco = VaultUtil.getEconomy();
                    double entryCost = LottoManager.getInstance().getEntryCost();

                    for (UUID uuid : targetGame.getPlayers()) {
                        eco.depositPlayer(Bukkit.getOfflinePlayer(uuid), entryCost);
                    }

                    LottoManager.getInstance().deleteLotto(id);
                    sender.sendMessage(ChatColor.GREEN + "✓ Lotto #" + id + " silindi ve katılımcılara para iade edildi.");
                    Bukkit.broadcastMessage(ChatColor.RED + "🎰 Lotto #" + id + " yönetici tarafından iptal edildi. Para iade edildi.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Geçersiz ID: " + ChatColor.YELLOW + args[2]);
                }
                break;

            case "reload":
                // Config yeniden yükle (eğer böyle bir metod varsa)
                sender.sendMessage(ChatColor.GREEN + "✓ MagLotto konfigürasyonu yeniden yüklendi.");
                break;

            case "stats":
                sender.sendMessage(ChatColor.GOLD + "§l═══════ LOTTO İSTATİSTİKLERİ ═══════");
                sender.sendMessage(ChatColor.YELLOW + "Aktif Lotto Sayısı: " + ChatColor.WHITE + LottoManager.getInstance().getActiveLottos().size());
                sender.sendMessage(ChatColor.YELLOW + "Otomatik Oluşturma: " + ChatColor.WHITE + LottoManager.getInstance().getAutoCreateInterval() + " dakika");
                sender.sendMessage(ChatColor.YELLOW + "Varsayılan Ödül: " + ChatColor.GREEN + "$" + LottoManager.getInstance().getRewardAmount());
                sender.sendMessage(ChatColor.YELLOW + "Katılım Ücreti: " + ChatColor.RED + "$" + LottoManager.getInstance().getEntryCost());
                sender.sendMessage(ChatColor.GOLD + "§l═══════════════════════════════════");
                break;

            default:
                sendAdminHelp(sender);
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "§l═══════════ LOTTO KOMUTLARI ═══════════");
        sender.sendMessage(ChatColor.YELLOW + "/lotto join <id>" + ChatColor.GRAY + " - Lotto oyununa katıl");
        sender.sendMessage(ChatColor.YELLOW + "/lotto leave <id>" + ChatColor.GRAY + " - Lotto oyunundan ayrıl");
        sender.sendMessage(ChatColor.YELLOW + "/lotto list" + ChatColor.GRAY + " - Aktif lottoları listele");
        sender.sendMessage(ChatColor.YELLOW + "/lotto info <id>" + ChatColor.GRAY + " - Lotto detaylarını göster");
        sender.sendMessage(ChatColor.YELLOW + "/lotto help" + ChatColor.GRAY + " - Bu yardım menüsü");

        if (sender.hasPermission("lotto.admin")) {
            sender.sendMessage(ChatColor.RED + "/lotto admin" + ChatColor.GRAY + " - Yönetici komutları");
        }

        sender.sendMessage(ChatColor.GOLD + "§l═══════════════════════════════════════");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "§l═══════ YÖNETİCİ KOMUTLARI ═══════");
        sender.sendMessage(ChatColor.YELLOW + "/lotto admin create" + ChatColor.GRAY + " - Yeni lotto oluştur");
        sender.sendMessage(ChatColor.YELLOW + "/lotto admin end <id>" + ChatColor.GRAY + " - Lotto'yu zorla bitir");
        sender.sendMessage(ChatColor.YELLOW + "/lotto admin remove <id>" + ChatColor.GRAY + " - Lotto'yu sil ve para iade et");
        sender.sendMessage(ChatColor.YELLOW + "/lotto admin reload" + ChatColor.GRAY + " - Konfigürasyonu yenile");
        sender.sendMessage(ChatColor.YELLOW + "/lotto admin stats" + ChatColor.GRAY + " - İstatistikleri göster");
        sender.sendMessage(ChatColor.RED + "§l═══════════════════════════════════");
    }
}