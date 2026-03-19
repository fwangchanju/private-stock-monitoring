package dev.eolmae.psms.external.kiwoom;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kiwoom")
public record KiwoomProperties(String appKey, String secret) {
}
