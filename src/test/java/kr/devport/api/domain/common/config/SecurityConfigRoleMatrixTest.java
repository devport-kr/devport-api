package kr.devport.api.domain.common.config;

import jakarta.servlet.FilterChain;
import kr.devport.api.domain.auth.oauth2.CustomOAuth2UserService;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationFailureHandler;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import kr.devport.api.domain.common.security.JwtAuthenticationFilter;
import kr.devport.api.domain.wiki.controller.WikiAuthoringController;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.service.WikiAuthoringWorkflowService;
import kr.devport.api.domain.wiki.service.WikiPublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WikiAuthoringController.class)
@Import(SecurityConfig.class)
class SecurityConfigRoleMatrixTest {

    private static final String PROJECT_BASE = "/api/wiki/admin/projects/7";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WikiAuthoringWorkflowService wikiAuthoringWorkflowService;

    @MockitoBean
    private WikiPublishingService wikiPublishingService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        WikiDraft draft = WikiDraft.builder().projectId(7L).sections(List.of(Map.of("sectionId", "what"))).build();
        when(wikiAuthoringWorkflowService.listDrafts(7L)).thenReturn(List.of(draft));
        when(wikiAuthoringWorkflowService.getDraft(7L, 11L)).thenReturn(draft);
    }

    @Test
    @DisplayName("editor can access draft list/get but cannot create draft")
    @WithMockUser(roles = "EDITOR")
    void editorReadOnlyMatrix() throws Exception {
        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROJECT_BASE + "/drafts/11"))
                .andExpect(status().isOk());

        mockMvc.perform(post(PROJECT_BASE + "/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("public requests are rejected for draft endpoints")
    void publicCannotAccessDraftEndpoints() throws Exception {
        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(PROJECT_BASE + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
