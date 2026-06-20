package kr.devport.api.domain.common.logging

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.sql.SQLException
import javax.sql.DataSource

@Component
class HikariStartupLogger(
    private val dataSource: DataSource,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val hikariDataSource = resolveHikariDataSource()
        if (hikariDataSource == null) {
            log.warn(
                "Startup datasource log skipped because the active DataSource is not a HikariDataSource: {}",
                dataSource.javaClass.name,
            )
            return
        }

        log.info(
            "HikariDataSource startup config resolved: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms",
            hikariDataSource.maximumPoolSize,
            hikariDataSource.minimumIdle,
            hikariDataSource.connectionTimeout,
        )
    }

    private fun resolveHikariDataSource(): HikariDataSource? {
        if (dataSource is HikariDataSource) {
            return dataSource
        }

        try {
            if (dataSource.isWrapperFor(HikariDataSource::class.java)) {
                return dataSource.unwrap(HikariDataSource::class.java)
            }
        } catch (e: SQLException) {
            log.warn("Failed to unwrap DataSource to HikariDataSource for startup logging", e)
        }

        return null
    }
}
