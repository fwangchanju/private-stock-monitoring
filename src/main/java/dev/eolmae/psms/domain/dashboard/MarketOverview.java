package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.MarketType;
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
@Table(name = "market_overview")
public class MarketOverview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 20)
	private MarketType marketType;

	@Column(nullable = false)
	private LocalDateTime snapshotTime;

	@Column(nullable = false)
	private LocalDateTime lastCollectedAt;

	@Column(nullable = false, length = 30)
	private String marketStatus;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal indexValue;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal changeValue;

	@Column(nullable = false, precision = 9, scale = 4)
	private BigDecimal changeRate;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal tradingValue;

	@Column(nullable = false)
	private int advancers;

	@Column(nullable = false)
	private int decliners;

	@Column(nullable = false)
	private int unchangedCount;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected MarketOverview() {
	}

	public static MarketOverview create(MarketType marketType, LocalDateTime snapshotTime,
		LocalDateTime lastCollectedAt, String marketStatus,
		BigDecimal indexValue, BigDecimal changeValue, BigDecimal changeRate,
		BigDecimal tradingValue, int advancers, int decliners, int unchangedCount) {
		var entity = new MarketOverview();
		entity.marketType = marketType;
		entity.snapshotTime = snapshotTime;
		entity.lastCollectedAt = lastCollectedAt;
		entity.marketStatus = marketStatus;
		entity.indexValue = indexValue;
		entity.changeValue = changeValue;
		entity.changeRate = changeRate;
		entity.tradingValue = tradingValue;
		entity.advancers = advancers;
		entity.decliners = decliners;
		entity.unchangedCount = unchangedCount;
		entity.createdAt = LocalDateTime.now();
		entity.updatedAt = LocalDateTime.now();
		return entity;
	}

	public void update(LocalDateTime snapshotTime, LocalDateTime lastCollectedAt,
		String marketStatus, BigDecimal indexValue, BigDecimal changeValue,
		BigDecimal changeRate, BigDecimal tradingValue,
		int advancers, int decliners, int unchangedCount) {
		this.snapshotTime = snapshotTime;
		this.lastCollectedAt = lastCollectedAt;
		this.marketStatus = marketStatus;
		this.indexValue = indexValue;
		this.changeValue = changeValue;
		this.changeRate = changeRate;
		this.tradingValue = tradingValue;
		this.advancers = advancers;
		this.decliners = decliners;
		this.unchangedCount = unchangedCount;
		this.updatedAt = LocalDateTime.now();
	}
}
