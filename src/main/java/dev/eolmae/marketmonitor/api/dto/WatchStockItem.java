package dev.eolmae.marketmonitor.api.dto;

import dev.eolmae.marketmonitor.common.enums.MarketType;

public record WatchStockItem(
	String stockCode,
	String stockName,
	MarketType marketType,
	int displayOrder
) {
}
