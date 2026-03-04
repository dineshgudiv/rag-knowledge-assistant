# Docker Proof

## compose ps
`
NAME                                 IMAGE                         COMMAND                  SERVICE    CREATED              STATUS                        PORTS
rag-knowledge-assistant-app-1        rag-knowledge-assistant-app   "java -jar /app/app.…"   app        About a minute ago   Up About a minute             127.0.0.1:8081->8080/tcp
rag-knowledge-assistant-postgres-1   postgres:16                   "docker-entrypoint.s…"   postgres   About a minute ago   Up About a minute (healthy)   127.0.0.1:5432->5432/tcp

`

## app logs tail 80
`
app-1  | Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts
app-1  | 
app-1  |   .   ____          _            __ _ _
app-1  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
app-1  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
app-1  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
app-1  |   '  |____| .__|_| |_|_| |_\__, | / / / /
app-1  |  =========|_|==============|___/=/_/_/_/
app-1  | 
app-1  |  :: Spring Boot ::                (v3.3.5)
app-1  | 
app-1  | 2026-03-03T21:19:07.057Z  INFO 1 --- [           main] c.c.r.RagAssistantApplication            : Starting RagAssistantApplication v0.0.1-SNAPSHOT using Java 17.0.18 with PID 1 (/app/app.jar started by root in /app)
app-1  | 2026-03-03T21:19:07.117Z  INFO 1 --- [           main] c.c.r.RagAssistantApplication            : The following 1 profile is active: "postgres"
app-1  | 2026-03-03T21:19:33.004Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
app-1  | 2026-03-03T21:19:33.169Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 139 ms. Found 7 JPA repository interfaces.
app-1  | 2026-03-03T21:19:35.980Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
app-1  | 2026-03-03T21:19:36.052Z  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
app-1  | 2026-03-03T21:19:36.052Z  INFO 1 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.31]
app-1  | 2026-03-03T21:19:36.476Z  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
app-1  | 2026-03-03T21:19:36.494Z  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 28577 ms
app-1  | Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts
app-1  | 2026-03-03T21:19:38.009Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
app-1  | 2026-03-03T21:19:38.747Z  INFO 1 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@1c6ac73c
app-1  | 2026-03-03T21:19:38.753Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
app-1  | 2026-03-03T21:19:38.814Z  INFO 1 --- [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://postgres:5432/rag (PostgreSQL 16.13)
app-1  | 2026-03-03T21:19:38.921Z  INFO 1 --- [           main] o.f.c.i.s.JdbcTableSchemaHistory         : Schema history table "public"."flyway_schema_history" does not exist yet
app-1  | 2026-03-03T21:19:38.929Z  INFO 1 --- [           main] o.f.core.internal.command.DbValidate     : Successfully validated 8 migrations (execution time 00:00.043s)
app-1  | 2026-03-03T21:19:38.995Z  INFO 1 --- [           main] o.f.c.i.s.JdbcTableSchemaHistory         : Creating Schema History table "public"."flyway_schema_history" ...
app-1  | 2026-03-03T21:19:39.184Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
app-1  | 2026-03-03T21:19:39.239Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - init"
app-1  | 2026-03-03T21:19:39.562Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "2 - seed eval cases"
app-1  | 2026-03-03T21:19:39.847Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "3 - add chunk embeddings"
app-1  | 2026-03-03T21:19:40.301Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "4 - eval case fields"
app-1  | 2026-03-03T21:19:40.380Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "6 - audit logs"
app-1  | 2026-03-03T21:19:40.452Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "6.1 - parent child tokens"
app-1  | 2026-03-03T21:19:40.560Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "6.2 - query log mode"
app-1  | 2026-03-03T21:19:40.596Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "7 - eval metrics"
app-1  | 2026-03-03T21:19:40.628Z  INFO 1 --- [           main] o.f.core.internal.command.DbMigrate      : Successfully applied 8 migrations to schema "public", now at version v7 (execution time 00:00.499s)
app-1  | 2026-03-03T21:19:40.856Z  INFO 1 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
app-1  | 2026-03-03T21:19:41.062Z  INFO 1 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.5.3.Final
app-1  | 2026-03-03T21:19:41.148Z  INFO 1 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
app-1  | 2026-03-03T21:19:42.146Z  INFO 1 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
app-1  | 2026-03-03T21:19:45.431Z  INFO 1 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
app-1  | 2026-03-03T21:19:45.554Z  INFO 1 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
app-1  | 2026-03-03T21:19:46.960Z  INFO 1 --- [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
app-1  | 2026-03-03T21:19:50.452Z  WARN 1 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
app-1  | 2026-03-03T21:19:50.579Z  INFO 1 --- [           main] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page: class path resource [static/index.html]
app-1  | 2026-03-03T21:19:52.331Z  INFO 1 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
app-1  | 2026-03-03T21:19:52.502Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
app-1  | 2026-03-03T21:19:52.533Z  INFO 1 --- [           main] c.c.r.RagAssistantApplication            : Started RagAssistantApplication in 51.859 seconds (process running for 66.524)
app-1  | 2026-03-03T21:19:53.628Z  INFO 1 --- [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
app-1  | 2026-03-03T21:19:53.630Z  INFO 1 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
app-1  | 2026-03-03T21:19:53.637Z  INFO 1 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 3 ms

`

## health
UP
