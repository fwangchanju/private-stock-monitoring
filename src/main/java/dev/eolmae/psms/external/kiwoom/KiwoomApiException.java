package dev.eolmae.psms.external.kiwoom;

import dev.eolmae.psms.exception.BusinessException;

public class KiwoomApiException extends BusinessException {

	public KiwoomApiException(String message) {
		super(message);
	}

	public KiwoomApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
