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
