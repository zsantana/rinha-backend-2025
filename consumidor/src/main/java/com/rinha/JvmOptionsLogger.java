package com.rinha;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Startup;

@ApplicationScoped
@Startup
public class JvmOptionsLogger {

    private static final Logger log = LoggerFactory.getLogger(JvmOptionsLogger.class);

    @PostConstruct
    void logJvmArguments() {
        var jvmArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        log.info("🚀 JVM Arguments:");
        jvmArgs.forEach(arg -> log.info("  {}", arg));
    }
}

