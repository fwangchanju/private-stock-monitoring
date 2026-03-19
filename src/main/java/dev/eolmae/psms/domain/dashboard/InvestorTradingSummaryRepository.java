package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorTradingSummaryRepository extends JpaRepository<InvestorTradingSummary, Long> {

	List<InvestorTradingSummary> findAllByOrderByMarketTypeAscInvestorTypeAsc();

	Optional<InvestorTradingSummary> findByMarketTypeAndInvestorType(MarketType marketType, InvestorType investorType);
}
