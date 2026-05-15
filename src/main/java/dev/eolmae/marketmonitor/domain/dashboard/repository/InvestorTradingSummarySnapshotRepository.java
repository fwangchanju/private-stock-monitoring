package dev.eolmae.marketmonitor.domain.dashboard.repository;
import dev.eolmae.marketmonitor.domain.dashboard.*;

import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorTradingSummarySnapshotRepository extends JpaRepository<InvestorTradingSummarySnapshot, Long> {

	Optional<InvestorTradingSummarySnapshot> findByMarketTypeAndInvestorTypeAndSnapshotTime(
		MarketType marketType, InvestorType investorType, LocalDateTime snapshotTime);
}
