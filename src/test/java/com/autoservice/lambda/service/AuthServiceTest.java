package com.autoservice.lambda.service;

import com.autoservice.lambda.dto.AuthResponse;
import com.autoservice.lambda.model.Customer;
import com.autoservice.lambda.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes para o serviço de autenticação.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private JwtTokenGenerator tokenGenerator;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(customerRepository, tokenGenerator);
    }

    @Test
    void testAuthenticateWithValidCpf() throws AuthService.AuthException {
        // Arrange
        Customer activeCustomer = new Customer("39053344705", "ATIVO", true);
        when(customerRepository.findByCpf("39053344705")).thenReturn(activeCustomer);
        when(tokenGenerator.generateToken("39053344705", "ATIVO")).thenReturn("token123");
        when(tokenGenerator.getExpiresInSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.authenticate("390.533.447-05");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void testAuthenticateWithMissingCpf() {
        assertThatThrownBy(() -> authService.authenticate(null))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("obrigatório")
                .extracting(e -> ((AuthService.AuthException) e).getStatusCode())
                .isEqualTo(400);

        assertThatThrownBy(() -> authService.authenticate(""))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("obrigatório");

        assertThatThrownBy(() -> authService.authenticate("   "))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    void testAuthenticateWithInvalidCpf() {
        assertThatThrownBy(() -> authService.authenticate("123"))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("inválido")
                .extracting(e -> ((AuthService.AuthException) e).getStatusCode())
                .isEqualTo(400);

        assertThatThrownBy(() -> authService.authenticate("11111111111"))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void testAuthenticateWithUnknownCustomer() {
        // Arrange
        when(customerRepository.findByCpf(anyString())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticate("39053344705"))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("não encontrado")
                .extracting(e -> ((AuthService.AuthException) e).getStatusCode())
                .isEqualTo(404);
    }

    @Test
    void testAuthenticateWithInactiveCustomer() {
        // Arrange
        Customer inactiveCustomer = new Customer("11144477735", "INATIVO", false);
        when(customerRepository.findByCpf("11144477735")).thenReturn(inactiveCustomer);

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticate("111.444.777-35"))
                .isInstanceOf(AuthService.AuthException.class)
                .hasMessageContaining("inativo")
                .extracting(e -> ((AuthService.AuthException) e).getStatusCode())
                .isEqualTo(403);
    }

    @Test
    void testAuthenticateWithFormattedCpf() throws AuthService.AuthException {
        // Arrange
        Customer customer = new Customer("39053344705", "ATIVO", true);
        when(customerRepository.findByCpf("39053344705")).thenReturn(customer);
        when(tokenGenerator.generateToken("39053344705", "ATIVO")).thenReturn("token123");
        when(tokenGenerator.getExpiresInSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.authenticate("390.533.447-05");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
    }
}
