package dev.eolmae.psms.domain.stock;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

	Optional<StockMaster> findByStockName(String stockName);
}
