package dev.eolmae.marketmonitor.domain.history.repository;
import dev.eolmae.marketmonitor.domain.history.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortSellingSnapshotRepository extends JpaRepository<ShortSellingSnapshot, Long> {

	boolean existsByStockCodeAndSnapshotTime(String stockCode, LocalDateTime snapshotTime);

	List<ShortSellingSnapshot> findByStockCodeAndTradeDateOrderBySnapshotTimeDesc(String stockCode, LocalDate tradeDate);
}
