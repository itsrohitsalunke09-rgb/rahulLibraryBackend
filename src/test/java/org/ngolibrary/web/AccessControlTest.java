package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.PermissionChecker;
import org.ngolibrary.security.SecurityConfig;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.BookImportService;
import org.ngolibrary.service.BookService;
import org.ngolibrary.service.IssueService;
import org.ngolibrary.service.OverdueNotificationService;
import org.ngolibrary.service.PeriodicalService;
import org.ngolibrary.service.PermissionService;
import org.ngolibrary.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The server, not just the app, must refuse an action the role may not do. These assert the HTTP
 * status for a role calling an endpoint it shouldn't reach, so a hidden button is never the only guard.
 */
@WebMvcTest(controllers = {UserController.class, IssueController.class, BookController.class,
        PermissionController.class, PeriodicalController.class})
@Import({SecurityConfig.class, PermissionChecker.class})
class AccessControlTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private PermissionService permissionService;
    @MockBean private UserService userService;
    @MockBean private IssueService issueService;
    @MockBean private BookService bookService;
    @MockBean private BookImportService bookImportService;
    @MockBean private PeriodicalService periodicalService;
    @MockBean private OverdueNotificationService overdueNotificationService;
    @MockBean private AuthService authService;
    @MockBean private JwtService jwtService;
    @MockBean private AppUserDetailsService userDetailsService;

    @BeforeEach
    void defaultPermissions() {
        TestPermissions.defaults(permissionService);
    }

    // --- A student must not reach staff endpoints ---

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotListPeople() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSeeAllIssues() throws Exception {
        mvc.perform(get("/api/issues")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotIssueABook() throws Exception {
        mvc.perform(post("/api/issues").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1,\"studentId\":1,\"days\":14}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotDeleteABook() throws Exception {
        mvc.perform(delete("/api/books/1").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotChangePermissions() throws Exception {
        mvc.perform(put("/api/permissions").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotOpenTheRegister() throws Exception {
        mvc.perform(get("/api/periodicals/register").param("date", "2026-10-31")).andExpect(status().isForbidden());
    }

    // --- A librarian without the REGISTER permission must not issue, return or mark registers ---

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianWithoutRegisterCannotIssueOrMark() throws Exception {
        when(permissionService.has(any(Role.class), any(Permission.class))).thenReturn(true);
        when(permissionService.has(eq(Role.LIBRARIAN), eq(Permission.REGISTER))).thenReturn(false);

        mvc.perform(post("/api/issues").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1,\"studentId\":1,\"days\":14}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/issues/1/return").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/periodicals/register").with(csrf()).param("date", "2026-10-31")
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianWithoutDeleteCannotDeleteABook() throws Exception {
        when(permissionService.has(any(Role.class), any(Permission.class))).thenReturn(true);
        when(permissionService.has(eq(Role.LIBRARIAN), eq(Permission.DELETE))).thenReturn(false);

        mvc.perform(delete("/api/books/1").with(csrf())).andExpect(status().isForbidden());
    }

    // --- Positive control: an admin reaches the same endpoints ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListPeople() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isOk());
    }
}
