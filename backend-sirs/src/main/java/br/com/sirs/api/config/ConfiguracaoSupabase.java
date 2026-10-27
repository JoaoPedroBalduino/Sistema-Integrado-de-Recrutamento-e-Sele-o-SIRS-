package br.com.sirs.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "sirs.auth.provider", havingValue = "supabase")
@EnableConfigurationProperties(PropriedadesSupabase.class)
public class ConfiguracaoSupabase {}
