package dev.eolmae.psms.api.dto;

import dev.eolmae.psms.domain.common.MarketType;

public record WatchStockItem(
	String stockCode,
	String stockName,
	MarketType marketType,
	int displayOrder
) {
}
