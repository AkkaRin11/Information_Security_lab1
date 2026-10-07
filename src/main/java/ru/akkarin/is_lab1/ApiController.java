package ru.akkarin.is_lab1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@RestController
public class ApiController {
    private final UserRepository users;
    private final NoteRepository notes;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public ApiController(UserRepository users, NoteRepository notes, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.notes = notes;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String hash = users.passwordHash(request.username()).orElse(null);
        if (hash == null || !encoder.matches(request.password(), hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new TokenResponse(jwt.issue(request.username()), "Bearer", 3600);
    }

    @GetMapping("/api/data")
    public List<NoteResponse> data(Authentication authentication) {
        return notes.findByOwner(authentication.getName()).stream().map(this::safeNote).toList();
    }

    @PostMapping("/api/data")
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request,
                                                Authentication authentication) {
        NoteRepository.Note note = notes.create(authentication.getName(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(safeNote(note));
    }

    private NoteResponse safeNote(NoteRepository.Note note) {
        return new NoteResponse(note.id(), HtmlUtils.htmlEscape(note.content()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record CreateNoteRequest(@NotBlank @Size(max = 500) String content) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
    public record NoteResponse(String id, String content) {}
}
