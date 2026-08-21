package com.autoservice.lambda.repository;

import com.autoservice.lambda.model.Customer;
import com.autoservice.lambda.util.SensitiveDataHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Repository para acesso a dados de cliente.
 */
public class CustomerRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);
    private final DatabaseConnection dbConnection;

    public CustomerRepository() {
        this.dbConnection = new DatabaseConnection();
    }

    public CustomerRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Busca cliente por CPF.
     *
     * @param cpf CPF do cliente (apenas dígitos)
     * @return Customer ou null se não encontrado
     */
    public Customer findByCpf(String cpf) {
        try {
            Connection conn = dbConnection.connect();
            if (conn == null) {
                return null;
            }

            String query = "SELECT cpf, status, (status = 'ATIVO') as active FROM cliente WHERE cpf = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, cpf);

            ResultSet rs = stmt.executeQuery();
            Customer customer = null;
            if (rs.next()) {
                customer = new Customer(
                        rs.getString("cpf"),
                        rs.getString("status"),
                        rs.getBoolean("active")
                );
                logger.debug("Cliente encontrado para cpf_hash={} com status={}",
                        SensitiveDataHasher.sha256Short(customer.getCpf()),
                        customer.getStatus());
            }

            rs.close();
            stmt.close();
            conn.close();

            return customer;
        } catch (SQLException e) {
            logger.error("Erro ao consultar cliente: {}", e.getMessage(), e);
            return null;
        }
    }
}
