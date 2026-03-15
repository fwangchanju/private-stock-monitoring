package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorTradingSummarySnapshotRepository extends JpaRepository<InvestorTradingSummarySnapshot, Long> {

	Optional<InvestorTradingSummarySnapshot> findByMarketTypeAndInvestorTypeAndSnapshotTime(
		MarketType marketType, InvestorType investorType, LocalDateTime snapshotTime);
}
