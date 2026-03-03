import { useState } from 'react';
import { apiClient } from '../api/client';
import { usePaymentStream } from '../hooks/usePaymentStream';
import type { PaymentResponse } from '../types';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { StatusBadge } from '../components/StatusBadge';
import { CopyButton } from '../components/CopyButton';
import { formatCurrency } from '../utils/date';

export function PaymentPage() {
  const [accessCode, setAccessCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);

  const { status, isApproved, reconnect } = usePaymentStream(payment?.paymentId ?? null);

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

  const handleReset = () => {
    setPayment(null);
    setAccessCode('');
    setError(null);
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

  // Payment view - PIX QR Code
  return (
    <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
      <div className="card w-full max-w-md">
        <header className="text-center mb-6">
          <h1 className="text-2xl font-bold tracking-tight text-black">
            {isApproved ? 'Pagamento Aprovado!' : 'Pague com PIX'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            {isApproved 
              ? 'Você já pode liberar a saída' 
              : 'Escaneie o QR Code ou copie o código PIX'}
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

        {/* PIX QR Code */}
        {!isApproved && (
          <div className="flex flex-col items-center gap-4 mb-6">
            {payment.qrCodeBase64 && (
              <div className="p-4 bg-white border-2 border-gray-200 rounded-xl">
                <img
                  src={`data:image/png;base64,${payment.qrCodeBase64}`}
                  alt="QR Code PIX"
                  className="w-48 h-48"
                />
              </div>
            )}

            {/* Copia e Cola */}
            {payment.qrCode && (
              <div className="w-full space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 text-center">
                  PIX Copia e Cola
                </p>
                <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
                  <p className="text-xs text-gray-600 break-all text-center mb-2 line-clamp-2">
                    {payment.qrCode}
                  </p>
                  <div className="flex justify-center">
                    <CopyButton text={payment.qrCode} />
                  </div>
                </div>
              </div>
            )}

            {/* Loading indicator */}
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <LoadingSpinner size="sm" />
              <span>Aguardando confirmação do pagamento...</span>
            </div>
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
