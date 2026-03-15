package dev.eolmae.psms.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchStockRepository extends JpaRepository<WatchStock, Long> {

	List<WatchStock> findByUserUserKeyOrderByDisplayOrderAsc(String userKey);
}
