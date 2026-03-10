# OpenIt - Frontend

Interface web para o sistema de estacionamento OpenIt, construída com React, TypeScript, Vite e TailwindCSS.

---

## Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Decisões Técnicas](#decisões-técnicas)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Desenvolvimento Local](#desenvolvimento-local)
- [Docker](#docker)
- [Integração com Backend](#integração-com-backend)

---

## Visão Geral

O frontend do OpenIt é uma Single Page Application (SPA) que fornece duas funcionalidades principais:

1. **Terminal de Pagamento**: Usuário insere código do ticket, visualiza QR Code PIX e acompanha status do pagamento em tempo real.
2. **Terminal de Saída**: Usuário valida ticket pago e aciona abertura da cancela.

---

## Tecnologias

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| React | 19 | Biblioteca de UI com componentes reativos |
| TypeScript | 5.x | Tipagem estática para prevenção de erros |
| Vite | 6.x | Build tool e dev server com hot reload |
| TailwindCSS | 4 | Framework CSS utilitário |
| React Router | 7.x | Roteamento SPA |
| react-qr-code | 4.x | Geração de QR Codes client-side |

---

## Decisões Técnicas

### Por que React com TypeScript?

**Problema**: A interface precisa gerenciar estados complexos durante o fluxo de pagamento (aguardando código, gerando PIX, monitorando pagamento, aprovado).

**Solução**: React permite modelar cada estado como componente declarativo. TypeScript garante que transições de estado são válidas em tempo de compilação.

```typescript
// Estados tipados garantem consistência
type PaymentStatus = 'idle' | 'connecting' | 'waiting' | 'approved' | 'error';
```

### Por que Vite ao invés de Create React App?

**Problema**: CRA é lento para builds e hot reload em projetos modernos.

**Solução**: Vite usa ESM nativo durante desenvolvimento, resultando em startup instantâneo e hot reload em menos de 100ms. Build de produção é 10x mais rápido.

### Por que TailwindCSS?

**Problema**: CSS customizado consome tempo e cria inconsistências visuais.

**Solução**: TailwindCSS permite desenvolvimento rápido com classes utilitárias. O design system fica consistente e responsivo sem CSS adicional.

```tsx
// Estilização direta no componente
<button className="btn-primary bg-black text-white px-4 py-2 rounded">
  Gerar PIX
</button>
```

### Server-Sent Events (SSE) para Monitoramento

**Problema**: O usuário precisa saber quando o pagamento foi confirmado sem recarregar a página.

**Alternativas consideradas**:
1. **Polling**: Requisições periódicas ao servidor. Ineficiente e consome recursos.
2. **WebSocket**: Bidirecional, mas complexo para este caso de uso unidirecional.
3. **SSE**: Conexão unidirecional do servidor para cliente. Ideal para atualizações de status.

**Implementação**: Hook customizado `usePaymentStream` gerencia conexão SSE com reconexão automática.

```typescript
// Hook encapsula toda a lógica de conexão SSE
const { status, isApproved, reconnect } = usePaymentStream(paymentId);
```

**Características**:
- Reconexão automática com backoff exponencial
- Cleanup automático ao desmontar componente
- Estados tipados para UI reativa

### QR Code Client-side vs Server-side

**Problema**: Mercado Pago retorna QR Code como base64 ou como payload EMV (texto).

**Solução**: Detectamos o formato recebido e renderizamos apropriadamente:
- Base64: Renderiza como imagem diretamente
- EMV Payload: Gera QR Code client-side com `react-qr-code`

```typescript
function isBase64Image(str: string): boolean {
  if (str.startsWith('data:image/')) return true;
  if (/^[A-Za-z0-9+/=]+$/.test(str) && str.startsWith('iVBOR')) return true;
  return false;
}
```

---

## Estrutura do Projeto

```
src/
├── api/
│   └── client.ts         # Cliente API centralizado com tipagem
│
├── components/
│   ├── CopyButton.tsx    # Botão de copiar para clipboard
│   ├── LoadingSpinner.tsx
│   ├── Navigation.tsx    # Header de navegação
│   └── StatusBadge.tsx   # Badge de status do pagamento
│
├── hooks/
│   └── usePaymentStream.ts  # Hook SSE para monitoramento
│
├── pages/
│   ├── PaymentPage.tsx   # Fluxo de pagamento PIX
│   └── ExitPage.tsx      # Terminal de saída
│
├── types/
│   └── index.ts          # Tipos TypeScript compartilhados
│
├── utils/
│   └── date.ts           # Formatação de datas e valores
│
├── App.tsx               # Componente raiz com rotas
├── main.tsx              # Entry point
└── index.css             # Estilos globais + Tailwind
```

### Organização por Feature

Cada página é autocontida com sua lógica de estado e chamadas de API. Componentes compartilhados ficam em `/components`.

---

## Desenvolvimento Local

### Pré-requisitos

- Node.js 20+
- npm ou pnpm

### Instalação

```bash
npm install
npm run dev
```

A aplicação estará disponível em `http://localhost:3000`.

### Proxy de Desenvolvimento

O Vite proxy redireciona `/api/*` para o backend:

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

### Scripts Disponíveis

| Script | Descrição |
|--------|-----------|
| `npm run dev` | Servidor de desenvolvimento |
| `npm run build` | Build de produção |
| `npm run preview` | Preview do build |
| `npm run lint` | Verificar código com ESLint |

---

## Docker

### Build

```bash
docker build -t openit-front .
```

### Execução

```bash
docker run -p 3000:80 openit-front
```

### Configuração Nginx para SSE

O `nginx.conf` inclui configurações específicas para Server-Sent Events:

```nginx
location /api/payments/stream {
    proxy_pass http://api:8080;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400s;
}
```

---

## Integração com Backend

### Endpoints Consumidos

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/payments` | POST | Criar pagamento PIX |
| `/payments/stream/{id}` | GET (SSE) | Monitorar status |
| `/access/exit` | PUT | Registrar saída |

### Cliente API Tipado

O cliente centralizado garante tipagem em todas as chamadas:

```typescript
class ApiClient {
  async createPayment(request: CreatePaymentRequest): Promise<PaymentResponse>;
  async exitAccess(request: AccessExitRequest): Promise<AccessExitResponse>;
  getPaymentStreamUrl(paymentId: string): string;
}
```

### Tratamento de Erros

Erros de API são capturados e exibidos ao usuário com mensagens contextuais:

```typescript
} catch (err) {
  if (errorObj.status === 400) {
    message = 'Código inválido ou acesso não encontrado';
  } else if (errorObj.message.includes('Payment')) {
    message = 'Pagamento não confirmado. Efetue o pagamento primeiro.';
  }
}
```

---

## Licenca

GNU General Public License v2.0
