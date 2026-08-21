package com.autoservice.lambda.service;

import com.autoservice.lambda.dto.AuthResponse;
import com.autoservice.lambda.model.Customer;
import com.autoservice.lambda.repository.CustomerRepository;
import com.autoservice.lambda.util.CpfValidator;
import com.autoservice.lambda.util.SensitiveDataHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Serviço de autenticação.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final CustomerRepository customerRepository;
    private final JwtTokenGenerator tokenGenerator;

    public AuthService(CustomerRepository customerRepository, JwtTokenGenerator tokenGenerator) {
        this.customerRepository = customerRepository;
        this.tokenGenerator = tokenGenerator;
    }

    public AuthService() {
        this(new CustomerRepository(), new JwtTokenGenerator());
    }

    /**
     * Autentica um cliente por CPF.
     *
     * @param cpfInput CPF formatado ou não
     * @return AuthResponse com token ou erro
     * @throws AuthException se houver erro na autenticação
     */
    public AuthResponse authenticate(String cpfInput) throws AuthException {
        // Validar CPF obrigatório
        if (cpfInput == null || cpfInput.trim().isEmpty()) {
            logger.warn("Requisição sem CPF");
            throw new AuthException(400, "CPF é obrigatório.");
        }

        // Limpar e validar CPF
        String cpf = CpfValidator.onlyDigits(cpfInput);
        if (!CpfValidator.isValid(cpf)) {
            MDC.put("cpf_hash", SensitiveDataHasher.sha256Short(cpf));
            logger.warn("CPF inválido");
            throw new AuthException(400, "CPF inválido.");
        }
        MDC.put("cpf_hash", SensitiveDataHasher.sha256Short(cpf));

        // Buscar cliente no banco
        logger.info("Buscando cliente por CPF hash");
        Customer customer = customerRepository.findByCpf(cpf);

        if (customer == null) {
            logger.warn("Cliente não encontrado");
            throw new AuthException(404, "Cliente não encontrado.");
        }

        // Validar status do cliente
        if (!customer.isActive()) {
            MDC.put("customer_status", customer.getStatus());
            logger.warn("Cliente inativo ou bloqueado");
            throw new AuthException(403, "Cliente inativo ou bloqueado.");
        }

        // Gerar token
        String token = tokenGenerator.generateToken(customer.getCpf(), customer.getStatus());
        long expiresIn = tokenGenerator.getExpiresInSeconds();

        MDC.put("customer_status", customer.getStatus());
        logger.info("Autenticação bem-sucedida");
        return new AuthResponse(token, "Bearer", expiresIn);
    }

    /**
     * Exceção de autenticação com status code.
     */
    public static class AuthException extends Exception {
        private final int statusCode;

        public AuthException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
