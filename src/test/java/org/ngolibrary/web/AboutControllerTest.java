package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.SiteContent;
import org.ngolibrary.repo.SiteContentRepository;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AboutController.class)
@Import(SecurityConfig.class)
class AboutControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SiteContentRepository content;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserDetailsService userDetailsService;

    @Test
    void anyoneCanReadTheSavedContent() throws Exception {
        SiteContent saved = new SiteContent();
        saved.setJson("{\"title\":\"Rahul\"}");
        when(content.findById("about")).thenReturn(Optional.of(saved));
        mvc.perform(get("/api/about")).andExpect(status().isOk()).andExpect(content().json("{\"title\":\"Rahul\"}"));
    }

    @Test
    void nothingSavedYetIsNoContent() throws Exception {
        when(content.findById("about")).thenReturn(Optional.empty());
        mvc.perform(get("/api/about")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotEdit() throws Exception {
        mvc.perform(put("/api/about").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(content, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void staffSaveAJsonObject() throws Exception {
        when(content.findById("about")).thenReturn(Optional.empty());
        when(content.save(any(SiteContent.class))).thenAnswer(inv -> inv.getArgument(0));
        mvc.perform(put("/api/about").contentType(MediaType.APPLICATION_JSON).content("{ \"title\" : \"Rahul\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"title\":\"Rahul\"}"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void somethingThatIsNotAJsonObjectIsRefused() throws Exception {
        mvc.perform(put("/api/about").contentType(MediaType.APPLICATION_JSON).content("[1, 2]"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/about").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
        verify(content, never()).save(any());
    }
}
