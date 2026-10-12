package com.federation.auth;

import com.federation.common.mail.MailService;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth — forgot / reset password")
class PasswordResetIntegrationTest extends ApiTestSupport {

    @MockBean MailService mailService;

    @Test
    @DisplayName("reset link works once, revokes sessions, and unknown e-mails look identical")
    void fullFlow() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String email = "reset" + suffix + "@test.local";
        MvcResult signup = mockMvc.perform(jsonBody(post("/auth/register"), Map.of(
                "email", email, "username", "reset" + suffix, "password", "Before@123",
                "firstName", "Reset", "lastName", "Me"))).andExpect(status().isCreated()).andReturn();
        String refresh = data(signup).path("refreshToken").asText();

        mockMvc.perform(jsonBody(post("/auth/forgot-password"), Map.of("email", email.toUpperCase())))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(email), anyString(), bodyCaptor.capture());
        Matcher m = Pattern.compile("/auth/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(bodyCaptor.getValue());
        assertThat(m.find()).isTrue();
        String token = m.group(1);

        // Mismatched confirmation and weak passwords are rejected without consuming the token
        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", token, "newPassword", "After@1234", "confirmPassword", "Other@1234")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", token, "newPassword", "weak", "confirmPassword", "weak")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", token, "newPassword", "After@1234", "confirmPassword", "After@1234")))
                .andExpect(status().isOk());

        assertThat(login(email, "After@1234")).isNotBlank();
        mockMvc.perform(jsonBody(post("/auth/login"), Map.of("usernameOrEmail", email, "password", "Before@123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(jsonBody(post("/auth/refresh"), Map.of("refreshToken", refresh)))
                .andExpect(status().isUnauthorized());

        // Single use
        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", token, "newPassword", "Again@1234", "confirmPassword", "Again@1234")))
                .andExpect(status().isBadRequest());

        // Unknown address: same 200 answer, no e-mail
        reset(mailService);
        mockMvc.perform(jsonBody(post("/auth/forgot-password"), Map.of("email", "nobody@nowhere.test")))
                .andExpect(status().isOk());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("a newer reset request invalidates the previous link")
    void newerTokenWins() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String email = "twice" + suffix + "@test.local";
        mockMvc.perform(jsonBody(post("/auth/register"), Map.of(
                "email", email, "username", "twice" + suffix, "password", "Before@123",
                "firstName", "Twice", "lastName", "Asked"))).andExpect(status().isCreated());

        mockMvc.perform(jsonBody(post("/auth/forgot-password"), Map.of("email", email))).andExpect(status().isOk());
        mockMvc.perform(jsonBody(post("/auth/forgot-password"), Map.of("email", email))).andExpect(status().isOk());

        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(mailService, times(2)).send(eq(email), anyString(), bodies.capture());
        Pattern p = Pattern.compile("token=([A-Za-z0-9_-]+)");
        Matcher first = p.matcher(bodies.getAllValues().get(0));
        Matcher second = p.matcher(bodies.getAllValues().get(1));
        assertThat(first.find() && second.find()).isTrue();

        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", first.group(1), "newPassword", "After@1234", "confirmPassword", "After@1234")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(jsonBody(post("/auth/reset-password"), Map.of(
                "token", second.group(1), "newPassword", "After@1234", "confirmPassword", "After@1234")))
                .andExpect(status().isOk());
    }
}
