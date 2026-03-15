package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.ProgramRankingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "program_trading_ranking_snapshot")
public class ProgramTradingRankingSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProgramRankingType rankingType;

	@Column(name = "rank_no", nullable = false)
	private int rank;

	@Column(nullable = false, length = 20)
	private String stockCode;

	@Column(nullable = false, length = 100)
	private String stockName;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programBuyAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programSellAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programNetBuyAmount;

	@Column(nullable = false)
	private LocalDateTime snapshotTime;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected ProgramTradingRankingSnapshot() {
	}
}
