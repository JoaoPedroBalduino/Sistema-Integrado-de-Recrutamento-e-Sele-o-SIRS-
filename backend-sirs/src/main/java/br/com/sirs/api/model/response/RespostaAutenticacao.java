package br.com.sirs.api.model.response;

import java.time.Instant;

public record RespostaAutenticacao(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    RespostaUsuario user,
    String refreshToken) {}
