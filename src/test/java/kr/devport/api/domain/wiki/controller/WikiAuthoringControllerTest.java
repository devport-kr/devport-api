package kr.devport.api.domain.wiki.controller;

import jakarta.servlet.FilterChain;
import kr.devport.api.domain.auth.oauth2.CustomOAuth2UserService;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationFailureHandler;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import kr.devport.api.domain.common.config.SecurityConfig;
import kr.devport.api.domain.common.security.JwtAuthenticationFilter;
import kr.devport.api.domain.wiki.dto.response.WikiVersionHistoryResponse;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.service.WikiAuthoringWorkflowService;
import kr.devport.api.domain.wiki.service.WikiPublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WikiAuthoringController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class WikiAuthoringControllerTest {

    private static final String PROJECT_BASE = "/api/wiki/admin/projects/7";

    private static final String DRAFT_REQUEST_JSON = """
            {
              "sections": [
                {
                  "sectionId": "what",
                  "summary": "summary",
                  "deepDiveMarkdown": "deep"
                }
              ],
              "currentCounters": {
                "stars": 10
              },
              "hiddenSections": ["chat"]
            }
            """;

    private static final String PUBLISH_REQUEST_JSON = """
            {
              "draftId": 100
            }
            """;

    private static final String ROLLBACK_REQUEST_JSON = """
            {
              "targetVersionNumber": 2
            }
            """;

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
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        WikiDraft draft = WikiDraft.builder()
                .projectId(7L)
                .sections(List.of(Map.of("sectionId", "what")))
                .currentCounters(Map.of("stars", 10))
                .hiddenSections(List.of("chat"))
                .build();

        when(wikiAuthoringWorkflowService.createDraft(anyLong(), anyList(), anyMap(), anyList(), isNull())).thenReturn(draft);
        when(wikiAuthoringWorkflowService.listDrafts(7L)).thenReturn(List.of(draft));
        when(wikiAuthoringWorkflowService.getDraft(7L, 11L)).thenReturn(draft);
        when(wikiAuthoringWorkflowService.updateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList())).thenReturn(draft);
        when(wikiAuthoringWorkflowService.regenerateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList())).thenReturn(draft);

        WikiVersionHistoryResponse publish = WikiVersionHistoryResponse.builder()
                .projectId(7L)
                .latestVersionNumber(3)
                .versions(List.of())
                .build();
        WikiVersionHistoryResponse rollback = WikiVersionHistoryResponse.builder()
                .projectId(7L)
                .latestVersionNumber(4)
                .versions(List.of())
                .build();

        when(wikiPublishingService.publish(7L, 100L)).thenReturn(publish);
        when(wikiPublishingService.rollbackToVersion(7L, 2)).thenReturn(rollback);
    }

    @Test
    @DisplayName("ADMIN can execute full draft lifecycle including publish and rollback")
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAllDraftLifecycleEndpoints() throws Exception {
        mockMvc.perform(post(PROJECT_BASE + "/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROJECT_BASE + "/drafts/11"))
                .andExpect(status().isOk());

        mockMvc.perform(put(PROJECT_BASE + "/drafts/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post(PROJECT_BASE + "/drafts/11/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post(PROJECT_BASE + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_REQUEST_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post(PROJECT_BASE + "/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROLLBACK_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(wikiAuthoringWorkflowService).createDraft(anyLong(), anyList(), anyMap(), anyList(), isNull());
        verify(wikiAuthoringWorkflowService).listDrafts(7L);
        verify(wikiAuthoringWorkflowService).getDraft(7L, 11L);
        verify(wikiAuthoringWorkflowService).updateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList());
        verify(wikiAuthoringWorkflowService).regenerateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList());
        verify(wikiPublishingService).publish(7L, 100L);
        verify(wikiPublishingService).rollbackToVersion(7L, 2);
    }

    @Test
    @DisplayName("EDITOR can list/get drafts but cannot mutate, publish, or rollback")
    @WithMockUser(roles = "EDITOR")
    void editorHasReadOnlyDraftAccess() throws Exception {
        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROJECT_BASE + "/drafts/11"))
                .andExpect(status().isOk());

        mockMvc.perform(post(PROJECT_BASE + "/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(PROJECT_BASE + "/drafts/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PROJECT_BASE + "/drafts/11/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PROJECT_BASE + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_REQUEST_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PROJECT_BASE + "/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROLLBACK_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(wikiAuthoringWorkflowService).listDrafts(7L);
        verify(wikiAuthoringWorkflowService).getDraft(7L, 11L);
        verify(wikiAuthoringWorkflowService, never()).createDraft(anyLong(), anyList(), anyMap(), anyList(), isNull());
        verify(wikiAuthoringWorkflowService, never()).updateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList());
        verify(wikiAuthoringWorkflowService, never()).regenerateDraft(anyLong(), anyLong(), anyList(), anyMap(), anyList());
        verify(wikiPublishingService, never()).publish(anyLong(), anyLong());
        verify(wikiPublishingService, never()).rollbackToVersion(anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("authenticated non-admin/editor users are denied draft APIs")
    @WithMockUser(roles = "USER")
    void nonPrivilegedAuthenticatedUserIsRejected() throws Exception {
        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PROJECT_BASE + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("unauthenticated public requests are rejected for draft visibility and lifecycle endpoints")
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get(PROJECT_BASE + "/drafts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(PROJECT_BASE + "/drafts/11"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(PROJECT_BASE + "/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRAFT_REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(PROJECT_BASE + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_REQUEST_JSON))
                .andExpect(status().isUnauthorized());
    }
}
