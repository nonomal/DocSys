package com.DocSystem.agent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.DocSystem.agent.client.DocSysClient;
import com.DocSystem.agent.llm.LLMService;
import com.DocSystem.agent.orchestrator.MainAgent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * DocSysAgentApplication - Native Spring + Tomcat 7.0.56 entry point
 *
 * This replaces Spring Boot with native Spring Framework 5.3.x for WAR deployment.
 * Tomcat 7.0.56 uses javax.* namespace (not jakarta.*).
 */
// MERGED INTO DocSys: this class is no longer the container bootstrap.
// DocSys' own spring-mybatis.xml (root context) and spring-mvc.xml (mvc context)
// now drive component scanning, MyBatis mapper scanning, DataSource, MVC and
// async/websocket support. This class is reduced to a plain @Configuration that
// is picked up by the root-context component-scan and only contributes the
// Agent-specific @Beans below (DocSysClient, MainAgent, MeterRegistry,
// GlobalExceptionHandler).
//
// Removed annotations and why:
//   @EnableWebMvc   -> DocSys spring-mvc.xml already configures Spring MVC
//   @ComponentScan  -> replaced by <context:component-scan base-package="com.DocSystem.agent"/>
//   @MapperScan     -> replaced by DocSys MapperScannerConfigurer (multi basePackage)
//   @EnableWebSocket -> Agent has no WebSocketConfigurer/handler, so it is not needed.
// Kept annotations and why:
//   @EnableAsync / @EnableScheduling -> Agent uses @Async (EvolutionTrigger) and
//     @Scheduled (SessionService). DocSys does NOT enable these itself, so they
//     must live here. They take effect in the root container, where those
//     @Component/@Service beans are registered; DocSys is unaffected.
@Configuration
@EnableAsync
@EnableScheduling
public class DocSysAgentApplication {

    private static final Logger log = LoggerFactory.getLogger(DocSysAgentApplication.class);

    /** Reference to the streaming executor so it can be shut down gracefully */
    private ExecutorService streamingExecutor;

    private MainAgent mainAgentField;

    @org.springframework.beans.factory.annotation.Value("${docsys.url:http://localhost:8100/DocSystem}")
    private String docsysUrl;

    @PostConstruct
    public void init() {
        log.info("DocSysAgent initialized via native Spring configuration");
    }

    @PreDestroy
    public void onShutdown() {
        log.info("DocSysAgent shutting down gracefully...");
        if (mainAgentField != null) {
            mainAgentField.shutdown();
        }
        if (streamingExecutor != null) {
            streamingExecutor.shutdown();
            try {
                if (!streamingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("streamingExecutor did not terminate in 30s, forcing shutdown");
                    streamingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Shutdown interrupted, forcing now");
                streamingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("DocSysAgent shutdown complete");
    }

    public void setMainAgent(MainAgent mainAgent) {
        this.mainAgentField = mainAgent;
    }

    /**
     * Allow AgentController to inject the streaming executor for shutdown.
     */
    public void setStreamingExecutor(ExecutorService executor) {
        this.streamingExecutor = executor;
    }

    @Bean
    public DocSysClient docSysClient() {
        log.info("Creating DocSysClient with URL: {}", docsysUrl);
        return new DocSysClient(docsysUrl);
    }

    @Bean
    public MainAgent mainAgent(DocSysClient docSysClient, LLMService llmService,
            com.DocSystem.agent.learning.service.CollaborativeFilteringService learningService,
            com.DocSystem.agent.learning.service.BehaviorTrackingService behaviorTrackingService,
            com.DocSystem.agent.evolution.EvolutionTrigger evolutionTrigger) {
        MainAgent agent = new MainAgent(docSysClient);
        agent.setLlmService(llmService);
        agent.setLearningService(learningService);
        agent.setBehaviorTrackingService(behaviorTrackingService);
        agent.setEvolutionTrigger(evolutionTrigger);
        log.info("MainAgent configured with LLMService, Learning Services, and EvolutionTrigger");
        return agent;
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * Micrometer MeterRegistry for AgentMetrics and any other monitoring
     * components. Uses SimpleMeterRegistry (in-memory) so the WAR runs
     * without a Prometheus pushgateway endpoint. Production deployments
     * may override this bean to expose metrics to Prometheus.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * MySQL-compatible DataSource. Self-discovered at agent startup from
     * DocSystem's own {@code jdbc.properties} (Path A — see
     * {@link com.DocSystem.agent.config.DocSysSelfDiscovery}). The agent WAR
     * carries no database credentials of its own; if DocSystem is not
     * deployed alongside (e.g. unit tests), the discovery falls back to
     * a hardcoded localhost URL.
     *
     * <p>For production, swap in HikariCP via pom change to
     * {@code com.zaxxer:HikariDataSource}.
     */
    // REMOVED @Bean DataSource dataSource(): merged app reuses DocSys' own
    // c3p0 ComboPooledDataSource defined in spring-mybatis.xml. Agent no longer
    // self-discovers the DB — it shares DocSys' single datasource and DB.
    // (DocSysSelfDiscovery is now dead code for the merged deployment.)

    /**
     * MultipartResolver for Spring 4.3 + @RequestParam MultipartFile.
     * Required so /skills/upload and /upload endpoints actually parse multipart bodies.
     * Without this, Spring returns 400 "Required request part 'file' is not present"
     * even with commons-fileupload on the classpath.
     */
    // REMOVED @Bean multipartResolver(): DocSys spring-mvc.xml already defines a
    // CommonsMultipartResolver in the MVC container. Two multipartResolver beans
    // (one per container) would be redundant; the MVC-container one is the one
    // DispatcherServlet actually uses for /agent/skills/upload etc.

    /**
     * Single MyBatis SqlSessionFactory bound to the DataSource above.
     * Mapper XML files live under classpath mapper directories.
     */
    // REMOVED @Bean sqlSessionFactory(): merged app reuses DocSys' single
    // SqlSessionFactory (spring-mybatis.xml). Notes on the two settings this bean
    // used to carry:
    //   1. mapUnderscoreToCamelCase=true  -> NOT migrated. Every Agent mapper uses
    //      an explicit <resultMap> for snake_case -> camelCase, so the global flag
    //      is unnecessary; leaving it off also avoids changing DocSys' own mappers.
    //   2. TaskStatusTypeHandler (case-insensitive enum)  -> registered in
    //      src/mybatis-config.xml <typeHandlers>.
    // Agent mapper XMLs are picked up by extending DocSys' sqlSessionFactory
    // mapperLocations to also include classpath:mapper/*.xml.

    /**
     * Global exception handler for better error reporting
     */
    @RestController
    public static class GlobalExceptionHandler {

        private static final Logger errorLog = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(Exception.class)
        public Map<String, Object> handleException(Exception e) {
            errorLog.error("Global exception handler caught: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("type", e.getClass().getName());
            response.put("cause", e.getCause() != null ? e.getCause().getMessage() : null);
            return response;
        }
    }
}
