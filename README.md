<div align="center">
  
# Libera.ai

### Sistema Inteligente de Gestão de Estacionamentos com IoT e Pagamento PIX

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/Reactive-WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Mercado Pago](https://img.shields.io/badge/Mercado%20Pago-PIX-009EE3?style=for-the-badge&logo=mercadopago&logoColor=white)](https://www.mercadopago.com.br/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-GPL%20v2-blue?style=for-the-badge)](LICENSE)

</div>

---

## Indice

- [Problema](#problema)
- [Solucao](#solucao)
- [Fluxo do Sistema](#fluxo-do-sistema)
- [Tecnologias e Justificativas](#tecnologias-e-justificativas)
- [Arquitetura do Sistema](#arquitetura-do-sistema)
- [Estrutura do Repositorio](#estrutura-do-repositorio)
- [Configuracao e Instalacao](#configuracao-e-instalacao)
- [Documentacao Tecnica Detalhada](#documentacao-tecnica-detalhada)
- [Licenca](#licenca)

---

## Problema

Estacionamentos comerciais enfrentam diversos desafios operacionais que impactam diretamente a experiencia do usuario e a eficiencia do negocio:

**Processos Manuais e Lentos**
- Cobranca manual de tarifas propensa a erros de calculo
- Filas longas nos caixas de pagamento, especialmente em horarios de pico
- Necessidade de operadores humanos para cada transacao

**Controle de Acesso Ineficiente**
- Cancelas operadas manualmente ou com sistemas desconectados
- Impossibilidade de rastrear tempo real de permanencia
- Falta de integracao entre entrada, permanencia e saida

**Metodos de Pagamento Limitados**
- Dependencia de dinheiro ou cartao fisico
- Dificuldade em adotar pagamentos digitais modernos como PIX
- Processos de conciliacao financeira complexos

**Sistemas Legados**
- Solucoes antigas dificeis de escalar e manter
- Integracao complexa com novos dispositivos IoT
- Falta de visibilidade em tempo real das operacoes

---

## Solucao

O **Libera.ai** e uma plataforma completa que automatiza todo o ciclo operacional do estacionamento, desde a deteccao da entrada ate a liberacao da saida com pagamento validado.

O sistema utiliza sensores IoT (ESP32) para detectar veiculos automaticamente, comunicacao MQTT para transmissao de dados em tempo real, processamento de pagamentos via PIX com o Mercado Pago, e uma interface web responsiva para interacao do usuario.

### Componentes da Solucao

| Componente | Funcao | Tecnologia Principal |
|------------|--------|----------------------|
| **Deteccao de Entrada** | Sensores identificam veiculos e geram codigo unico | ESP32 + Sensor |
| **Comunicacao IoT** | Transmissao de eventos entre dispositivos | MQTT + Broker Publico |
| **Orquestracao** | Recebe eventos MQTT e interage com backend | Node-RED |
| **Backend API** | Logica de negocio e integracao de pagamentos | Spring Boot + WebFlux |
| **Pagamentos** | Geracao de QR Code PIX e confirmacao | Mercado Pago SDK |
| **Interface Web** | Terminal de pagamento e liberacao de saida | React + TypeScript |
| **Banco de Dados** | Persistencia de acessos e pagamentos | MySQL |

### Diferenciais

- **Pagamento PIX**: Metodo de pagamento instantaneo e sem taxas para o usuario
- **Tempo Real**: Atualizacoes de status via Server-Sent Events (SSE)
- **Automacao Completa**: Desde a deteccao ate a liberacao sem intervencao humana
- **Arquitetura Moderna**: Clean Architecture e DDD para escalabilidade e manutencao

---

## Fluxo do Sistema

O sistema opera em um ciclo completo que vai desde a deteccao de entrada do veiculo ate a liberacao de saida apos pagamento confirmado.

### Diagrama de Fluxo Completo

```mermaid
sequenceDiagram
    actor User as Usuario/Veiculo
    participant ESP as ESP32<br/>(Sensor)
    participant MQTT as Broker MQTT<br/>(Publico)
    participant NodeRED as Node-RED<br/>(Orquestrador)
    participant API as Backend API<br/>(Spring Boot)
    participant DB as MySQL<br/>Database
    participant MP as Mercado Pago<br/>API
    participant Web as Interface Web<br/>(Terminal)

    Note over User,Web: FASE 1: ENTRADA NO ESTACIONAMENTO
    User->>ESP: Veiculo detectado pelo sensor
    ESP->>ESP: Gera codigo unico (primeira deteccao)
    ESP->>MQTT: Publica codigo via MQTT
    MQTT->>NodeRED: Entrega mensagem
    NodeRED->>DB: INSERT access_record (code, entry_time)
    
    Note over User,Web: FASE 2: PERMANENCIA
    User->>User: Veiculo estacionado (tempo sendo contado)
    
    Note over User,Web: FASE 3: PAGAMENTO
    User->>Web: Acessa terminal e insere codigo
    Web->>API: POST /payments {accessCode}
    API->>DB: SELECT access WHERE code = ?
    DB-->>API: Retorna registro de entrada
    API->>API: Calcula tempo e valor (R$ 10/hora)
    API->>MP: Cria pagamento PIX
    MP-->>API: Retorna QR Code + payment_id
    API->>DB: INSERT payment (PENDING)
    API-->>Web: {qrCode, amount, paymentId}
    Web->>User: Exibe QR Code PIX

    Note over User,Web: FASE 4: CONFIRMACAO DE PAGAMENTO
    User->>MP: Escaneia e paga via app bancario
    MP->>API: Webhook: pagamento confirmado
    API->>DB: UPDATE payment SET paid = true
    
    Note over User,Web: FASE 5: MONITORAMENTO EM TEMPO REAL
    Web->>API: GET /payments/stream/{id} (SSE)
    loop A cada 1 segundo
        API->>DB: SELECT paid FROM payment
        API-->>Web: Event: true/false
    end
    Web->>User: Notifica: Pagamento Aprovado

    Note over User,Web: FASE 6: LIBERACAO DE SAIDA
    User->>Web: Solicita liberacao com codigo
    Web->>API: PUT /access/exit {code}
    API->>DB: Valida pagamento confirmado
    DB-->>API: Pagamento OK
    API->>DB: UPDATE access SET exit_time = NOW()
    API->>NodeRED: POST /open-gate {code}
    NodeRED->>MQTT: Publica comando de abertura
    MQTT->>ESP: Entrega comando
    ESP->>ESP: Abre cancela por 15 segundos
    ESP-->>User: Cancela aberta
```

### Detalhamento das Fases

| Fase | Descricao | Componentes Envolvidos |
|------|-----------|------------------------|
| **1. Entrada** | Sensor ESP32 detecta veiculo e gera codigo unico. Codigo e publicado via MQTT e Node-RED insere no banco de dados. | ESP32, MQTT Broker, Node-RED, MySQL |
| **2. Permanencia** | Veiculo permanece no estacionamento. Sistema registra tempo de entrada para calculo posterior. | MySQL |
| **3. Pagamento** | Usuario insere codigo no terminal web. Sistema calcula valor baseado no tempo e gera QR Code PIX via Mercado Pago. | Frontend, Backend, Mercado Pago |
| **4. Confirmacao** | Usuario paga via PIX. Mercado Pago envia webhook ao backend confirmando pagamento. | Mercado Pago, Backend, MySQL |
| **5. Monitoramento** | Frontend mantem conexao SSE com backend, recebendo atualizacoes em tempo real sobre status do pagamento. | Frontend, Backend (WebFlux) |
| **6. Liberacao** | Usuario solicita saida. Backend valida pagamento e envia comando via HTTP para Node-RED, que publica via MQTT para ESP32 abrir a cancela. | Frontend, Backend, Node-RED, MQTT, ESP32 |

---

## Tecnologias e Justificativas

A escolha de cada tecnologia foi baseada em requisitos tecnicos e limitacoes do projeto academico.

### Backend

| Tecnologia | Justificativa |
|------------|---------------|
| **Java 21** | Linguagem robusta com Virtual Threads para alta concorrencia. Ecossistema maduro e ampla documentacao. |
| **Spring Boot 3.5** | Framework padrao de mercado para APIs REST. Facilita configuracao e integracao com banco de dados e servicos externos. |
| **Spring WebFlux** | Suporte nativo a Server-Sent Events (SSE) para atualizacoes em tempo real sem polling constante do cliente. |
| **MySQL 8.0** | Banco de dados relacional confiavel. Ideal para dados transacionais como acessos e pagamentos. |
| **Mercado Pago SDK** | SDK oficial para integracao PIX. Suporte a QR Code dinamico e webhooks para notificacao de pagamento. |

### Frontend

| Tecnologia | Justificativa |
|------------|---------------|
| **React 19** | Biblioteca moderna para interfaces reativas. Facilita gerenciamento de estado durante fluxo de pagamento. |
| **TypeScript** | Tipagem estatica previne erros em tempo de desenvolvimento. Melhora manutencao do codigo. |
| **Vite** | Build tool rapida com hot reload. Melhora produtividade durante desenvolvimento. |
| **TailwindCSS 4** | Estilizacao utilitaria permite desenvolvimento rapido de interface responsiva sem CSS customizado extenso. |

### IoT e Comunicacao

| Tecnologia | Justificativa |
|------------|---------------|
| **ESP32** | Microcontrolador com WiFi integrado. Baixo custo e amplamente usado em projetos IoT academicos. |
| **MQTT** | Protocolo leve ideal para IoT. Comunicacao assíncrona entre dispositivos com baixo consumo de recursos. |
| **Broker Publico** | Elimina necessidade de infraestrutura propria para o projeto academico. |
| **Node-RED** | Ferramenta visual para orquestracao de fluxos IoT. Conecta MQTT ao backend sem necessidade de codigo complexo. |

### Infraestrutura

| Tecnologia | Justificativa |
|------------|---------------|
| **Docker** | Containerizacao garante ambiente consistente entre desenvolvimento e producao. |
| **Docker Compose** | Orquestracao simples de multiplos containers (frontend, backend, banco). |

---

## Arquitetura do Sistema

O backend foi projetado seguindo principios de **Clean Architecture** e **Domain-Driven Design (DDD)**, organizando o codigo em bounded contexts independentes.

### Visao Geral

```mermaid
flowchart TB
    subgraph IoT["Camada IoT"]
        ESP["ESP32<br/>(Sensores)"]
        MQTT["Broker MQTT"]
        NodeRED["Node-RED"]
    end
    
    subgraph Frontend["Camada de Apresentacao"]
        WEB["React + TypeScript"]
    end
    
    subgraph Backend["Camada de Aplicacao - Spring Boot"]
        CTRL["Controllers REST"]
        UC["Use Cases"]
        DOMAIN["Domain Models"]
        INFRA["Infrastructure"]
    end
    
    subgraph External["Servicos Externos"]
        MP["Mercado Pago"]
    end
    
    subgraph Storage["Persistencia"]
        DB[("MySQL")]
    end
    
    ESP <-->|MQTT| MQTT
    MQTT <--> NodeRED
    NodeRED -->|HTTP| CTRL
    WEB -->|HTTP REST| CTRL
    CTRL --> UC
    UC --> DOMAIN
    UC --> INFRA
    INFRA --> DB
    INFRA -->|HTTPS| MP
    MP -->|Webhook| CTRL
    CTRL -->|HTTP| NodeRED
```

### Camadas do Backend

| Camada | Responsabilidade |
|--------|------------------|
| **Presentation** | Controllers REST, DTOs, validacao de entrada |
| **Application** | Use Cases que orquestram logica de negocio |
| **Domain** | Entidades e regras de negocio puras |
| **Infrastructure** | Repositorios JPA, integracao Mercado Pago, comunicacao Node-RED |

---

## Estrutura do Repositorio

```
Libera.ai/
├── back/                          # Backend - API REST (Java/Spring Boot)
│   ├── src/
│   │   └── main/java/br/centroweg/libera_ai/
│   │       ├── module/
│   │       │   ├── access/           # Modulo de Controle de Acesso
│   │       │   │   ├── presentation/    # Controllers, DTOs
│   │       │   │   ├── application/     # Use Cases
│   │       │   │   ├── domain/          # Entidades, Portas
│   │       │   │   └── infrastructure/  # Repositorios, Adaptadores
│   │       │   │
│   │       │   └── payment/          # Modulo de Pagamentos
│   │       │       ├── presentation/    # Controllers, DTOs
│   │       │       ├── application/     # Use Cases
│   │       │       ├── domain/          # Entidades, Portas
│   │       │       └── infrastructure/  # Repositorios, Mercado Pago
│   │       │
│   │       └── share/            # Codigo compartilhado
│   │
│   ├── Dockerfile
│   ├── compose.yml
│   ├── pom.xml
│   └── README.md                 # Documentacao tecnica do backend
│
├── front/                        # Frontend - Interface Web
│   ├── src/
│   │   ├── api/                  # Cliente API
│   │   ├── components/           # Componentes React
│   │   ├── hooks/                # Hooks customizados (SSE)
│   │   ├── pages/                # Paginas da aplicacao
│   │   └── types/                # Tipos TypeScript
│   │
│   ├── Dockerfile
│   ├── package.json
│   └── README.md                 # Documentacao tecnica do frontend
│
├── docker-compose.yml            # Orquestracao completa
└── README.md                     # Este arquivo
```

---

## Configuracao e Instalacao

### Pre-requisitos

- Docker 20+ e Docker Compose 1.29+
- Token de acesso do Mercado Pago ([obter aqui](https://www.mercadopago.com.br/developers))
- Node-RED configurado com broker MQTT (para integracao IoT completa)

### Configuracao de Variaveis de Ambiente

Crie o arquivo `.env` na raiz do projeto:

```env
# Banco de Dados MySQL
DB_ROOT_PASSWORD=sua_senha_root_segura
DB_NAME=libera_db
DB_USER=libera_user
DB_PASSWORD=sua_senha_usuario_segura

# Mercado Pago
MERCADOPAGO_ACCESS_TOKEN=seu_access_token_mercadopago

# Node-RED (orquestrador IoT)
NODE_HOST=172.17.0.1
NODE_PORT=1880
```

### Execucao com Docker Compose

```bash
# Iniciar todos os servicos
docker compose up -d --build

# Verificar status
docker compose ps

# Ver logs
docker compose logs -f
```

### Endpoints Disponiveis

| Servico | URL | Descricao |
|---------|-----|-----------|
| Frontend | http://localhost:3000 | Interface web do terminal |
| Backend API | http://localhost:8080 | API REST |
| Health Check | http://localhost:8080/actuator/health | Status da aplicacao |

### Variaveis de Ambiente

| Variavel | Descricao |
|----------|-----------|
| `DB_ROOT_PASSWORD` | Senha root do MySQL |
| `DB_NAME` | Nome do banco de dados |
| `DB_USER` | Usuario do banco |
| `DB_PASSWORD` | Senha do usuario |
| `MERCADOPAGO_ACCESS_TOKEN` | Token de acesso Mercado Pago |
| `NODE_HOST` | Host do Node-RED |
| `NODE_PORT` | Porta do Node-RED |

---

## Documentacao Tecnica Detalhada

Para informacoes tecnicas detalhadas sobre cada componente, consulte:

- **[Backend README](./back/README.md)**: Arquitetura, endpoints, integracao Mercado Pago, decisoes tecnicas
- **[Frontend README](./front/README.md)**: Componentes React, hooks SSE, integracao com API

---

## Licenca

Este projeto esta licenciado sob a **GNU General Public License v2.0**.

A GPL v2.0 garante aos usuarios as liberdades de usar, estudar, compartilhar e modificar o software. Para mais detalhes, consulte o arquivo [LICENSE](LICENSE).

---

## Autores

**Centro WEG**

Projeto academico desenvolvido com foco em arquitetura limpa, integracao IoT e boas praticas de engenharia de software.
