package com.hazely.senusboard.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Holds the one-time Admin bootstrap configuration. */
@Configuration
@ConfigurationProperties(prefix = "app.admin.bootstrap")
@Getter
@Setter
public class AdminBootstrapProperties {

    private boolean isEnabled;
    private String email;
    private String password;
    private String name;
    private String organization;
}
