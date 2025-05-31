package org.ozaii.maglotto.lotto;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.ozaii.maglotto.MagLotto;
import org.ozaii.maglotto.managers.ConfigManager;
import org.ozaii.maglotto.utils.JsonLogHelper;
import org.ozaii.maglotto.utils.VaultUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LottoManager {

    private static volatile LottoManager instance;

    public static LottoManager getInstance() {
        if (instance == null) {
            synchronized (LottoManager.class) {
                if (instance == null) {
                    instance = new LottoManager();
                }
            }
        }
        return instance;
    }

    private final Map<Integer, LottoGame> activeGames = new ConcurrentHashMap<>();
    private final Map<Integer, BukkitTask> lottoTasks = new ConcurrentHashMap<>();
    private ConfigManager configManager;
    private JsonLogHelper logHelper;
    private int lottoIdCounter = 1;
    private BukkitTask autoCreateTask;

    private LottoManager() {
        // Boş constructor
    }

    public void initialize() {
        configManager = ConfigManager.getInstance();
        logHelper = new JsonLogHelper();
        lottoIdCounter = configManager.getStartingId();
        startAutoLottoCreator();
    }

    public LottoGame createNewLotto() {
        int id = lottoIdCounter++;
        LottoGame game = new LottoGame(
                id,
                configManager.getStartDelay(),
                false,
                configManager.getRewardAmount(),
                configManager.getMinPlayers(),
                configManager.getMaxPlayers()
        );

        activeGames.put(id, game);

        MagLotto.getInstance().getLogger().info("Yeni lotto oluşturuldu: #" + id);

        // Oyunculara duyuru
        broadcastNewLotto(game);

        // Log kaydı
        logHelper.logLottoCreated(id, game.getRewardAmount(), game.getMinPlayers(),
                configManager.getEntryCost(), configManager.getStartDelay());

        // Otomatik başlatma zamanlayıcısını başlat
        scheduleLottoLifecycle(game);

        return game;
    }

    private void broadcastNewLotto(LottoGame game) {
        String formattedTitle = configManager.getLottoMessageTitle().replace("%lotto%", "Lotto #" + game.getId());
        String formattedSubtitle = configManager.getLottoMessageSubtitle();

        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            player.sendMessage("§6§l═══════════════════════════════════");
            player.sendMessage("§e§l🎰 YENİ LOTTO BAŞLADI! 🎰");
            player.sendMessage("");
            player.sendMessage("§7Lotto ID: §a#" + game.getId());
            player.sendMessage("§7Ödül: §6$" + game.getRewardAmount());
            player.sendMessage("§7Katılım Ücreti: §c$" + configManager.getEntryCost());
            player.sendMessage("§7Minimum Oyuncu: §b" + game.getMinPlayers());
            player.sendMessage("§7Başlama Süresi: §e" + configManager.getStartDelay() + " saniye");
            player.sendMessage("");
            player.sendMessage("§a/lotto join " + game.getId() + " §7- Katılmak için");
            player.sendMessage("§6§l═══════════════════════════════════");

            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
            player.playSound(player.getLocation(), "block.note_block.pling", 1.0f, 1.5f);

            player.sendTitle(formattedTitle, formattedSubtitle, 10, 60, 20);
        });

        Bukkit.broadcastMessage("§a[MagLotto] §eYeni lotto oluşturuldu! ID: #" + game.getId() + " | Ödül: $" + game.getRewardAmount());
    }

    private void scheduleLottoLifecycle(LottoGame game) {
        final int gameId = game.getId();

        // Başlama zamanlayıcısı
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                LottoGame current = getLotto(gameId);
                if (current == null) {
                    cancel();
                    return;
                }

                if (current.canStart()) {
                    // Yeterli oyuncu var, lotto'yu başlat ve bitir
                    Bukkit.broadcastMessage("§a[MagLotto] §eLotto #" + gameId + " yeterli oyuncuya ulaştı, çekiliş yapılıyor!");
                    finishLotto(gameId);
                } else if (configManager.isCancelIfNotEnoughPlayers()) {
                    // Yeterli oyuncu yok, iptal et
                    Bukkit.broadcastMessage("§c[MagLotto] §eLotto #" + gameId + " yeterli oyuncuya ulaşılamadı, iptal ediliyor.");
                    logHelper.logLottoCancelled(gameId, "Başlama süresi doldu, yeterli oyuncu yok", current.getPlayers().size());

                    // Katılımcılara para iade et
                    refundPlayers(current);
                    deleteLotto(gameId);
                }

                // Task'ı temizle
                lottoTasks.remove(gameId);
                cancel();
            }
        }.runTaskLater(MagLotto.getInstance(), configManager.getStartDelay() * 20L);

        lottoTasks.put(gameId, task);
    }

    private void refundPlayers(LottoGame game) {
        Economy eco = VaultUtil.getEconomy();
        double entryCost = configManager.getEntryCost();

        for (UUID uuid : game.getPlayers()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            eco.depositPlayer(player, entryCost);

            if (player.isOnline()) {
                player.getPlayer().sendMessage("§e[MagLotto] §aLotto #" + game.getId() + " iptal edildi, katılım ücretiniz iade edildi: $" + entryCost);
            }
        }
    }

    public void deleteLotto(int id) {
        activeGames.remove(id);

        // Varsa task'ı iptal et
        BukkitTask task = lottoTasks.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        MagLotto.getInstance().getLogger().info("Lotto #" + id + " silindi.");
    }

    public Collection<LottoGame> getActiveLottos() {
        return Collections.unmodifiableCollection(activeGames.values());
    }

    public LottoGame getLotto(int id) {
        return activeGames.get(id);
    }

    public boolean addPlayerToLotto(int lottoId, UUID playerUUID) {
        LottoGame game = getLotto(lottoId);
        if (game != null && game.addPlayer(playerUUID)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
            logHelper.logPlayerJoined(lottoId, playerUUID, player.getName());

            if (configManager.isAutoStartWhenFull() && game.canStart()) {
                Bukkit.broadcastMessage("§a[MagLotto] §eLotto #" + lottoId + " minimum oyuncu sayısına ulaştı! " + configManager.getQuickStartDelay() + " saniye sonra başlayacak!");

                BukkitTask currentTask = lottoTasks.remove(lottoId);
                if (currentTask != null && !currentTask.isCancelled()) {
                    currentTask.cancel();
                }

                int quickStartDelay = configManager.getQuickStartDelay();
                BukkitTask quickStartTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        LottoGame current = getLotto(lottoId);
                        if (current != null && current.canStart()) {
                            Bukkit.broadcastMessage("§a[MagLotto] §eLotto #" + lottoId + " başlıyor!");
                            finishLotto(lottoId);
                        }
                        lottoTasks.remove(lottoId);
                    }
                }.runTaskLater(MagLotto.getInstance(), quickStartDelay * 20L); // Yapılandırılabilir süre

                lottoTasks.put(lottoId, quickStartTask);
            }

            return true;
        }
        return false;
    }

    public boolean removePlayerFromLotto(int lottoId, UUID playerUUID) {
        LottoGame game = getLotto(lottoId);
        if (game != null && game.removePlayer(playerUUID)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
            logHelper.logPlayerLeft(lottoId, playerUUID, player.getName());
            return true;
        }
        return false;
    }

    public void finishLotto(int lottoId) {
        LottoGame game = getLotto(lottoId);
        if (game == null) return;

        Set<UUID> players = game.getPlayers();
        if (players.isEmpty()) {
            Bukkit.broadcastMessage("§c[MagLotto] §eLotto #" + lottoId + " kimse katılmadığı için iptal edildi.");
            logHelper.logLottoCancelled(lottoId, "Katılımcı yok", 0);
            deleteLotto(lottoId);
            return;
        }

        // Lotto'yu bitir olarak işaretle
        game.finish();

        UUID winnerUUID = pickRandomPlayer(players);
        if (winnerUUID != null) {
            game.setWinner(winnerUUID);
            double reward = game.getRewardAmount();

            OfflinePlayer winner = Bukkit.getOfflinePlayer(winnerUUID);
            VaultUtil.getEconomy().depositPlayer(winner, reward);

            // Kazanan duyurusu
            Bukkit.broadcastMessage("§6§l═══════════════════════════════════");
            Bukkit.broadcastMessage("§e§l🎰 LOTTO SONUÇLANDI! 🎰");
            Bukkit.broadcastMessage("§a§lKazanan: §e§l" + winner.getName());
            Bukkit.broadcastMessage("§a§lÖdül: §6§l$" + reward);
            Bukkit.broadcastMessage("§7Lotto #" + lottoId + " - Katılımcı: " + players.size());
            Bukkit.broadcastMessage("§6§l═══════════════════════════════════");

            // Kazanana özel mesaj
            if (winner.isOnline()) {
                Player winnerPlayer = winner.getPlayer();
                winnerPlayer.sendTitle("§6§lTEBRİKLER!", "§a$" + reward + " kazandınız!", 20, 100, 20);
                winnerPlayer.playSound(winnerPlayer.getLocation(), "entity.player.levelup", 2.0f, 1.0f);
            }

            List<UUID> participantList = new ArrayList<>(players);
            List<String> participantNames = participantList.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .collect(Collectors.toList());

            logHelper.logLottoFinished(lottoId, winnerUUID, winner.getName(), reward, participantList, participantNames);
        }

        // Lotto'yu sil
        deleteLotto(lottoId);
    }

    private void startAutoLottoCreator() {
        if (autoCreateTask != null && !autoCreateTask.isCancelled()) {
            autoCreateTask.cancel();
        }

        autoCreateTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Sadece aktif lotto sayısı az ise yeni lotto oluştur
                if (activeGames.size() < 3) { // Max 3 aktif lotto
                    createNewLotto();
                }
            }
        }.runTaskTimer(MagLotto.getInstance(), 0L, configManager.getAutoCreateInterval() * 60L * 20L);
    }

    private UUID pickRandomPlayer(Set<UUID> players) {
        List<UUID> list = new ArrayList<>(players);
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }

    public void sendNewLottoMessage(Player player, int id) {
        LottoGame game = getLotto(id);
        if (game == null) return;

        String formattedTitle = configManager.getLottoMessageTitle().replace("%lotto%", "Lotto #" + id);
        String formattedSubtitle = configManager.getLottoMessageSubtitle();

        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("§e§l🎰 YENİ LOTTO BAŞLADI! 🎰");
        player.sendMessage("");
        player.sendMessage("§7Lotto ID: §a#" + id);
        player.sendMessage("§7Ödül: §6$" + game.getRewardAmount());
        player.sendMessage("§7Katılım Ücreti: §c$" + configManager.getEntryCost());
        player.sendMessage("§7Minimum Oyuncu: §b" + configManager.getMinPlayers());
        player.sendMessage("§7Başlama Süresi: §e" + game.getStartDelay() + " saniye");
        player.sendMessage("");
        player.sendMessage("§a/lotto join " + id + " §7- Katılmak için");
        player.sendMessage("§6§l═══════════════════════════════════");

        player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
        player.playSound(player.getLocation(), "block.note_block.pling", 1.0f, 1.5f);

        player.sendTitle(formattedTitle, formattedSubtitle, 10, 60, 20);
    }

    // Getter metodları ConfigManager'dan alıyor
    public double getEntryCost() { return configManager.getEntryCost(); }
    public int getStartDelay() { return configManager.getStartDelay(); }
    public double getRewardAmount() { return configManager.getRewardAmount(); }
    public int getMinPlayers() { return configManager.getMinPlayers(); }
    public int getAutoCreateInterval() { return configManager.getAutoCreateInterval(); }
    public JsonLogHelper getLogHelper() { return logHelper; }
    public int getLastLottoId() { return lottoIdCounter - 1; }
    public boolean lottoJoinMessage() { return configManager.isLottoJoinMessage(); }

    public void shutdown() {
        // Tüm task'ları iptal et
        lottoTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        lottoTasks.clear();

        if (autoCreateTask != null && !autoCreateTask.isCancelled()) {
            autoCreateTask.cancel();
        }

        // Aktif lottolardaki oyunculara para iade et
        for (LottoGame game : activeGames.values()) {
            refundPlayers(game);
        }

        activeGames.clear();
    }
}