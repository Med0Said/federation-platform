package com.federation;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for full Spring-context integration tests.
 * Uses H2 in-memory DB (profile: test, Flyway disabled).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Shared infrastructure injected by subclasses via @Autowired
}
