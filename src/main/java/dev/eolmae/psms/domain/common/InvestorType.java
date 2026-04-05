package dev.eolmae.psms.domain.common;

public enum InvestorType {
	// ka10051: 업종별투자자순매수 7종
	PERSONAL,           // 개인 (ind_netprps)
	FOREIGNER,          // 외국인 (frgnr_netprps) / ka10065 9000
	INSTITUTION,        // 기관계 (orgn_netprps) / ka10065 9999
	FINANCIAL_INVESTMENT, // 금융투자 (sc_netprps) / ka10065 1000
	TRUST,              // 투신 (invtrt_netprps) / ka10065 3000
	PENSION_FUND,       // 연기금등 (endw_netprps) / ka10065 6000
	PRIVATE_FUND,       // 사모펀드 (samo_fund_netprps)

	// ka10065: 장중투자자별매매상위 추가 6종
	INSURANCE,          // 보험 (orgn_tp=2000)
	BANK,               // 은행 (orgn_tp=4000)
	OTHER_FINANCE,      // 기타금융 (orgn_tp=5000)
	GOVERNMENT,         // 국가 (orgn_tp=7000)
	OTHER_CORP,         // 기타법인 (orgn_tp=7100)
	FOREIGN_COMPANY     // 외국계 (orgn_tp=9100)
}
