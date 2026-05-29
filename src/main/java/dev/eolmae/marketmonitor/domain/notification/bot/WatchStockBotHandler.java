package dev.eolmae.marketmonitor.domain.notification.bot;

import dev.eolmae.marketmonitor.collector.WatchStockBackfillService;
import dev.eolmae.marketmonitor.external.telegram.TelegramClient;
import dev.eolmae.marketmonitor.domain.stock.StockMaster;
import dev.eolmae.marketmonitor.domain.stock.repository.StockMasterRepository;
import dev.eolmae.marketmonitor.domain.stock.WatchStock;
import dev.eolmae.marketmonitor.domain.stock.repository.WatchStockRepository;
import dev.eolmae.marketmonitor.domain.user.AppUser;
import dev.eolmae.marketmonitor.domain.user.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchStockBotHandler {

    private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final String CMD_ADD = "/add";
    private static final String CMD_DEL = "/del";
    private static final String CMD_LIST = "/list";
    private static final String CMD_LIST_SHORT = "/l";

    private final TelegramClient telegramClient;
    private final StockMasterRepository stockMasterRepository;
    private final WatchStockRepository watchStockRepository;
    private final AppUserRepository appUserRepository;
    private final WatchStockBackfillService watchStockBackfillService;

    private final ConcurrentHashMap<String, ConversationState> conversationStates = new ConcurrentHashMap<>();

    public void handle(String chatId, String text) {
        String trimmed = text.trim();

        ConversationState state = conversationStates.getOrDefault(chatId, ConversationState.IDLE);

        if (state == ConversationState.WAITING_FOR_ADD_STOCK) {
            conversationStates.put(chatId, ConversationState.IDLE);
            addWatchStock(chatId, trimmed);
            return;
        }

        if (state == ConversationState.WAITING_FOR_DEL_STOCK) {
            conversationStates.put(chatId, ConversationState.IDLE);
            deleteWatchStock(chatId, trimmed);
            return;
        }

        if (trimmed.equals(CMD_ADD)) {
            conversationStates.put(chatId, ConversationState.WAITING_FOR_ADD_STOCK);
            telegramClient.sendMessage(chatId, "등록할 종목명 또는 코드를 입력하세요.");
            return;
        }

        if (trimmed.startsWith(CMD_ADD + " ")) {
            String input = trimmed.substring(CMD_ADD.length() + 1).trim();
            addWatchStock(chatId, input);
            return;
        }

        if (trimmed.equals(CMD_DEL)) {
            conversationStates.put(chatId, ConversationState.WAITING_FOR_DEL_STOCK);
            telegramClient.sendMessage(chatId, "삭제할 종목명 또는 코드를 입력하세요.");
            return;
        }

        if (trimmed.startsWith(CMD_DEL + " ")) {
            String input = trimmed.substring(CMD_DEL.length() + 1).trim();
            deleteWatchStock(chatId, input);
            return;
        }

        if (trimmed.equals(CMD_LIST) || trimmed.equals(CMD_LIST_SHORT)) {
            listWatchStocks(chatId);
            return;
        }

        log.debug("처리되지 않은 메시지: chatId={}, text={}", chatId, trimmed);
    }

    private void addWatchStock(String chatId, String input) {
        Optional<StockMaster> stockOpt = resolveStock(input);

        if (stockOpt.isEmpty()) {
            telegramClient.sendMessage(chatId, "등록되지 않은 종목입니다.");
            return;
        }

        StockMaster stock = stockOpt.get();
        AppUser user = resolveUserByChatId(chatId);

        boolean alreadyRegistered = watchStockRepository
            .findByUserUserKeyAndStockStockCode(user.getUserKey(), stock.getStockCode())
            .isPresent();

        if (alreadyRegistered) {
            telegramClient.sendMessage(chatId,
                String.format("이미 등록된 종목입니다: %s (%s)", stock.getStockName(), stock.getStockCode()));
            return;
        }

        int nextOrder = watchStockRepository
            .findByUserUserKeyOrderByDisplayOrderAsc(user.getUserKey())
            .size() + 1;

        watchStockRepository.save(WatchStock.create(user, stock, nextOrder));

        telegramClient.sendMessage(chatId,
            String.format("관심종목에 추가되었습니다: %s (%s)", stock.getStockName(), stock.getStockCode()));
        log.info("관심종목 등록: userKey={}, stockCode={}", user.getUserKey(), stock.getStockCode());

        watchStockBackfillService.backfill(stock.getStockCode());
    }

    private void deleteWatchStock(String chatId, String input) {
        Optional<StockMaster> stockOpt = resolveStock(input);

        if (stockOpt.isEmpty()) {
            telegramClient.sendMessage(chatId, "등록되지 않은 종목입니다.");
            return;
        }

        StockMaster stock = stockOpt.get();
        AppUser user = resolveUserByChatId(chatId);

        Optional<WatchStock> watchStockOpt = watchStockRepository
            .findByUserUserKeyAndStockStockCode(user.getUserKey(), stock.getStockCode());

        if (watchStockOpt.isEmpty()) {
            telegramClient.sendMessage(chatId,
                String.format("관심종목에 없는 종목입니다: %s (%s)", stock.getStockName(), stock.getStockCode()));
            return;
        }

        watchStockRepository.delete(watchStockOpt.get());

        telegramClient.sendMessage(chatId,
            String.format("관심종목에서 삭제되었습니다: %s (%s)", stock.getStockName(), stock.getStockCode()));
        log.info("관심종목 삭제: userKey={}, stockCode={}", user.getUserKey(), stock.getStockCode());
    }

    private void listWatchStocks(String chatId) {
        AppUser user = resolveUserByChatId(chatId);

        List<WatchStock> watchStocks = watchStockRepository
            .findByUserUserKeyOrderByDisplayOrderAsc(user.getUserKey());

        if (watchStocks.isEmpty()) {
            telegramClient.sendMessage(chatId, "등록된 관심종목이 없습니다.");
            return;
        }

        StringBuilder sb = new StringBuilder("관심종목 목록:\n");
        for (int i = 0; i < watchStocks.size(); i++) {
            StockMaster stock = watchStocks.get(i).getStock();
            sb.append(String.format("%d. %s (%s)\n", i + 1, stock.getStockName(), stock.getStockCode()));
        }

        telegramClient.sendMessage(chatId, sb.toString().trim());
    }

    private Optional<StockMaster> resolveStock(String input) {
        if (STOCK_CODE_PATTERN.matcher(input).matches()) {
            return stockMasterRepository.findById(input);
        }
        return stockMasterRepository.findByStockName(input);
    }

    private AppUser resolveUserByChatId(String chatId) {
        return appUserRepository.findByTelegramChatId(chatId)
            .orElseThrow(() -> new IllegalStateException(
                "chatId에 해당하는 사용자가 없습니다: chatId=" + chatId));
    }
}
