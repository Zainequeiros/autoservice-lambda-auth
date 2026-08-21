package com.autoservice.lambda.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gerencia conexão com RDS PostgreSQL.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final HikariDataSource dataSource;

    public DatabaseConnection() {
        this.host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
        this.port = System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 5432;
        this.database = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "autoservice";
        this.user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
        this.password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

        this.dataSource = initializeDataSource();
    }

    public DatabaseConnection(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;

        this.dataSource = initializeDataSource();
    }

    /**
     * Inicializa o pool de conexões.
     */
    private HikariDataSource initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000); // 10 segundos
        config.setIdleTimeout(600000); // 10 minutos
        config.setMaxLifetime(1800000); // 30 minutos

        logger.info("Inicializando pool de conexões para {}:{}/{}", host, port, database);
        return new HikariDataSource(config);
    }

    /**
     * Obtém uma conexão do pool.
     *
     * @return Conexão com banco de dados
     * @throws SQLException se falhar ao conectar
     */
    public Connection connect() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Falha ao obter conexão: {}", e.getMessage(), e);
            throw new SQLException("Falha ao conectar com banco de dados", e);
        }
    }

    /**
     * Fecha o pool de conexões.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Pool de conexões fechado");
        }
    }
}
