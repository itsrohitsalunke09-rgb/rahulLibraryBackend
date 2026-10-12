package org.ngolibrary.security;

import java.util.List;

import org.ngolibrary.domain.UserAccount;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Spring Security user that keeps the account loaded by {@link JwtAuthFilter},
 * so controllers don't have to query the users table again in the same request.
 */
public class AppUserPrincipal extends User {

    private final transient UserAccount account;

    public AppUserPrincipal(UserAccount account) {
        super(account.getUsername(), account.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        this.account = account;
    }

    public UserAccount getAccount() {
        return account;
    }
}
