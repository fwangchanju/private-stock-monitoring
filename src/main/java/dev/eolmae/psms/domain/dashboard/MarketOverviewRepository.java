package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.MarketType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketOverviewRepository extends JpaRepository<MarketOverview, Long> {

	List<MarketOverview> findAllByOrderByMarketTypeAsc();

	Optional<MarketOverview> findByMarketType(MarketType marketType);
}
