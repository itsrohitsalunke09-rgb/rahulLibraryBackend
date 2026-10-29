package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.PermissionChecker;
import org.ngolibrary.security.SecurityConfig;
import org.ngolibrary.service.PermissionService;
import org.ngolibrary.service.BookImportService;
import org.ngolibrary.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, PermissionChecker.class})
class BookControllerTest {

    @MockBean
    private PermissionService permissionService;

    @org.junit.jupiter.api.BeforeEach
    void defaultPermissions() {
        TestPermissions.defaults(permissionService);
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookService bookService;
    @MockBean
    private BookImportService importService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    void registrationNumberOrderFollowsTheDirection() throws Exception {
        when(bookService.listPagedByRegistrationNumber(any(), any(Pageable.class), any(Boolean.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/books/paged").param("sort", "registrationNumber").param("direction", "desc"))
                .andExpect(status().isOk());
        verify(bookService).listPagedByRegistrationNumber(isNull(), any(Pageable.class), eq(true));

        mvc.perform(get("/api/books/paged").param("sort", "registrationNumber").param("q", "gita"))
                .andExpect(status().isOk());
        verify(bookService).listPagedByRegistrationNumber(eq("gita"), any(Pageable.class), eq(false));
    }
}
