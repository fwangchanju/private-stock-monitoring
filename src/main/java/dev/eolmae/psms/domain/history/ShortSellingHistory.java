package dev.eolmae.psms.domain.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "short_selling_history")
public class ShortSellingHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String stockCode;

	@Column(nullable = false)
	private LocalDate tradeDate;

	@Column(nullable = false)
	private long shortVolume;

	@Column(nullable = false)
	private long shortBalanceVolume;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal shortAmount;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal shortAvgPrice;

	@Column(nullable = false, precision = 9, scale = 4)
	private BigDecimal shortRatio;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal closePrice;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal priceChange;

	@Column(nullable = false, precision = 9, scale = 4)
	private BigDecimal changeRate;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected ShortSellingHistory() {
	}

	public static ShortSellingHistory create(String stockCode, LocalDate tradeDate,
		long shortVolume, long shortBalanceVolume,
		BigDecimal shortAmount, BigDecimal shortAvgPrice, BigDecimal shortRatio,
		BigDecimal closePrice, BigDecimal priceChange, BigDecimal changeRate) {
		var entity = new ShortSellingHistory();
		entity.stockCode = stockCode;
		entity.tradeDate = tradeDate;
		entity.shortVolume = shortVolume;
		entity.shortBalanceVolume = shortBalanceVolume;
		entity.shortAmount = shortAmount;
		entity.shortAvgPrice = shortAvgPrice;
		entity.shortRatio = shortRatio;
		entity.closePrice = closePrice;
		entity.priceChange = priceChange;
		entity.changeRate = changeRate;
		entity.createdAt = LocalDateTime.now();
		return entity;
	}

	public void update(long shortVolume, long shortBalanceVolume,
		BigDecimal shortAmount, BigDecimal shortAvgPrice, BigDecimal shortRatio,
		BigDecimal closePrice, BigDecimal priceChange, BigDecimal changeRate) {
		this.shortVolume = shortVolume;
		this.shortBalanceVolume = shortBalanceVolume;
		this.shortAmount = shortAmount;
		this.shortAvgPrice = shortAvgPrice;
		this.shortRatio = shortRatio;
		this.closePrice = closePrice;
		this.priceChange = priceChange;
		this.changeRate = changeRate;
	}
}
