package dev.eolmae.marketmonitor.api.dto;

import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.math.BigDecimal;

public record IntradayInvestorRankingItem(
	MarketType marketType,
	InvestorType investorType,
	int rank,
	String stockCode,
	String stockName,
	BigDecimal netBuyAmount,
	long tradedVolume
) {
}
