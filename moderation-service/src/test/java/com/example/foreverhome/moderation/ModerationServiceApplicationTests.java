package com.example.foreverhome.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ModerationServiceApplicationTests {

    @MockitoBean
    OllamaChatModel ollamaChatModel;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
    }
}
