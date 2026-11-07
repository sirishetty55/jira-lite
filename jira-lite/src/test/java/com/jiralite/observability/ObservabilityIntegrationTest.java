package com.jiralite.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.dto.request.RegisterRequest;
import com.jiralite.entity.User;
import com.jiralite.model.Role;
import com.jiralite.repository.UserRepository;
import com.jiralite.security.CorrelationIdFilter;
import com.jiralite.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    private String adminToken;
    private String devToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Observability")
                .email("admin.obs@test.com")
                .password("hashed_password")
                .role(Role.ADMIN)
                .active(true)
                .build());

        User dev = userRepository.save(User.builder()
                .firstName("Developer")
                .lastName("Observability")
                .email("dev.obs@test.com")
                .password("hashed_password")
                .role(Role.DEVELOPER)
                .active(true)
                .build());

        adminToken = tokenProvider.generateToken(admin.getEmail());
        devToken = tokenProvider.generateToken(dev.getEmail());
    }

    @Test
    @DisplayName("Correlation ID: Missing header automatically generates a random UUID")
    void missingCorrelationId_GeneratesUuid() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(UUID_PATTERN.matcher(correlationId).matches()).isTrue();
    }

    @Test
    @DisplayName("Correlation ID: Valid client-provided header is accepted and echoed back")
    void validCorrelationId_EchoesBack() throws Exception {
        String clientTraceId = "req-trace-valid_12345";

        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId));
    }

    @Test
    @DisplayName("Correlation ID: Malicious/CRLF injection is rejected and replaced with safe UUID")
    void maliciousCrlfCorrelationId_ReplacedWithUuid() throws Exception {
        // Direct filter resolution validation against CRLF
        String maliciousHeader = "test\r\nInjected-Header: evil";
        String resolvedDirectly = correlationIdFilter.resolveCorrelationId(maliciousHeader);
        assertThat(resolvedDirectly).isNotNull();
        assertThat(resolvedDirectly).doesNotContain("Injected-Header");
        assertThat(resolvedDirectly).doesNotContain("\r");
        assertThat(resolvedDirectly).doesNotContain("\n");
        assertThat(UUID_PATTERN.matcher(resolvedDirectly).matches()).isTrue();

        // HTTP request with invalid characters (spaces and symbols)
        String invalidHttpHeader = "invalid id with spaces & symbols!";
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, invalidHttpHeader))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).doesNotContain("invalid id");
        assertThat(UUID_PATTERN.matcher(correlationId).matches()).isTrue();
    }

    @Test
    @DisplayName("Correlation ID: Excessively long ID (>64 chars) is rejected and replaced with safe UUID")
    void excessivelyLongCorrelationId_ReplacedWithUuid() throws Exception {
        String longHeader = "a".repeat(128);

        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, longHeader))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).hasSize(36);
        assertThat(UUID_PATTERN.matcher(correlationId).matches()).isTrue();
    }

    @Test
    @DisplayName("Correlation ID: Special script characters are rejected and replaced with safe UUID")
    void scriptInjectionCorrelationId_ReplacedWithUuid() throws Exception {
        String scriptHeader = "<script>alert(1)</script>";

        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, scriptHeader))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).doesNotContain("<script>");
        assertThat(UUID_PATTERN.matcher(correlationId).matches()).isTrue();
    }

    @Test
    @DisplayName("Correlation ID: Included in structured error response on validation failure (400)")
    void errorResponse_IncludesCorrelationId_OnValidationFailure() throws Exception {
        String clientTraceId = "val-error-trace-001";
        RegisterRequest invalidRequest = new RegisterRequest("", "", "not-an-email", "");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        assertThat(jsonNode.has("correlationId")).isTrue();
        assertThat(jsonNode.get("correlationId").asText()).isEqualTo(clientTraceId);
        assertThat(jsonNode.get("status").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("error").asText()).isEqualTo("Validation Failed");
    }

    @Test
    @DisplayName("Correlation ID: Included in structured error response on 401 Unauthorized")
    void errorResponse_IncludesCorrelationId_OnUnauthorized() throws Exception {
        String clientTraceId = "unauth-trace-002";

        MvcResult result = mockMvc.perform(get("/api/projects")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        assertThat(jsonNode.has("correlationId")).isTrue();
        assertThat(jsonNode.get("correlationId").asText()).isEqualTo(clientTraceId);
        assertThat(jsonNode.get("status").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("MDC Cleanup: Thread MDC is clean before and after request dispatch")
    void mdcCleanup_MdcClearedAfterRequest() throws Exception {
        // Confirm MDC clean before request
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();

        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-mdc-cleanup"))
                .andExpect(status().isOk());

        // Confirm MDC clean after request
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Actuator Prometheus: Anonymous request returns 401 Unauthorized")
    void prometheus_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actuator Prometheus: Non-admin (DEVELOPER) returns 403 Forbidden")
    void prometheus_DeveloperRole_Returns403() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + devToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator Prometheus: Admin returns 200 with standard Prometheus metrics and no sensitive data")
    void prometheus_AdminRole_Returns200_WithMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).isNotEmpty();

        // Standard Prometheus exposition metric names
        assertThat(body).contains("jvm_memory_used_bytes");
        assertThat(body).contains("system_cpu_count");
        assertThat(body).contains("application_ready_time_seconds");

        // Verify sensitive credentials/secrets are not leaked in metric tags or names
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("dGVzdF9zZWNyZX");
        assertThat(body).doesNotContain("admin.obs@test.com");
    }
}
