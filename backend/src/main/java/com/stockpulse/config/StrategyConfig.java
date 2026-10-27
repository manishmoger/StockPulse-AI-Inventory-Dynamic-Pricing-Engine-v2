package com.stockpulse.config;

import com.stockpulse.strategy.CommerceAdvisor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class StrategyConfig {

    private final AtomicReference<String> activeStrategyName = new AtomicReference<>("AI");
    private final ApplicationContext applicationContext;

    public StrategyConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public CommerceAdvisor getActiveAdvisor() {
        return applicationContext.getBean(activeStrategyName.get(), CommerceAdvisor.class);
    }

    public String getActiveStrategyName() {
        return activeStrategyName.get();
    }

    public void setActiveStrategy(String name) {
        String normalized = name.toUpperCase(Locale.ROOT);
        try {
            applicationContext.getBean(normalized, CommerceAdvisor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown strategy: " + name
                    + ". Must match a @Component(\"NAME\") implementing CommerceAdvisor.", e);
        }
        activeStrategyName.set(normalized);
    }
}
