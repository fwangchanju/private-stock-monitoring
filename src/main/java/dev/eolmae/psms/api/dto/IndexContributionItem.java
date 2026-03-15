package dev.eolmae.psms.api.dto;

import dev.eolmae.psms.domain.common.MarketType;
import java.math.BigDecimal;

public record IndexContributionItem(
	MarketType marketType,
	int rank,
	String stockCode,
	String stockName,
	BigDecimal contributionScore,
	BigDecimal priceChangeRate
) {
}
