package com.konkerlabs.platform.registry.alerts.test.config;

import com.github.fakemongo.Fongo;
import com.konkerlabs.platform.registry.alerts.config.MongoAlertsConfig;
import com.mongodb.Mongo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoTestConfig extends MongoAlertsConfig {

    @Override
    protected String getDatabaseName() {
        return "registry-test";
    }

    @Override
    @Bean
    public Mongo mongo() throws Exception {
        return new Fongo("registry-test").getMongo();
    }
}
