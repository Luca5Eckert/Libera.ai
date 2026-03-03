import { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { usePaymentStream } from '../hooks/usePaymentStream';
import type { PaymentResponse } from '../types';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { StatusBadge } from '../components/StatusBadge';
import { formatCurrency } from '../utils/date';

export function PaymentPage() {
  const [accessCode, setAccessCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [waitingPayment, setWaitingPayment] = useState(false);

  const { status, isApproved, reconnect } = usePaymentStream(payment?.paymentId ?? null);

  // Effect to automatically redirect to Mercado Pago when payment is created
  useEffect(() => {
    if (payment?.linkPayment && !waitingPayment) {
      // Open Mercado Pago checkout in a new tab
      window.open(payment.linkPayment, '_blank');
      setWaitingPayment(true);
    }
  }, [payment, waitingPayment]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const code = parseInt(accessCode, 10);
    if (isNaN(code) || code <= 0) {
      setError('Por favor, insira um código válido');
      return;
    }

    setIsLoading(true);

    try {
      const response = await apiClient.createPayment({ accessCode: code });
      setPayment(response);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao gerar pagamento';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenCheckout = () => {
    if (payment?.linkPayment) {
      window.open(payment.linkPayment, '_blank');
    }
  };

  const handleReset = () => {
    setPayment(null);
    setAccessCode('');
    setError(null);
    setWaitingPayment(false);
  };

  // Initial form view
  if (!payment) {
    return (
      <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
        <div className="card w-full max-w-md">
          <header className="text-center mb-8 pb-6 border-b border-gray-100">
            <h1 className="text-2xl font-bold tracking-tight text-black">Pagamento</h1>
            <p className="text-sm text-gray-500 mt-1">Insira o código do seu ticket</p>
          </header>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label
                htmlFor="access-code"
                className="block text-xs font-semibold uppercase tracking-wider text-gray-600 mb-2"
              >
                Código do Ticket
              </label>
              <input
                type="number"
                id="access-code"
                value={accessCode}
                onChange={(e) => setAccessCode(e.target.value)}
                required
                placeholder="Ex: 001234"
                className="input-primary"
                autoComplete="off"
                autoFocus
                disabled={isLoading}
              />
            </div>

            {error && (
              <p className="text-red-600 text-sm font-medium text-center bg-red-50 p-3 rounded-lg">
                {error}
              </p>
            )}

            <button type="submit" disabled={isLoading} className="btn-primary">
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  Gerando pagamento...
                </>
              ) : (
                'Pagar'
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Payment view - waiting for Checkout Pro completion
  return (
    <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
      <div className="card w-full max-w-md">
        <header className="text-center mb-6">
          <h1 className="text-2xl font-bold tracking-tight text-black">
            {isApproved ? 'Pagamento Aprovado!' : 'Aguardando Pagamento'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            {isApproved 
              ? 'Você já pode liberar a saída' 
              : 'Complete o pagamento na aba do Mercado Pago'}
          </p>
        </header>

        {/* Status badge */}
        <div className="flex justify-center mb-6">
          <StatusBadge status={status} />
        </div>

        {/* Amount */}
        <div className="text-center mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">
            Valor a pagar
          </p>
          <p className="text-3xl font-bold text-black">{formatCurrency(payment.amount)}</p>
        </div>

        {/* Waiting animation */}
        {!isApproved && (
          <div className="flex flex-col items-center gap-4 mb-6">
            <div className="p-4 bg-white border-2 border-gray-200 rounded-xl">
              <div className="w-48 h-48 flex items-center justify-center">
                <div className="text-center">
                  <LoadingSpinner size="lg" />
                  <p className="mt-4 text-sm text-gray-500">Aguardando confirmação...</p>
                </div>
              </div>
            </div>

            {/* Button to re-open checkout */}
            <button onClick={handleOpenCheckout} className="btn-secondary text-sm">
              Abrir Checkout Novamente
            </button>
          </div>
        )}

        {/* Success icon */}
        {isApproved && (
          <div className="flex justify-center mb-6">
            <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center">
              <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
          </div>
        )}

        {/* Error retry */}
        {status === 'error' && (
          <button onClick={reconnect} className="btn-secondary mb-4">
            Tentar reconectar
          </button>
        )}

        {/* Actions */}
        <div className="space-y-3">
          {isApproved && (
            <a href="/exit" className="btn-primary no-underline">
              Ir para Liberação de Saída
            </a>
          )}
          <button onClick={handleReset} className="btn-secondary">
            {isApproved ? 'Novo Pagamento' : 'Cancelar'}
          </button>
        </div>
      </div>
    </div>
  );
}
