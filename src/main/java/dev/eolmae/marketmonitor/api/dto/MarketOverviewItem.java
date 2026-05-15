package dev.eolmae.marketmonitor.api.dto;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.math.BigDecimal;

public record MarketOverviewItem(
	MarketType marketType,
	String marketStatus,
	BigDecimal indexValue,
	BigDecimal changeValue,
	BigDecimal changeRate,
	BigDecimal tradingValue,
	int upperLimitCount,
	int lowerLimitCount,
	int advancers,
	int decliners,
	int unchangedCount
) {
}
