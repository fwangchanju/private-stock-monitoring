package dev.eolmae.marketmonitor.domain.dashboard.repository;
import dev.eolmae.marketmonitor.domain.dashboard.*;

import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorTradingSummaryRepository extends JpaRepository<InvestorTradingSummary, Long> {

	List<InvestorTradingSummary> findAllByOrderByMarketTypeAscInvestorTypeAsc();

	Optional<InvestorTradingSummary> findByMarketTypeAndInvestorType(MarketType marketType, InvestorType investorType);
}
