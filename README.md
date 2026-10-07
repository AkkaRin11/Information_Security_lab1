# Защищённый REST API — лабораторная работа 1

Учебный API заметок на Java 25, Spring Boot, Spring Security и H2. Пользователь входит по логину и паролю, получает JWT и с ним создаёт и читает свои заметки.

## Запуск

Нужны Java 25 и Maven (либо включённый Maven Wrapper). Задайте переменные окружения. Секрет JWT должен содержать не менее 32 байт UTF-8, пароль — не менее 12 символов.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export DEMO_USERNAME="student"
export DEMO_PASSWORD="choose-a-long-unique-password"
./mvnw spring-boot:run
```

В PowerShell переменные задаются как `$env:JWT_SECRET`, `$env:DEMO_USERNAME`, `$env:DEMO_PASSWORD`; запуск — `.\mvnw.cmd spring-boot:run`. База сохраняется в каталоге `data/`, который исключён из Git. Пользователь создаётся при первом запуске, и при последующих запусках его хэш не перезаписывается. Для смены учебного пароля удалите локальную базу при остановленном приложении.

## API

| Метод | Путь | Доступ | Описание |
| --- | --- | --- | --- |
| POST | `/auth/login` | Открытый | Проверяет логин и пароль, выдаёт JWT на 1 час |
| GET | `/api/data` | Bearer JWT | Возвращает заметки текущего пользователя |
| POST | `/api/data` | Bearer JWT | Создаёт заметку текущего пользователя |

Примеры запросов:

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"choose-a-long-unique-password"}'
```

Ответ содержит `accessToken`, `tokenType` и `expiresIn`. Подставьте полученный токен в следующие запросы:

```bash
curl -i http://localhost:8080/api/data \
  -H 'Authorization: Bearer ВАШ_JWT'

curl -i -X POST http://localhost:8080/api/data \
  -H 'Authorization: Bearer ВАШ_JWT' \
  -H 'Content-Type: application/json' \
  -d '{"content":"Первая заметка"}'

curl -i http://localhost:8080/api/data
```

Последний запрос без токена возвращает HTTP 401. Создание заметки возвращает HTTP 201, чтение — HTTP 200. Тело заметки обязательно и ограничено 500 символами.

## Меры защиты

- **SQLi:** все запросы к H2 в `UserRepository` и `NoteRepository` выполняются через `JdbcTemplate` с параметрами `?` или `PreparedStatement`. Строки SQL не собираются из пользовательского ввода.
- **XSS:** пользовательское содержимое заметки кодируется `HtmlUtils.htmlEscape` перед выдачей в JSON. Например, `<script>` возвращается как `&lt;script&gt;`. В базе хранится исходный текст. При отображении на клиенте также следует пользоваться безопасным текстовым выводом.
- **Аутентификация:** пароль хранится только как BCrypt-хэш. JWT подписывается HS256 секретом из переменной окружения, содержит `iss`, `sub`, время выпуска и истечения. `JwtFilter` проверяет подпись, алгоритм, издателя и срок действия на защищённых маршрутах. Сессии сервера отключены. Отсутствующий или неверный токен даёт HTTP 401.
- **Разделение данных:** запросы заметок ограничены именем пользователя из проверенного JWT; клиент не может подставить чужого владельца в запрос создания.
- **Секреты:** `JWT_SECRET` и пароль не хранятся в репозитории. Файл `.env` и каталог базы исключены из Git.

## Проверки и CI

Локально: `./mvnw test` (Windows: `.\mvnw.cmd test`). Интеграционные тесты проверяют успешный вход, неверный пароль, попытку SQLi, запрет без токена и экранирование XSS.

При каждом `push` и `pull_request` GitHub Actions запускает тесты, **SpotBugs (SAST)** и **OWASP Dependency-Check (SCA)**. SpotBugs останавливает сборку при найденных дефектах. Dependency-Check формирует HTML-отчёт и останавливает сборку при CVSS 9.0+ или ошибке сканирования. Отчёты доступны в артефакте `security-reports` запуска Actions. База уязвимостей загружается из сжатого NVD feed и сохраняется в кеше GitHub Actions для следующих запусков.

Локальные команды сканеров:

```bash
./mvnw -DskipTests spotbugs:spotbugs spotbugs:check
./mvnw -DskipTests dependency-check:check
```

## Источники

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [JWT Introduction](https://jwt.io/introduction)
- [GitHub Actions: Java with Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven)
