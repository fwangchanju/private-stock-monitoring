package dev.eolmae.psms.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchStockRepository extends JpaRepository<WatchStock, Long> {

	List<WatchStock> findByUserUserKeyOrderByDisplayOrderAsc(String userKey);

	@Query("SELECT DISTINCT ws.stock.stockCode FROM WatchStock ws")
	List<String> findDistinctStockCodes();
}
