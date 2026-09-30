package kr.devport.api.domain.auth.repository;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
