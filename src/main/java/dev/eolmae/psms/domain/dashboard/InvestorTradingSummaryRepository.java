package dev.eolmae.psms.domain.dashboard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorTradingSummaryRepository extends JpaRepository<InvestorTradingSummary, Long> {

	List<InvestorTradingSummary> findAllByOrderByMarketTypeAscInvestorTypeAsc();
}
