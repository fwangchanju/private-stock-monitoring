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

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal shortAmount;

	@Column(nullable = false, precision = 9, scale = 4)
	private BigDecimal shortRatio;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected ShortSellingHistory() {
	}

	public static ShortSellingHistory create(String stockCode, LocalDate tradeDate,
		long shortVolume, BigDecimal shortAmount, BigDecimal shortRatio) {
		var entity = new ShortSellingHistory();
		entity.stockCode = stockCode;
		entity.tradeDate = tradeDate;
		entity.shortVolume = shortVolume;
		entity.shortAmount = shortAmount;
		entity.shortRatio = shortRatio;
		entity.createdAt = LocalDateTime.now();
		return entity;
	}
}
