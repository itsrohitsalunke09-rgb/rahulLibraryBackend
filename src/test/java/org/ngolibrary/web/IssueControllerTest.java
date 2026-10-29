package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.PermissionChecker;
import org.ngolibrary.security.SecurityConfig;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.IssueService;
import org.ngolibrary.service.OverdueNotificationService;
import org.ngolibrary.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IssueController.class)
@Import({SecurityConfig.class, PermissionChecker.class})
class IssueControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PermissionService permissionService;
    @MockBean
    private IssueService issueService;
    @MockBean
    private AuthService authService;
    @MockBean
    private OverdueNotificationService overdueNotificationService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserDetailsService userDetailsService;

    @BeforeEach
    void signedInAsAStudent() {
        TestPermissions.defaults(permissionService);
        UserAccount me = new UserAccount();
        me.setId(7L);
        me.setRole(Role.STUDENT);
        when(authService.current(any(Authentication.class))).thenReturn(me);
        when(issueService.studentYear(any(), any())).thenReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void aStudentSeesOnlyTheirOwnBooksFromTheGalaxy() throws Exception {
        mvc.perform(get("/api/issues/student/7")).andExpect(status().isOk());
        mvc.perform(get("/api/issues/student/8")).andExpect(status().isForbidden());
    }
}
