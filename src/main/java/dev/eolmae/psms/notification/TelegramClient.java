package dev.eolmae.psms.notification;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramClient {

	private static final String BASE_URL = "https://api.telegram.org";

	private final TelegramProperties properties;
	private final RestClient restClient = RestClient.create();

	public void sendMessage(String chatId, String text) {
		try {
			restClient.post()
				.uri(BASE_URL + "/bot" + properties.botToken() + "/sendMessage")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
					"chat_id", chatId,
					"text", text,
					"parse_mode", "HTML"
				))
				.retrieve()
				.toBodilessEntity();

			log.debug("텔레그램 메시지 발송 완료: chatId={}", chatId);
		} catch (Exception e) {
			log.error("텔레그램 메시지 발송 실패: chatId={}", chatId, e);
		}
	}
}
