package dev.eolmae.psms.api.dto;

import java.time.LocalTime;

public record NotificationSettingResponse(
	String userKey,
	boolean reminderEnabled,
	LocalTime reminderTime,
	String timezone
) {
}
