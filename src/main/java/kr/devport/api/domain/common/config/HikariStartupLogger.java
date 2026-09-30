package kr.devport.api.domain.common.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
@Slf4j
public class HikariStartupLogger implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        HikariDataSource hikariDataSource = resolveHikariDataSource();
        if (hikariDataSource == null) {
            log.warn("Startup datasource log skipped because the active DataSource is not a HikariDataSource: {}", dataSource.getClass().getName());
            return;
        }

        log.info(
            "HikariDataSource startup config resolved: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms",
            hikariDataSource.getMaximumPoolSize(),
            hikariDataSource.getMinimumIdle(),
            hikariDataSource.getConnectionTimeout()
        );
    }

    private HikariDataSource resolveHikariDataSource() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            return hikariDataSource;
        }

        try {
            if (dataSource.isWrapperFor(HikariDataSource.class)) {
                return dataSource.unwrap(HikariDataSource.class);
            }
        } catch (SQLException e) {
            log.warn("Failed to unwrap DataSource to HikariDataSource for startup logging", e);
        }

        return null;
    }
}
