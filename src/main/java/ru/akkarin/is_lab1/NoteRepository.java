package ru.akkarin.is_lab1;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class NoteRepository {
    private final JdbcTemplate jdbc;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages the injected JdbcTemplate lifecycle")
    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Note> findByOwner(String owner) {
        return jdbc.query("SELECT id, content FROM notes WHERE owner = ? ORDER BY id",
                (rs, row) -> new Note(rs.getString("id"), rs.getString("content")), owner);
    }

    public Note create(String owner, String content) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO notes(id, owner, content) VALUES (?, ?, ?)", id, owner, content);
        return new Note(id, content);
    }

    public record Note(String id, String content) {}
}
