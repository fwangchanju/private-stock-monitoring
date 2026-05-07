package dev.eolmae.psms.domain.stock;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchStockRepository extends JpaRepository<WatchStock, Long> {

	List<WatchStock> findByUserUserKeyOrderByDisplayOrderAsc(String userKey);

	@Query("SELECT DISTINCT ws.stock.stockCode FROM WatchStock ws")
	List<String> findDistinctStockCodes();

	Optional<WatchStock> findByUserUserKeyAndStockStockCode(String userKey, String stockCode);
}
