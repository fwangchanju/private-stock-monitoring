package dev.eolmae.psms.external.kiwoom;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public class KiwoomResponseParser {

	private KiwoomResponseParser() {
	}

	public static BigDecimal parseBigDecimal(JsonNode node, String fieldName) {
		String raw = node.path(fieldName).asText("0").replace(",", "").trim();
		if (raw.isEmpty() || raw.equals("-")) {
			return BigDecimal.ZERO;
		}
		// 키움 API 일부 필드는 "--180311" 형태로 음수를 표현 (부호 prefix + 숫자)
		// 앞의 여분 '-' 제거 후 "-180311" 로 변환
		if (raw.startsWith("--")) {
			raw = raw.substring(1);
		}
		try {
			return new BigDecimal(raw);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	public static long parseLong(JsonNode node, String fieldName) {
		String raw = node.path(fieldName).asText("0").replace(",", "").trim();
		if (raw.isEmpty() || raw.equals("-")) {
			return 0L;
		}
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public static int parseInt(JsonNode node, String fieldName) {
		String raw = node.path(fieldName).asText("0").replace(",", "").trim();
		if (raw.isEmpty() || raw.equals("-")) {
			return 0;
		}
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static String parseString(JsonNode node, String fieldName) {
		return node.path(fieldName).asText("").trim();
	}
}
