package com.skillexchange.service;

import com.skillexchange.dto.video.VideoMeetingTokenDto;
import com.skillexchange.dto.video.JaasParticipantDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Service
public class JaasTokenService {
    @Value("${app.jaas.enabled:false}")
    private boolean enabled;

    @Value("${app.jaas.app-id:}")
    private String appId;

    @Value("${app.jaas.api-key-id:}")
    private String apiKeyId;

    @Value("${app.jaas.private-key-path:}")
    private String privateKeyPath;

    @Value("${app.jaas.private-key:}")
    private String privateKeyInline;

    @Value("${app.jaas.jwt-ttl-minutes:15}")
    private int jwtTtlMinutes;

    @Value("${app.jaas.use-staging:false}")
    private boolean useStaging;

    private PrivateKey privateKey;

    @PostConstruct
    void loadConfiguration() {
        if (!enabled) return;
        if (isBlank(appId) || isBlank(apiKeyId)) {
            throw new IllegalStateException("JaaS is enabled but JAAS_APP_ID or JAAS_API_KEY_ID is missing");
        }
        String pem = privateKeyInline;
        if (isBlank(pem) && !isBlank(privateKeyPath)) {
            try {
                pem = Files.readString(Path.of(privateKeyPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read JAAS private key file", e);
            }
        }
        if (isBlank(pem)) {
            throw new IllegalStateException("JaaS is enabled but JAAS_PRIVATE_KEY or JAAS_PRIVATE_KEY_PATH is missing");
        }
        privateKey = parsePrivateKey(pem);
        if (jwtTtlMinutes < 1 || jwtTtlMinutes > 60) {
            throw new IllegalStateException("JAAS_JWT_TTL_MINUTES must be between 1 and 60");
        }
    }

    public boolean isConfigured() {
        return enabled && privateKey != null;
    }

    public VideoMeetingTokenDto createToken(JaasParticipantDto participant, String roomName, boolean moderator) {
        if (!isConfigured()) {
            throw new IllegalStateException("SkillSwap video is not configured. Set JaaS credentials before joining the meeting.");
        }
        if (isBlank(roomName) || roomName.contains("/") || roomName.contains("\\")) {
            throw new IllegalArgumentException("Invalid JaaS room name");
        }
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtTtlMinutes * 60L);
        String kid = apiKeyId.contains("/") ? apiKeyId : appId + "/" + apiKeyId;
        Map<String,Object> context = new LinkedHashMap<>();
        Map<String,Object> userContext = new LinkedHashMap<>();
        userContext.put("id", "skillswap-user-" + participant.id());
        userContext.put("name", safeName(participant));
        userContext.put("email", participant.email());
        userContext.put("moderator", moderator ? "true" : "false");
        context.put("user", userContext);
        context.put("features", Map.of(
                "recording", false,
                "livestreaming", false,
                "transcription", false,
                "outbound-call", true
        ));
        context.put("room", Map.of("regex", false));

        String jwt = Jwts.builder()
                .header().keyId(kid).type("JWT").and()
                .issuer("chat")
                .claim("aud", "jitsi")
                .subject(appId)
                .notBefore(Date.from(now.minusSeconds(5)))
                .expiration(Date.from(exp))
                .claim("room", roomName)
                .claim("context", context)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        return VideoMeetingTokenDto.builder()
                .appId(appId)
                .roomName(roomName)
                .jwt(jwt)
                .useStaging(useStaging)
                .build();
    }

    private static String safeName(JaasParticipantDto participant) {
        return !isBlank(participant.fullName()) ? participant.fullName() : participant.username();
    }

    private static PrivateKey parsePrivateKey(String pem) {
        try {
            String normalized = pem.replace("\r", "").trim();
            normalized = normalized.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("JAAS private key must be a PKCS#8 RSA private key PEM", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
