package com.hazely.senusboard.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/** Configures authentication rules that vary between deployments. */
@Configuration
@ConfigurationProperties(prefix = "app.auth")
@Getter
@Setter
public class AuthProperties {

    private List<String> emailDomains = new ArrayList<>();
}
