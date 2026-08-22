package com.dailymate.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityHandlers {

    private final ObjectMapper objectMapper;

    public ApiSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) ->
                writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) ->
                writeProblem(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("timestamp", problem.getProperties().get("timestamp"));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
