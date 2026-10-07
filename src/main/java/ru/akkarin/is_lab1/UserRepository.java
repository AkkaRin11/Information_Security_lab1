package ru.akkarin.is_lab1;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages the injected JdbcTemplate lifecycle")
    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> passwordHash(String username) {
        List<String> hashes = jdbc.query("SELECT password_hash FROM users WHERE username = ?",
                (rs, row) -> rs.getString(1), username);
        return hashes.stream().findFirst();
    }

    public void createIfAbsent(String username, String hash) {
        if (passwordHash(username).isEmpty()) {
            jdbc.update("INSERT INTO users(username, password_hash) VALUES (?, ?)", username, hash);
        }
    }
}
