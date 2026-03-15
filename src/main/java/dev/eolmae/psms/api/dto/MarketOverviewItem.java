package dev.eolmae.psms.api.dto;

import dev.eolmae.psms.domain.common.MarketType;
import java.math.BigDecimal;

public record MarketOverviewItem(
	MarketType marketType,
	String marketStatus,
	BigDecimal indexValue,
	BigDecimal changeValue,
	BigDecimal changeRate,
	BigDecimal tradingValue,
	int advancers,
	int decliners,
	int unchangedCount
) {
}
