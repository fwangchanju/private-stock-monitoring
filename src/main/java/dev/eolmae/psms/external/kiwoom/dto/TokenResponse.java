package dev.eolmae.psms.external.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("expires_in") long expiresIn
) {
}
