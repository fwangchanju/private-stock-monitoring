package dev.eolmae.marketmonitor.api.dto;

import dev.eolmae.marketmonitor.common.enums.MarketType;

public record StockMasterItem(
	String stockCode,
	String stockName,
	MarketType marketType
) {}
