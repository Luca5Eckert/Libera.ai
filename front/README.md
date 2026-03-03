# Libera.ai - Frontend

Interface web para o sistema de estacionamento Libera.ai, construida com React, TypeScript, Vite e TailwindCSS.

---

## Indice

- [Visao Geral](#visao-geral)
- [Tecnologias](#tecnologias)
- [Decisoes Tecnicas](#decisoes-tecnicas)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Desenvolvimento Local](#desenvolvimento-local)
- [Docker](#docker)
- [Integracao com Backend](#integracao-com-backend)

---

## Visao Geral

O frontend do Libera.ai e uma Single Page Application (SPA) que fornece duas funcionalidades principais:

1. **Terminal de Pagamento**: Usuario insere codigo do ticket, visualiza QR Code PIX e acompanha status do pagamento em tempo real.
2. **Terminal de Saida**: Usuario valida ticket pago e aciona abertura da cancela.

---

## Tecnologias

| Tecnologia | Versao | Proposito |
|------------|--------|-----------|
| React | 19 | Biblioteca de UI com componentes reativos |
| TypeScript | 5.x | Tipagem estatica para prevencao de erros |
| Vite | 6.x | Build tool e dev server com hot reload |
| TailwindCSS | 4 | Framework CSS utilitario |
| React Router | 7.x | Roteamento SPA |
| react-qr-code | 4.x | Geracao de QR Codes client-side |

---

## Decisoes Tecnicas

### Por que React com TypeScript?

**Problema**: A interface precisa gerenciar estados complexos durante o fluxo de pagamento (aguardando codigo, gerando PIX, monitorando pagamento, aprovado).

**Solucao**: React permite modelar cada estado como componente declarativo. TypeScript garante que transicoes de estado sao validas em tempo de compilacao.

```typescript
// Estados tipados garantem consistencia
type PaymentStatus = 'idle' | 'connecting' | 'waiting' | 'approved' | 'error';
```

### Por que Vite ao inves de Create React App?

**Problema**: CRA e lento para builds e hot reload em projetos modernos.

**Solucao**: Vite usa ESM nativo durante desenvolvimento, resultando em startup instantaneo e hot reload em menos de 100ms. Build de producao e 10x mais rapido.

### Por que TailwindCSS?

**Problema**: CSS customizado consome tempo e cria inconsistencias visuais.

**Solucao**: TailwindCSS permite desenvolvimento rapido com classes utilitarias. O design system fica consistente e responsivo sem CSS adicional.

```tsx
// Estilizacao direta no componente
<button className="btn-primary bg-black text-white px-4 py-2 rounded">
  Gerar PIX
</button>
```

### Server-Sent Events (SSE) para Monitoramento

**Problema**: O usuario precisa saber quando o pagamento foi confirmado sem recarregar a pagina.

**Alternativas consideradas**:
1. **Polling**: Requisicoes periodicas ao servidor. Ineficiente e consome recursos.
2. **WebSocket**: Bidirecional, mas complexo para este caso de uso unidirecional.
3. **SSE**: Conexao unidirecional do servidor para cliente. Ideal para atualizacoes de status.

**Implementacao**: Hook customizado `usePaymentStream` gerencia conexao SSE com reconexao automatica.

```typescript
// Hook encapsula toda a logica de conexao SSE
const { status, isApproved, reconnect } = usePaymentStream(paymentId);
```

**Caracteristicas**:
- Reconexao automatica com backoff exponencial
- Cleanup automatico ao desmontar componente
- Estados tipados para UI reativa

### QR Code Client-side vs Server-side

**Problema**: Mercado Pago retorna QR Code como base64 ou como payload EMV (texto).

**Solucao**: Detectamos o formato recebido e renderizamos apropriadamente:
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
│   ├── CopyButton.tsx    # Botao de copiar para clipboard
│   ├── LoadingSpinner.tsx
│   ├── Navigation.tsx    # Header de navegacao
│   └── StatusBadge.tsx   # Badge de status do pagamento
│
├── hooks/
│   └── usePaymentStream.ts  # Hook SSE para monitoramento
│
├── pages/
│   ├── PaymentPage.tsx   # Fluxo de pagamento PIX
│   └── ExitPage.tsx      # Terminal de saida
│
├── types/
│   └── index.ts          # Tipos TypeScript compartilhados
│
├── utils/
│   └── date.ts           # Formatacao de datas e valores
│
├── App.tsx               # Componente raiz com rotas
├── main.tsx              # Entry point
└── index.css             # Estilos globais + Tailwind
```

### Organizacao por Feature

Cada pagina e autocontida com sua logica de estado e chamadas de API. Componentes compartilhados ficam em `/components`.

---

## Desenvolvimento Local

### Pre-requisitos

- Node.js 20+
- npm ou pnpm

### Instalacao

```bash
npm install
npm run dev
```

A aplicacao estara disponivel em `http://localhost:3000`.

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

### Scripts Disponiveis

| Script | Descricao |
|--------|-----------|
| `npm run dev` | Servidor de desenvolvimento |
| `npm run build` | Build de producao |
| `npm run preview` | Preview do build |
| `npm run lint` | Verificar codigo com ESLint |

---

## Docker

### Build

```bash
docker build -t libera-front .
```

### Execucao

```bash
docker run -p 3000:80 libera-front
```

### Configuracao Nginx para SSE

O `nginx.conf` inclui configuracoes especificas para Server-Sent Events:

```nginx
location /api/payments/stream {
    proxy_pass http://api:8080;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400s;
}
```

---

## Integracao com Backend

### Endpoints Consumidos

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/payments` | POST | Criar pagamento PIX |
| `/payments/stream/{id}` | GET (SSE) | Monitorar status |
| `/access/exit` | PUT | Registrar saida |

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

Erros de API sao capturados e exibidos ao usuario com mensagens contextuais:

```typescript
} catch (err) {
  if (errorObj.status === 400) {
    message = 'Codigo invalido ou acesso nao encontrado';
  } else if (errorObj.message.includes('Payment')) {
    message = 'Pagamento nao confirmado. Efetue o pagamento primeiro.';
  }
}
```

---

## Licenca

GNU General Public License v2.0
