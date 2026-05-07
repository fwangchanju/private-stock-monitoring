package dev.eolmae.psms.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShortSellingHistoryItem(
	LocalDate tradeDate,
	long shortVolume,
	long shortBalanceVolume,
	BigDecimal shortAmount,
	BigDecimal shortAvgPrice,
	BigDecimal shortRatio,
	BigDecimal closePrice,
	BigDecimal priceChange,
	BigDecimal changeRate
) {
}
