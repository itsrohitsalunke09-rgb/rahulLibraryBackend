package org.ngolibrary.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.PeriodicalFrequency;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalSummary;
import org.ngolibrary.security.AppUserDetailsService;
import org.ngolibrary.security.JwtService;
import org.ngolibrary.security.SecurityConfig;
import org.ngolibrary.service.PeriodicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PeriodicalController.class)
@Import(SecurityConfig.class)
class PeriodicalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PeriodicalService service;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentSeesTheSubscribedListWithoutPrices() throws Exception {
        when(service.subscribed()).thenReturn(List.of(new PeriodicalSummary(1L, "Sakal", "Marathi", PeriodicalFrequency.DAILY)));
        mvc.perform(get("/api/periodicals/subscribed"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"id\":1,\"name\":\"Sakal\",\"language\":\"Marathi\",\"frequency\":\"DAILY\"}]", true));
    }

    @Test
    void anonymousCannotSeeTheList() throws Exception {
        mvc.perform(get("/api/periodicals/subscribed")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSeeTheMasterOrRegister() throws Exception {
        mvc.perform(get("/api/periodicals")).andExpect(status().isForbidden());
        mvc.perform(get("/api/periodicals/report").param("month", "2026-10")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianCannotSeeTheMaster() throws Exception {
        mvc.perform(get("/api/periodicals")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianUsesTheMagazineRegister() throws Exception {
        when(service.magazines()).thenReturn(List.of());
        mvc.perform(get("/api/periodicals/magazines")).andExpect(status().isOk());
        mvc.perform(post("/api/periodicals/2/receipts").contentType("application/json")
                        .content("{\"issueNo\":12,\"receivedOn\":\"2026-10-20\",\"periodFrom\":\"2026-10-20\",\"periodTo\":\"2026-10-20\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSeeTheMagazineRegister() throws Exception {
        mvc.perform(get("/api/periodicals/magazines")).andExpect(status().isForbidden());
    }
}
