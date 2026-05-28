# Project Instructions

## Env Vars — Hard Rule

Whenever you add/modify a property in `application.yaml`, you MUST in the same change update `README.md`:
- Group under a section, format: `` - `ENV_VAR` – Russian explanation (по умолчанию: default) ``
- Preserve YAML order; do NOT sort alphabetically.

## Spring Config Pattern

Use `@Value` parameter injection (not `@ConfigurationProperties`, except for complex Spring classes like `KafkaProperties`).

```java
@Configuration("serviceConfig")
class Config {
    @Bean
    RestClient serviceRestClient(
            @Value("${models.service.url}") String url,
            @Value("${models.service.read-timeout}") Duration readTimeout,
            @Value("${models.service.connection-timeout}") Duration connectionTimeout) {
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(connectionTimeout);
                    setReadTimeout(readTimeout);
                }})
                .build();
    }
}
```

```yaml
models:
  service:
    url: ${MODELS_SERVICE_URL:http://localhost:8080}
    read-timeout: ${MODELS_SERVICE_READ_TIMEOUT:PT30S}
    connection-timeout: ${MODELS_SERVICE_CONNECTION_TIMEOUT:PT10S}
```

**Env var name = full YAML path → SCREAMING_CASE, `.`/`-` → `_`.**
- `models.service.read-timeout` → `MODELS_SERVICE_READ_TIMEOUT` ✅
- Never use partial names like `SERVICE_URL` ❌.

**`@ConfigurationProperties` exception** — only for Spring's own complex classes:
```java
@Bean(autowireCandidate = false)
@ConfigurationProperties("spring.kafka")
KafkaProperties kafkaProperties() {
    return new KafkaProperties() {{
        getProducer().setValueSerializer(JsonSerializer.class);
    }};
}
```

**Bean disambiguation by name.** Match the injection-point parameter name to the `@Bean` method name instead of using `@Qualifier`.

## Lombok + Spring Annotations on Constructor Injection — Hard Rule

When using constructor injection with Lombok (`@RequiredArgsConstructor`) and `final` fields, pay special attention to Spring annotations that must be present on constructor parameters.

- Put Spring annotations on `final` fields (not on setters; setter injection is forbidden).
- Rely on Lombok parameter annotation copying via `lombok.copyableAnnotations`.
- Current project setup in `lombok.config` already copies:
  - `org.springframework.beans.factory.annotation.Value`
  - `org.springframework.context.annotation.Lazy`
- If you want to use other parameter-sensitive Spring annotations (for example `@Qualifier`) on `final` fields with `@RequiredArgsConstructor`, first add them to `lombok.copyableAnnotations` in `lombok.config`.

Correct (`@Value` on field + `@RequiredArgsConstructor`):
```java
@Service
@RequiredArgsConstructor
class UserDocumentsHousekeepingUseCase {
    @Value("${rag.user-documents.retention}")
    private final Duration userRagRetention;
    private final DocumentRepository repository;
}
```

Also correct (explicit constructor parameter annotation when needed):
```java
@Service
class SomeService {
    private final Client client;

    SomeService(@Qualifier("primaryClient") Client client) {
        this.client = client;
    }
}
```

Wrong (annotation lost for generated constructor parameter):
```java
@Service
@RequiredArgsConstructor
class SomeService {
    @Qualifier("primaryClient") // Will NOT be copied unless configured in lombok.config
    private final Client client;
}
```

If in doubt, prefer explicit constructor for that class.

## Code Style

### Guard Clause for Empty Collections — Hard Rule

- This rule is mandatory and easy to miss, so keep it explicit in reviews and generated code.
- For short guard clauses (including empty-collection checks), use one line with no braces:
  ```java
  if (expired.isEmpty()) return;
  ```
- Do not rewrite this pattern into a multi-line block when the condition is short.

- Lombok for boilerplate; `@Slf4j` for logging; constructor injection only.
- `Duration` (PT format: `PT30S`, `PT10M`) for all timeouts.
- `var` for locals when the type is obvious from the RHS.
- Single-statement `if` / `if-else` → no braces. Короткое условие (guard clause, условно ≤ ~60 символов) — в одну строку; длинное — условие и тело на отдельных строках:
  ```java
  if (documents.isEmpty()) return;

  if (repository.existsByFilenameAndContentHash(filename, contentHash))
      return;

  if (condition)
      return value;
  else
      return defaultValue;
  ```
- `StringUtils.hasText(s)` instead of `s != null && !s.isEmpty()`.
- `Optional` chains over nested null checks:
  ```java
  return Optional.ofNullable(req.getRemoteAddress())
          .map(InetSocketAddress::getAddress)
          .map(InetAddress::getHostAddress)
          .orElse("unknown");
  ```
- `"%s = %s".formatted(a, b)` instead of `String.format(...)`.

## Controllers

- По возможности задавай HTTP-статус через `@ResponseStatus`, а не через `ResponseEntity`. `ResponseEntity` — если статус, заголовки или тело собираются динамически.

```java
@PostMapping
@ResponseStatus(CREATED)
OrderDto create(@RequestBody OrderRequest req) {
    return service.create(req);
}
```

## Architecture

- Business logic in service layer; HTTP via `RestClient`.
- Keep `connection-timeout` short (10–30s) and `read-timeout` sized to expected processing time.
- Configure Tomcat `connection-timeout` and `keep-alive-timeout` for long-running requests.

## Package by Feature

Flat package per feature; all classes (Entity, DTO, Repository, Service, Controller) at the same level. Only shared/cross-cutting config goes under `config/`.

```
src/main/java/dev/rost/redis
├── RedisApplication.java
├── config/RedisConfig.java
└── article/
    ├── Article.java
    ├── Comment.java
    ├── ArticleInfo.java
    ├── ArticleRepository.java
    ├── ArticleService.java
    └── ArticleController.java
```

❌ Don't organize by layer (`controller/`, `service/`, `repository/`).
❌ Don't create sub-packages inside a feature.
