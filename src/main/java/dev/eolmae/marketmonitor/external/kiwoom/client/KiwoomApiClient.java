package dev.eolmae.marketmonitor.external.kiwoom.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eolmae.marketmonitor.exception.KiwoomApiException;
import dev.eolmae.marketmonitor.external.kiwoom.KiwoomApiConstants;
import dev.eolmae.marketmonitor.external.kiwoom.KiwoomRequest;
import dev.eolmae.marketmonitor.external.kiwoom.config.KiwoomProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomApiClient {

	private static final String BASE_URL = "https://api.kiwoom.com";

	private final KiwoomProperties properties;
	private final KiwoomTokenManager tokenManager;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RestClient restClient = RestClient.create();

	/**
	 * 타입 안전 API 호출. request DTO가 직렬화되어 요청 바디로 전송된다.
	 * 응답 전체(rt_cd, msg1 포함 root object)를 dataClass 타입으로 역직렬화해 반환한다.
	 * 호출마다 callIntervalMs 딜레이를 적용한다.
	 */
	public <T> T post(KiwoomRequest request, Class<T> dataClass) {
		log.debug("Kiwoom API 호출: apiId={}, path={}", request.apiId(), request.path());

		sleep(properties.callIntervalMs());

		JsonNode response = callApi(request.path(), request.apiId(), request);
		return objectMapper.convertValue(response, dataClass);
	}

	private JsonNode callApi(String path, String apiId, Object requestBody) {
		String raw = restClient.post()
			.uri(BASE_URL + path)
			.contentType(MediaType.APPLICATION_JSON)
			.header("authorization", "Bearer " + tokenManager.getToken())
			.header("appkey", properties.appKey())
			.header("secretkey", properties.secret())
			.header("api-id", apiId)
			.body(requestBody)
			.retrieve()
			.body(String.class);

		JsonNode response;
		try {
			response = objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new KiwoomApiException("Kiwoom API 응답 파싱 실패: " + raw, e);
		}

		String returnCode = response.path("return_code").asText();
		if (!KiwoomApiConstants.SUCCESS_CODE.equals(returnCode)) {
			log.warn("Kiwoom API 오류 응답: apiId={}, return_code={}, msg={}", apiId, returnCode, response.path("return_msg").asText());
			throw new KiwoomApiException("Kiwoom API 오류: " + response.path("return_msg").asText());
		}

		return response;
	}

	private void sleep(long millis) {
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
