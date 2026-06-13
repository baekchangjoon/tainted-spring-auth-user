package com.tainted.authuser.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findBySocialProviderAndExternalId(String socialProvider, String externalId);
}
