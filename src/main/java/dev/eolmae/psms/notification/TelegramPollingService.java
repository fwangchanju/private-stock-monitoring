package dev.eolmae.psms.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramPollingService {

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org";

    private final TelegramProperties telegramProperties;
    private final WatchStockBotHandler watchStockBotHandler;
    private final RestClient restClient = RestClient.create();

    private long offset = 0;

    @Scheduled(fixedDelay = 3000)
    public void pollUpdates() {
        try {
            String url = TELEGRAM_API_BASE
                + "/bot" + telegramProperties.botToken()
                + "/getUpdates?offset=" + offset;

            GetUpdatesResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(GetUpdatesResponse.class);

            if (response == null || !response.ok() || response.result() == null) {
                return;
            }

            for (Update update : response.result()) {
                processUpdate(update);
                offset = update.updateId() + 1;
            }
        } catch (Exception e) {
            log.error("텔레그램 업데이트 폴링 중 오류 발생", e);
        }
    }

    private void processUpdate(Update update) {
        if (update.message() == null) {
            return;
        }

        Message message = update.message();
        if (message.chat() == null || message.text() == null || message.text().isBlank()) {
            return;
        }

        String chatId = String.valueOf(message.chat().id());
        String allowedChatId = telegramProperties.chatId();

        if (!chatId.equals(allowedChatId)) {
            log.warn("허용되지 않은 chatId로부터 메시지 수신: chatId={}", chatId);
            return;
        }

        log.debug("텔레그램 메시지 수신: chatId={}, text={}", chatId, message.text());
        watchStockBotHandler.handle(chatId, message.text());
    }

    // --- Telegram API 응답 DTO ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GetUpdatesResponse(
        boolean ok,
        List<Update> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Update(
        @JsonProperty("update_id") long updateId,
        Message message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(
        @JsonProperty("message_id") long messageId,
        Chat chat,
        String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Chat(
        long id
    ) {}
}
