# Libera.ai - Frontend

Interface web para o sistema de estacionamento Libera.ai, construída com React, TypeScript, Vite e TailwindCSS.

## 🚀 Tecnologias

- **React 19** - Biblioteca de interface de usuário
- **TypeScript** - Tipagem estática
- **Vite** - Build tool e dev server
- **TailwindCSS 4** - Framework CSS utilitário
- **React Router** - Roteamento SPA
- **react-qr-code** - Geração de QR Codes

## 📁 Estrutura do Projeto

```
src/
├── api/              # Cliente API (fetch)
├── components/       # Componentes reutilizáveis
├── hooks/            # React hooks customizados
├── pages/            # Páginas da aplicação
├── types/            # Tipos TypeScript
├── index.css         # Estilos globais + Tailwind
├── main.tsx          # Entry point
└── App.tsx           # Componente raiz com rotas
```

## 🛠️ Desenvolvimento Local

### Pré-requisitos

- Node.js 20+
- npm ou pnpm

### Instalação

```bash
# Instalar dependências
npm install

# Rodar em modo de desenvolvimento
npm run dev
```

A aplicação estará disponível em `http://localhost:3000`.

### Variáveis de Ambiente

Em desenvolvimento, a API é acessada via proxy do Vite (configurado em `vite.config.ts`).
O proxy redireciona `/api/*` para `http://localhost:8080/*`.

Para produção, crie um arquivo `.env.production`:

```env
VITE_API_URL=/api
```

### Scripts Disponíveis

```bash
npm run dev      # Servidor de desenvolvimento com hot reload
npm run build    # Build de produção
npm run preview  # Preview do build de produção
npm run lint     # Verificar código com ESLint
```

## 🐳 Docker

### Build da Imagem

```bash
docker build -t libera-front .
```

### Execução Standalone

```bash
docker run -p 3000:80 libera-front
```

### Com Docker Compose (Recomendado)

Na raiz do projeto:

```bash
docker compose up -d --build
```

## 🎨 Design System

A interface utiliza uma paleta **preto e branco** com tons de cinza, seguindo princípios de design minimalista:

- **Cores principais**: Preto (#000), Branco (#FFF), tons de cinza
- **Tipografia**: Inter (Google Fonts)
- **Componentes**: Cards com bordas sutis, botões com feedback visual
- **Estados**: Loading discreto, badges de status coloridos

## 📱 Páginas

### 1. Pagamento PIX (`/`)

- Campo para inserir código do ticket
- Geração de QR Code PIX
- Código copia-e-cola com botão de copiar
- Monitoramento em tempo real via SSE
- Estados: Aguardando → Aprovado

### 2. Terminal de Saída (`/exit`)

- Validação do ticket após pagamento
- Exibição de entrada, saída e tempo total
- Liberação da cancela

## 🔌 Integração com Backend

A aplicação se comunica com a API REST do backend:

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/payments` | POST | Criar pagamento PIX |
| `/payments/stream/{id}` | GET (SSE) | Monitorar status do pagamento |
| `/access/exit` | PUT | Registrar saída e liberar cancela |

### Server-Sent Events (SSE)

O hook `usePaymentStream` implementa:
- Reconexão automática com backoff exponencial
- Estados: `idle`, `connecting`, `waiting`, `approved`, `error`
- Cleanup automático ao desmontar/trocar de rota
- Fallback para navegadores sem suporte SSE

## 🔧 Troubleshooting

### CORS em Desenvolvimento

Em desenvolvimento local, o Vite proxy resolve problemas de CORS.
Se ainda houver erro, verifique se o backend está rodando em `localhost:8080`.

### SSE não conecta

1. Verifique se o backend está rodando
2. Confirme que a rota `/payments/stream/{id}` está acessível
3. No Docker, certifique-se que o proxy nginx está configurado corretamente

### Build falha

```bash
# Limpar cache e reinstalar
rm -rf node_modules dist
npm install
npm run build
```

## 📜 Licença

GNU General Public License v2.0
