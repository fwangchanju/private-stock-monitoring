package dev.eolmae.psms.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShortSellingHistoryItem(
	LocalDate tradeDate,
	long shortVolume,
	BigDecimal shortAmount,
	BigDecimal shortRatio
) {
}
