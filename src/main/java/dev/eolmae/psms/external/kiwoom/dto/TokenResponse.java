package dev.eolmae.psms.external.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("token") String token,
	@JsonProperty("expires_dt") String expiresAt  // yyyyMMddHHmmss
) {
}
