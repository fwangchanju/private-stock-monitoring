package dev.eolmae.psms.notification;

import dev.eolmae.psms.domain.stock.StockMaster;
import dev.eolmae.psms.domain.stock.StockMasterRepository;
import dev.eolmae.psms.domain.stock.WatchStock;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.domain.user.AppUser;
import dev.eolmae.psms.domain.user.AppUserRepository;
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

    private final TelegramClient telegramClient;
    private final StockMasterRepository stockMasterRepository;
    private final WatchStockRepository watchStockRepository;
    private final AppUserRepository appUserRepository;

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

        if (trimmed.equals("/add")) {
            conversationStates.put(chatId, ConversationState.WAITING_FOR_ADD_STOCK);
            telegramClient.sendMessage(chatId, "등록할 종목명 또는 코드를 입력하세요.");
            return;
        }

        if (trimmed.startsWith("/add ")) {
            String input = trimmed.substring(5).trim();
            addWatchStock(chatId, input);
            return;
        }

        if (trimmed.equals("/del")) {
            conversationStates.put(chatId, ConversationState.WAITING_FOR_DEL_STOCK);
            telegramClient.sendMessage(chatId, "삭제할 종목명 또는 코드를 입력하세요.");
            return;
        }

        if (trimmed.startsWith("/del ")) {
            String input = trimmed.substring(5).trim();
            deleteWatchStock(chatId, input);
            return;
        }

        if (trimmed.equals("/list") || trimmed.equals("/l")) {
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
