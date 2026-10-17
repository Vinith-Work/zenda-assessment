package com.example.feeplanner.system;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    private final HealthEndpoint healthEndpoint;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final Environment environment;

    public SystemController(HealthEndpoint healthEndpoint,
                            ObjectProvider<BuildProperties> buildProperties,
                            Environment environment) {
        this.healthEndpoint = healthEndpoint;
        this.buildProperties = buildProperties;
        this.environment = environment;
    }

    /**
     * Overall status plus each check's status, without details. HealthEndpoint.health() always
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        HealthComponent health = healthEndpoint.health();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", health.getStatus().getCode());
        if (health instanceof CompositeHealth composite) {
            Map<String, String> components = new LinkedHashMap<>();
            composite.getComponents().forEach((name, component) -> components.put(name, component.getStatus().getCode()));
            body.put("components", components);
        }

        HttpStatus status = Status.UP.equals(health.getStatus()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }

    @GetMapping("/api/info")
    public Map<String, Object> info() {
        BuildProperties build = buildProperties.getIfAvailable();
        long startMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", build != null ? build.getName() : environment.getProperty("spring.application.name"));
        info.put("version", build != null ? build.getVersion() : "unknown");
        info.put("buildTime", build != null ? build.getTime() : null);
        info.put("servedBy", hostname());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("startedAt", Instant.ofEpochMilli(startMillis));
        info.put("uptimeSeconds", Duration.ofMillis(System.currentTimeMillis() - startMillis).toSeconds());
        return info;
    }

    private static String hostname() {
        String name = System.getenv("HOSTNAME");
        if (name == null || name.isBlank()) {
            name = System.getenv("COMPUTERNAME"); // Windows
        }
        return name == null || name.isBlank() ? "unknown" : name;
    }
}
