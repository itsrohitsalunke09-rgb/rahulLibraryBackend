package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ProfileUpdateRequest;
import org.ngolibrary.dto.UserRequest;
import org.ngolibrary.repo.BookIssueRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {

    private UserAccountRepository users;
    private PhotoStorage photos;
    private UserService service;
    private UserAccount librarian;

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        photos = mock(PhotoStorage.class);
        service = new UserService(users, mock(PasswordEncoder.class), mock(BookIssueRepository.class), photos);
        librarian = account(1L, Role.LIBRARIAN);
    }

    private static UserAccount account(Long id, Role role) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setUsername("u" + id);
        u.setFullName("User " + id);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private static UserRequest request(Role role) {
        UserRequest r = new UserRequest();
        r.setUsername("new");
        r.setFullName("New Person");
        r.setPassword("secret1");
        r.setRole(role);
        return r;
    }

    private static void assertStatus(HttpStatus status, Runnable call) {
        assertEquals(status, assertThrows(ResponseStatusException.class, call::run).getStatus());
    }

    @Test
    void librarianCannotCreateOrPromoteAnAdmin() {
        assertStatus(HttpStatus.FORBIDDEN, () -> service.create(request(Role.ADMIN), librarian));

        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.STUDENT)));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.update(2L, request(Role.ADMIN), librarian));
        verify(users, never()).save(any());
    }

    @Test
    void librarianCannotEditAnAdmin() {
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.ADMIN)));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.update(2L, request(Role.STUDENT), librarian));
    }

    @Test
    void invalidDateOfBirthIsRejected() {
        when(users.findById(1L)).thenReturn(Optional.of(librarian));
        ProfileUpdateRequest profile = new ProfileUpdateRequest();
        profile.setFullName("User 1");
        profile.setDateOfBirth("31/05/2010");
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.updateProfile(1L, profile));
        verify(users, never()).save(any());
    }

    private static MockMultipartFile photo(String type) {
        return new MockMultipartFile("photo", "aadhaar.jpg", type, new byte[] {1, 2, 3});
    }

    @Test
    void storesAnAadhaarPhotoForTheUser() {
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.STUDENT)));
        service.saveAadhaarPhoto(2L, photo("image/jpeg"), librarian);
        verify(photos).put("aadhaar/2", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void rejectsAnAadhaarFileThatIsNotAPhoto() {
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.STUDENT)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.saveAadhaarPhoto(2L, photo("application/pdf"), librarian));
        verify(photos, never()).put(any(), any(), any());
    }

    @Test
    void librarianCannotChangeAnAdminsAadhaarPhoto() {
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.ADMIN)));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.saveAadhaarPhoto(2L, photo("image/jpeg"), librarian));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.deleteAadhaarPhoto(2L, librarian));
    }

    @Test
    void deletingAnAccountDeletesItsAadhaarPhoto() {
        when(users.existsById(2L)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.LIBRARIAN)));
        service.delete(2L, account(9L, Role.ADMIN));
        verify(photos).delete("aadhaar/2");
    }

    @Test
    void accountIsStillDeletedWhenPhotoStorageIsDown() {
        when(users.existsById(2L)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.LIBRARIAN)));
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .when(photos).delete("aadhaar/2");
        service.delete(2L, account(9L, Role.ADMIN));
        verify(users).deleteById(2L);
    }

    @Test
    void aLibrarianWithDeleteStillCannotDeleteAnAdminOrThemselves() {
        when(users.findById(2L)).thenReturn(Optional.of(account(2L, Role.ADMIN)));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.delete(2L, librarian));
        when(users.findById(1L)).thenReturn(Optional.of(librarian));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.delete(1L, librarian));
        verify(users, never()).deleteById(any());
    }
}
