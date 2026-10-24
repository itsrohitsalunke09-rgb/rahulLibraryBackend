package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ngolibrary.service.PhotoStorage;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.SecurityConfig;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NewsController.class)
@Import(SecurityConfig.class)
class NewsControllerTest {

    private static final String STORY_JSON = "{\"headline\":\"Book fair\",\"body\":\"Join us\",\"eventDate\":\"2026-10-01\"}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NewsService newsService;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserDetailsService userDetailsService;

    private MockMultipartFile storyPart() {
        return new MockMultipartFile("story", "", "application/json", STORY_JSON.getBytes());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanList() throws Exception {
        when(newsService.list(0, 10, null)).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/api/news")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotCreate() throws Exception {
        mvc.perform(multipart("/api/news").file(storyPart())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotUpdate() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/api/news/1").file(storyPart())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotDelete() throws Exception {
        mvc.perform(delete("/api/news/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianCreates() throws Exception {
        UserAccount author = new UserAccount();
        when(authService.current(any(Authentication.class))).thenReturn(author);
        mvc.perform(multipart("/api/news").file(storyPart())).andExpect(status().isCreated());
        verify(newsService).create(any(), eq(null), eq(author));
    }

    @Test
    void anonymousPhotoRefused() throws Exception {
        mvc.perform(get("/api/news/1/photo")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void photoHasCacheHeader() throws Exception {
        PhotoStorage.Photo photo = new PhotoStorage.Photo("image/jpeg", new byte[] {1, 2, 3});
        when(newsService.photo(1L)).thenReturn(photo);
        mvc.perform(get("/api/news/1/photo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "private, max-age=31536000"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }
}
