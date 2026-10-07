CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS notes (
    id VARCHAR(36) PRIMARY KEY,
    owner VARCHAR(64) NOT NULL,
    content VARCHAR(500) NOT NULL,
    FOREIGN KEY (owner) REFERENCES users(username)
);
