<div align="center">
  
# Libera.ai - Backend

### API REST para Gestao de Estacionamentos

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/Reactive-WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Mercado Pago](https://img.shields.io/badge/Mercado%20Pago-PIX-009EE3?style=for-the-badge&logo=mercadopago&logoColor=white)](https://www.mercadopago.com.br/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## Indice

- [Visao Geral](#visao-geral)
- [Tecnologias](#tecnologias)
- [Decisoes Tecnicas](#decisoes-tecnicas)
- [Arquitetura](#arquitetura)
- [Estrutura Modular](#estrutura-modular)
- [API Endpoints](#api-endpoints)
- [Configuracao e Execucao](#configuracao-e-execucao)
- [Integracao Mercado Pago](#integracao-mercado-pago)
- [Integracao IoT](#integracao-iot)

---

## Visao Geral

Backend da plataforma Libera.ai desenvolvido em Java 21 com Spring Boot. Fornece APIs REST para controle de acesso de veiculos e processamento de pagamentos via PIX.

### Funcionalidades

- Registro de entrada de veiculos (via Node-RED)
- Calculo de tarifa baseado em tempo de permanencia
- Geracao de pagamentos PIX via Mercado Pago
- Monitoramento de status em tempo real via SSE
- Validacao de pagamento para liberacao de saida
- Comunicacao com Node-RED para acionamento de cancela

---

## Tecnologias

| Tecnologia | Versao | Proposito |
|------------|--------|-----------|
| Java | 21 LTS | Runtime com Virtual Threads |
| Spring Boot | 3.5.11 | Framework de aplicacao |
| Spring WebFlux | 6.x | Programacao reativa e SSE |
| Spring Data JPA | 3.x | ORM e persistencia |
| Hibernate | 6.x | Implementacao JPA |
| MySQL | 8.0 | Banco de dados relacional |
| Mercado Pago SDK | 2.1.27 | Integracao de pagamentos |
| Lombok | Latest | Reducao de boilerplate |

---

## Decisoes Tecnicas

### Por que Java 21 com Virtual Threads?

**Problema**: O sistema precisa lidar com multiplas conexoes SSE simultaneas e chamadas HTTP para Node-RED e Mercado Pago.

**Alternativas consideradas**:
1. **Thread Pool tradicional**: Limita numero de conexoes simultaneas
2. **Programacao reativa completa**: Complexidade de codigo aumenta significativamente
3. **Virtual Threads**: Escala para milhares de conexoes com codigo sincrono tradicional

**Decisao**: Virtual Threads permitem codigo sincrono simples com escalabilidade de solucoes reativas.

```properties
spring.threads.virtual.enabled=true
```

### Por que Spring WebFlux para SSE?

**Problema**: O frontend precisa saber em tempo real quando o pagamento foi confirmado.

**Alternativas consideradas**:
1. **Polling do cliente**: Gera carga desnecessaria no servidor
2. **WebSocket**: Bidirecional, mas desnecessario para este caso unidirecional
3. **Server-Sent Events**: Nativo HTTP, unidirecional, ideal para atualizacoes de status

**Implementacao**:

```java
@GetMapping(path = "/stream/{paymentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<Boolean> streamPaymentStatus(@PathVariable String paymentId) {
    return Flux.interval(Duration.ofSeconds(1))
            .map(tick -> getPaymentStatusUseCase.execute(paymentId))
            .distinctUntilChanged();
}
```

O `distinctUntilChanged()` evita eventos duplicados quando o status nao muda.

### Por que Clean Architecture com DDD?

**Problema**: O sistema precisa integrar com multiplos sistemas externos (Mercado Pago, Node-RED) e deve ser facil de manter e testar.

**Decisao**: Separacao em bounded contexts (Access e Payment) com camadas bem definidas:

| Camada | Responsabilidade | Dependencias |
|--------|------------------|--------------|
| Presentation | Controllers, DTOs, validacao | Application |
| Application | Use Cases, orquestracao | Domain |
| Domain | Entidades, regras de negocio | Nenhuma |
| Infrastructure | JPA, APIs externas | Domain (via Ports) |

**Beneficio**: Trocar Mercado Pago por outro gateway requer apenas novo Adapter, sem alterar logica de negocio.

### Por que Hexagonal Architecture (Ports & Adapters)?

**Problema**: Integracao com sistemas externos (Mercado Pago, Node-RED) deve ser desacoplada da logica de negocio.

**Implementacao**:

```java
// Port (interface no dominio)
public interface PaymentProvider {
    PaymentInfo generatePayment(double amount);
}

// Adapter (implementacao na infraestrutura)
@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {
    @Override
    public PaymentInfo generatePayment(double amount) {
        // Integracao com SDK Mercado Pago
    }
}
```

### Por que Records para DTOs e Eventos?

**Problema**: DTOs e eventos de dominio devem ser imutaveis e thread-safe.

**Decisao**: Java Records garantem imutabilidade pelo compilador:

```java
public record ExitAccessEvent(int code) {}
public record PaymentInfo(String paymentId, String qrCode, double amount) {}
```

### Validacao na Camada de Apresentacao

**Problema**: Dados invalidos nao devem chegar a camada de aplicacao.

**Implementacao**: Jakarta Bean Validation nos DTOs:

```java
public record CreatePaymentRequest(
    @NotNull(message = "Access code is required")
    @Positive(message = "Access code must be positive")
    Integer accessCode
) {}
```

---

## Arquitetura

```mermaid
flowchart TB
    subgraph Presentation["PRESENTATION LAYER"]
        CTRL["Controllers REST"]
        DTO["DTOs"]
        MAPPER["Mappers"]
    end
    
    subgraph Application["APPLICATION LAYER"]
        UC_ACCESS["Access Use Cases"]
        UC_PAYMENT["Payment Use Cases"]
    end
    
    subgraph Domain["DOMAIN LAYER"]
        subgraph AccessBC["Access Module"]
            ACCESS_MODEL["Access Entity"]
            ACCESS_PORT["Ports/Interfaces"]
        end
        
        subgraph PaymentBC["Payment Module"]
            PAYMENT_MODEL["Payment Entity"]
            PAYMENT_PORT["Ports/Interfaces"]
        end
    end
    
    subgraph Infrastructure["INFRASTRUCTURE LAYER"]
        JPA["JPA Repositories"]
        MP["Mercado Pago Adapter"]
        NODE_PROD["Node-RED Adapter"]
    end
    
    subgraph External["SISTEMAS EXTERNOS"]
        MERCADO["Mercado Pago API"]
        NODE["Node-RED"]
        MYSQL[("MySQL")]
    end
    
    CTRL --> UC_ACCESS
    CTRL --> UC_PAYMENT
    
    UC_ACCESS --> ACCESS_MODEL
    UC_ACCESS --> ACCESS_PORT
    UC_PAYMENT --> PAYMENT_MODEL
    UC_PAYMENT --> PAYMENT_PORT
    
    ACCESS_PORT -.implements.-> JPA
    ACCESS_PORT -.implements.-> NODE_PROD
    PAYMENT_PORT -.implements.-> JPA
    PAYMENT_PORT -.implements.-> MP
    
    JPA --> MYSQL
    MP --> MERCADO
    NODE_PROD --> NODE
```

---

## Estrutura Modular

```
src/main/java/br/centroweg/libera_ai/
│
├── Application.java                    # Bootstrap Spring Boot
│
├── module/
│   ├── access/                         # Modulo de Controle de Acesso
│   │   ├── presentation/
│   │   │   ├── controller/
│   │   │   │   └── AccessController.java
│   │   │   ├── dto/
│   │   │   │   ├── AccessExitRequest.java
│   │   │   │   └── AccessExitResponse.java
│   │   │   └── mapper/
│   │   │       └── AccessMapper.java
│   │   │
│   │   ├── application/
│   │   │   └── use_case/
│   │   │       └── AccessExitUseCase.java
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Access.java
│   │   │   ├── event/
│   │   │   │   └── ExitAccessEvent.java
│   │   │   └── port/
│   │   │       ├── AccessRepository.java
│   │   │       └── ExitEventProducer.java
│   │   │
│   │   └── infrastructure/
│   │       ├── persistence/
│   │       │   └── repository/
│   │       │       └── AccessRepositoryAdapter.java
│   │       └── producer/
│   │           └── NodeExitEventProducer.java
│   │
│   └── payment/                        # Modulo de Pagamentos
│       ├── presentation/
│       │   ├── controller/
│       │   │   └── PaymentController.java
│       │   ├── dto/
│       │   │   ├── CreatePaymentRequest.java
│       │   │   ├── PaymentResponse.java
│       │   │   └── MercadoPagoWebhookRequest.java
│       │   └── mapper/
│       │       └── PaymentMapper.java
│       │
│       ├── application/
│       │   └── use_case/
│       │       ├── CreatePaymentUseCase.java
│       │       ├── GetPaymentStatusUseCase.java
│       │       └── ProcessPaymentNotificationUseCase.java
│       │
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Payment.java
│       │   │   └── PaymentInfo.java
│       │   └── port/
│       │       ├── PaymentRepository.java
│       │       └── PaymentProvider.java
│       │
│       └── infrastructure/
│           └── payment/
│               └── MercadoPagoPaymentProvider.java
│
└── share/
    └── config/                         # Configuracoes globais
```

---

## API Endpoints

### Modulo de Acesso

#### PUT /access/exit

Registra saida de veiculo e aciona abertura da cancela.

**Request:**
```json
{
  "code": 12345
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "entryDate": "20/02/2026 08:00:00",
  "exitDate": "20/02/2026 17:30:00"
}
```

**Erros:**

| Codigo | Descricao |
|--------|-----------|
| 400 | Codigo invalido ou acesso nao encontrado |
| 500 | Falha na comunicacao com Node-RED |

### Modulo de Pagamentos

#### POST /payments

Cria pagamento e gera QR Code PIX.

**Request:**
```json
{
  "accessCode": 12345
}
```

**Response (201 Created):**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440000",
  "qrCode": "iVBORw0KGgoAAAANSUhEUgAA...",
  "amount": 20.0
}
```

**Regra de Calculo**: R$ 10,00 por hora, arredondado para cima.

#### GET /payments/stream/{paymentId}

Stream SSE para monitoramento de pagamento.

**Headers:**
```
Accept: text/event-stream
```

**Response:**
```
data: false

data: false

data: true
```

#### POST /payments/webhook

Webhook para notificacoes do Mercado Pago.

**Request (enviada pelo Mercado Pago):**
```json
{
  "action": "payment.updated",
  "data": {
    "id": "12345678"
  }
}
```

---

## Configuracao e Execucao

### Variaveis de Ambiente

```env
# Banco de Dados
DB_ROOT_PASSWORD=senha_root
DB_NAME=libera_db
DB_USER=libera_user
DB_PASSWORD=senha_usuario

# Mercado Pago
MERCADOPAGO_ACCESS_TOKEN=seu_token

# Node-RED
NODE_HOST=172.17.0.1
NODE_PORT=1880
```

### Docker Compose

```bash
docker compose up -d --build
```

### Desenvolvimento Local

```bash
./mvnw spring-boot:run
```

---

## Integracao Mercado Pago

### Geracao de Pagamento PIX

```java
@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {
    
    @Override
    public PaymentInfo generatePayment(double amount) {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
            .transactionAmount(BigDecimal.valueOf(amount))
            .paymentMethodId("pix")
            .payer(PaymentPayerRequest.builder()
                .email(defaultEmail)
                .build())
            .build();
            
        Payment payment = paymentClient.create(request);
        String qrCode = payment.getPointOfInteraction()
                               .getTransactionData()
                               .getQrCodeBase64();
        
        return new PaymentInfo(String.valueOf(payment.getId()), qrCode, amount);
    }
}
```

### Webhook de Confirmacao

Mercado Pago envia notificacao para `/payments/webhook` quando pagamento e confirmado. O `ProcessPaymentNotificationUseCase` atualiza o status no banco.

---

## Integracao IoT

### Comunicacao com Node-RED

O `NodeExitEventProducer` envia comandos HTTP para Node-RED abrir a cancela:

```java
@Component
public class NodeExitEventProducer implements ExitEventProducer {

    @Override
    public void send(ExitAccessEvent event) {
        try {
            log.info("Enviando sinal de liberacao para codigo: {}", event.code());
            restTemplate.postForEntity(nodeUrl, event, Void.class);
        } catch (Exception e) {
            log.error("Falha na comunicacao com Node-RED: {}", e.getMessage());
            throw new RuntimeException("Erro de integracao IoT", e);
        }
    }
}
```

Node-RED recebe o comando HTTP e publica via MQTT para o ESP32.

---

## Licenca

GNU General Public License v2.0

---

<div align="center">

**Desenvolvido por Centro WEG**

</div>
