# Java Lambda Auth - Estrutura de Projeto

## Estrutura de Diretórios

```text
autoservice-lambda-auth/
├── .github/workflows/
│   └── ci-cd.yml
├── src/
│   ├── main/
│   │   ├── java/com/autoservice/lambda/
│   │   │   ├── dto/
│   │   │   │   ├── AuthRequest.java
│   │   │   │   └── AuthResponse.java
│   │   │   ├── model/
│   │   │   │   └── Customer.java
│   │   │   ├── repository/
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   └── DatabaseConnection.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   └── JwtTokenGenerator.java
│   │   │   ├── util/
│   │   │   │   └── CpfValidator.java
│   │   │   └── AuthHandler.java
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       ├── java/com/autoservice/lambda/
│       │   ├── service/
│       │   │   ├── AuthServiceTest.java
│       │   │   └── JwtTokenGeneratorTest.java
│       │   └── util/
│       │       └── CpfValidatorTest.java
│       └── resources/
│           └── logback-test.xml
├── pom.xml
├── README.md
└── STRUCTURE.md
```

## Observações

- O repositório está padronizado em **Java 21 + Maven**.
- O empacotamento final gera um **fat JAR** para deployment no AWS Lambda.
- A integração com banco e JWT é feita pelas classes em `repository/` e `service/`.
