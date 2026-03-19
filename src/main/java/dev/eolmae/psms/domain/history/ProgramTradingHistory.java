package dev.eolmae.psms.domain.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "program_trading_history")
public class ProgramTradingHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String stockCode;

	@Column(nullable = false)
	private LocalDateTime snapshotTime;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programBuyAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programSellAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal programNetBuyAmount;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected ProgramTradingHistory() {
	}

	public static ProgramTradingHistory create(String stockCode, LocalDateTime snapshotTime,
		BigDecimal programBuyAmount, BigDecimal programSellAmount, BigDecimal programNetBuyAmount) {
		var entity = new ProgramTradingHistory();
		entity.stockCode = stockCode;
		entity.snapshotTime = snapshotTime;
		entity.programBuyAmount = programBuyAmount;
		entity.programSellAmount = programSellAmount;
		entity.programNetBuyAmount = programNetBuyAmount;
		entity.createdAt = LocalDateTime.now();
		return entity;
	}
}
