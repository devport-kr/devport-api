package kr.devport.api.domain.auth.repository;

import kr.devport.api.domain.auth.entity.PasswordResetToken;
import kr.devport.api.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
