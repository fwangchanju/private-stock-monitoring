package dev.eolmae.marketmonitor.domain.dashboard.repository;
import dev.eolmae.marketmonitor.domain.dashboard.*;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketOverviewRepository extends JpaRepository<MarketOverview, Long> {

	List<MarketOverview> findAllByOrderByMarketTypeAsc();

	Optional<MarketOverview> findByMarketType(MarketType marketType);
}
