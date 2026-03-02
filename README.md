<div align="center">
  
# 🔐 Libera.ai

### Plataforma Inteligente de Gestão de Estacionamentos com Pagamento Automático

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/Reactive-WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Mercado Pago](https://img.shields.io/badge/Mercado%20Pago-PIX-009EE3?style=for-the-badge&logo=mercadopago&logoColor=white)](https://www.mercadopago.com.br/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-GPL%20v2-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Sobre o Projeto

O **Libera.ai** é uma solução completa e moderna para **gestão de estacionamentos inteligentes**, integrando:

- 🚗 **Controle de Acesso Físico** via IoT (ESP32)
- 💳 **Processamento de Pagamentos** via PIX (Mercado Pago)
- 🎨 **Interface Web Responsiva** para terminais de saída
- 🏗️ **Arquitetura Modular** baseada em Clean Architecture e DDD

### 🌟 Principais Funcionalidades

- ✅ Registro automático de entrada/saída de veículos
- 💰 Cálculo automático de tarifa baseado em tempo de permanência
- 📱 Geração de QR Code PIX para pagamento instantâneo
- 🔄 Monitoramento de pagamento em tempo real (Server-Sent Events)
- 🚪 Integração com catracas/cancelas via ESP32
- 📊 Rastreamento completo com histórico em banco de dados

---

## 📂 Estrutura do Repositório

```
Libera.ai/
├── back/              # Backend - API REST (Java/Spring Boot)
│   ├── src/          # Código-fonte modular (Access + Payment)
│   ├── Dockerfile    # Container da aplicação
│   ├── compose.yml   # Orquestração Docker
│   └── README.md     # 📘 Documentação técnica completa
│
└── front/            # Frontend - Interface Web
    └── index.html    # Terminal de saída (HTML/TailwindCSS)
```

---

## 🚀 Quick Start

### Pré-requisitos

- Docker 20+ e Docker Compose
- Token de acesso do Mercado Pago ([obtenha aqui](https://www.mercadopago.com.br/developers))

### 1. Configuração

Crie o arquivo `.env` na pasta `back/`:

```env
# Banco de Dados
DB_ROOT_PASSWORD=sua_senha_segura
DB_NAME=libera_db
DB_USER=libera_user
DB_PASSWORD=sua_senha_segura

# Mercado Pago
MERCADOPAGO_ACCESS_TOKEN=seu_token_aqui

# Node.js (Orchestrator IoT)
NODE_HOST=172.17.0.1
NODE_PORT=3000
```

### 2. Executar

```bash
cd back/
docker compose up -d --build
```

### 3. Acessar

- **API**: http://localhost:8080
- **Terminal Web**: Abrir `front/index.html` no navegador
- **Health Check**: http://localhost:8080/actuator/health

---

## 📚 Documentação Completa

Para informações detalhadas sobre:

- 🏗️ **Arquitetura e Design Patterns**
- 💎 **Estrutura Modular (Bounded Contexts)**
- 🎨 **Camada de Apresentação (Presentation Layer)**
- 💳 **Sistema de Pagamentos (Mercado Pago PIX)**
- 🔌 **API Endpoints Completos**
- ⚡ **Detalhes de Engenharia (WebFlux, Virtual Threads, DDD)**

**👉 Consulte a [documentação técnica completa no backend](./back/README.md)**

---

## 🏗️ Arquitetura em Alto Nível

```mermaid
flowchart TB
    subgraph Frontend["🎨 Frontend"]
        WEB["Interface Web<br/>(HTML/TailwindCSS)"]
    end
    
    subgraph Backend["☕ Backend - Spring Boot"]
        ACCESS["Access Module<br/>(Controle de Acesso)"]
        PAYMENT["Payment Module<br/>(Pagamentos PIX)"]
    end
    
    subgraph External["🌐 Sistemas Externos"]
        MP["Mercado Pago API"]
        NODE["Node.js Orchestrator"]
        ESP["ESP32 (Catraca)"]
    end
    
    subgraph Storage["💾 Armazenamento"]
        DB[("MySQL Database")]
    end
    
    WEB -->|REST API| ACCESS
    WEB -->|REST API| PAYMENT
    ACCESS --> DB
    PAYMENT --> DB
    PAYMENT -->|QR Code PIX| MP
    ACCESS -->|ExitEvent| NODE
    NODE -->|Comando| ESP
    
    style Frontend fill:#e8f5e9,stroke:#2e7d32
    style Backend fill:#e1f5fe,stroke:#01579b
    style External fill:#fce4ec,stroke:#c2185b
    style Storage fill:#f3e5f5,stroke:#7b1fa2
```

---

## 🛠️ Tech Stack Resumido

| Camada | Tecnologias |
|--------|-------------|
| **Backend** | Java 21, Spring Boot 3.5, Spring WebFlux, JPA/Hibernate |
| **Pagamentos** | Mercado Pago SDK Java, PIX |
| **Frontend** | HTML5, TailwindCSS, Vanilla JavaScript |
| **Banco de Dados** | MySQL 8.0 |
| **IoT** | ESP32, Node.js Orchestrator |
| **DevOps** | Docker, Docker Compose |

---

## 📊 Fluxo do Sistema

### Jornada Completa do Usuário

1. 🚗 **Entrada**: Veículo detectado → Código gerado → Catraca abre
2. 🕐 **Permanência**: Veículo estacionado (tempo calculado automaticamente)
3. 💳 **Pagamento**: Terminal web → QR Code PIX → Pagamento via app bancário
4. ✅ **Saída**: Pagamento confirmado → Catraca liberada → Saída registrada

---

## 🎯 Diferenciais Técnicos

| Característica | Implementação |
|----------------|---------------|
| **Modularidade** | Bounded Contexts (DDD) com módulos Access e Payment |
| **Escalabilidade** | Clean Architecture + Hexagonal Architecture |
| **Performance** | Java 21 Virtual Threads + WebFlux Reactive Streams |
| **Tempo Real** | Server-Sent Events (SSE) para status de pagamento |
| **Pagamentos** | Integração nativa com Mercado Pago PIX |
| **IoT** | Comunicação resiliente com ESP32 via Node.js |

---

## 📜 Licença

Este projeto está licenciado sob a **GNU General Public License v2.0** - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

## 👥 Desenvolvido por

<div align="center">

**Centro WEG**

Desenvolvido com ☕ Java, 💚 Spring Boot e 🚀 paixão por tecnologia

</div>
