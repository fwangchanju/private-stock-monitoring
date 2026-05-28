package dev.eolmae.marketmonitor.common.enums;

import java.util.Arrays;
import java.util.List;

public enum MarketType {
	KOSPI,
	KOSDAQ,
	/** API 파라미터 전용 (DB 저장 금지) — KOSPI + KOSDAQ 통합 조회용 */
	ALL;

	/** DB에 저장 가능한 값만 반환 (ALL 제외) */
	public static List<MarketType> storableValues() {
		return Arrays.stream(values()).filter(m -> m != ALL).toList();
	}
}
