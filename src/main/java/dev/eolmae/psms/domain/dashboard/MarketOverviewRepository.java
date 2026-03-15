package dev.eolmae.psms.domain.dashboard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketOverviewRepository extends JpaRepository<MarketOverview, Long> {

	List<MarketOverview> findAllByOrderByMarketTypeAsc();
}
