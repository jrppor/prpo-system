package com.jirapat.prpo.filter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void shouldGenerateRequestIdWhenNotProvided() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Request-Id"), captor.capture());
        assertThat(captor.getValue()).isNotBlank();
        assertThat(captor.getValue()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldUseProvidedRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("my-correlation-id-123");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Request-Id", "my-correlation-id-123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldIgnoreBlankRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Request-Id"), captor.capture());
        assertThat(captor.getValue()).isNotBlank();
        assertThat(captor.getValue()).doesNotContain("   ");
    }

    @Test
    void shouldCleanMdcAfterRequest() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("test-id");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void shouldCleanMdcEvenOnException() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("test-id");
        org.mockito.Mockito.doThrow(new jakarta.servlet.ServletException("error"))
                .when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (jakarta.servlet.ServletException ignored) {
        }

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void shouldSetMdcDuringFilterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("trace-abc");

        final String[] capturedMdc = {null};
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedMdc[0] = MDC.get("requestId");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedMdc[0]).isEqualTo("trace-abc");
    }
}
