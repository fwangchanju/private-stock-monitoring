package dev.eolmae.psms.domain.common;

public enum InvestorType {
	// ka10051: 업종별투자자순매수 7종
	PERSONAL(null),              // 개인 (ind_netprps)
	FOREIGNER("9000"),           // 외국인 (frgnr_netprps)
	INSTITUTION("9999"),         // 기관계 (orgn_netprps)
	FINANCIAL_INVESTMENT("1000"), // 금융투자 (sc_netprps)
	TRUST("3000"),               // 투신 (invtrt_netprps)
	PENSION_FUND("6000"),        // 연기금등 (endw_netprps)
	PRIVATE_FUND(null),          // 사모펀드 (samo_fund_netprps)

	// ka10065: 장중투자자별매매상위 추가 6종
	INSURANCE("2000"),           // 보험
	BANK("4000"),                // 은행
	OTHER_FINANCE("5000"),       // 기타금융
	GOVERNMENT("7000"),          // 국가
	OTHER_CORP("7100"),          // 기타법인
	FOREIGN_COMPANY("9100");     // 외국계

	private final String ka10065Code;

	InvestorType(String ka10065Code) {
		this.ka10065Code = ka10065Code;
	}

	public boolean hasKa10065Code() {
		return ka10065Code != null;
	}

	public String ka10065Code() {
		if (ka10065Code == null) {
			throw new IllegalStateException("ka10065 미지원 투자자 타입: " + this);
		}
		return ka10065Code;
	}
}
