package org.ozaii.maglotto.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.ozaii.maglotto.lotto.LottoGame;
import org.ozaii.maglotto.lotto.LottoManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LottoTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        // İlk argüman - ana komutlar
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("join");
            subcommands.add("leave");
            subcommands.add("list");
            subcommands.add("info");
            subcommands.add("help");

            if (sender.hasPermission("lotto.admin")) {
                subcommands.add("admin");
            }

            return filterAndSort(subcommands, args[0]);
        }

        // İkinci argüman
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "join":
                    // Sadece katılınabilir lottolar
                    return getJoinableLottoIds(args[1]);

                case "leave":
                    // Oyuncunun katıldığı lottolar (şimdilik tüm aktif lottolar)
                    return getActiveLottoIds(args[1]);

                case "info":
                    // Tüm aktif lottolar
                    return getActiveLottoIds(args[1]);

                case "admin":
                    if (sender.hasPermission("lotto.admin")) {
                        List<String> adminCommands = Arrays.asList(
                                "create", "end", "finish", "remove", "delete",
                                "cancel", "reload", "stats"
                        );
                        return filterAndSort(adminCommands, args[1]);
                    }
                    break;
            }
        }

        // Üçüncü argüman
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String adminAction = args[1].toLowerCase();

            if (subCommand.equals("admin") && sender.hasPermission("lotto.admin")) {
                switch (adminAction) {
                    case "end":
                    case "finish":
                        // Bitirilebilir lottolar
                        return getActiveLottoIds(args[2]);

                    case "remove":
                    case "delete":
                    case "cancel":
                        // Silinebilir lottolar
                        return getActiveLottoIds(args[2]);

                    case "info":
                        // Tüm lottolar
                        return getActiveLottoIds(args[2]);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Katılınabilir lotto ID'lerini döndürür
     */
    private List<String> getJoinableLottoIds(String input) {
        List<String> ids = new ArrayList<>();

        for (LottoGame game : LottoManager.getInstance().getActiveLottos()) {
            // Lotto henüz başlamamışsa katılınabilir
            if (!game.isStarted()) {
                ids.add(String.valueOf(game.getId()));
            }
        }

        return filterAndSort(ids, input);
    }

    /**
     * Aktif lotto ID'lerini döndürür
     */
    private List<String> getActiveLottoIds(String input) {
        List<String> ids = new ArrayList<>();

        for (LottoGame game : LottoManager.getInstance().getActiveLottos()) {
            ids.add(String.valueOf(game.getId()));
        }

        return filterAndSort(ids, input);
    }

    /**
     * Listeyi filtreler ve sıralar
     */
    private List<String> filterAndSort(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return options.stream().sorted().collect(Collectors.toList());
        }

        String lowerInput = input.toLowerCase();

        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Akıllı öneri sistemi
     */
    private List<String> getSmartSuggestions(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        // Oyuncunun geçmiş davranışlarına göre öneriler
        if (args.length == 1) {
            // En çok kullanılan komutları öne çıkar
            if (LottoManager.getInstance().getActiveLottos().size() > 0) {
                suggestions.add("join");
                suggestions.add("list");
            }
            suggestions.add("help");
        }

        return suggestions;
    }
}