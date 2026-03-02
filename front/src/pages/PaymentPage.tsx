import { useState } from 'react';
import QRCode from 'react-qr-code';
import { apiClient } from '../api/client';
import { usePaymentStream } from '../hooks/usePaymentStream';
import type { PaymentResponse } from '../types';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { StatusBadge } from '../components/StatusBadge';
import { CopyButton } from '../components/CopyButton';

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

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  // Initial form view
  if (!payment) {
    return (
      <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
        <div className="card w-full max-w-md">
          <header className="text-center mb-8 pb-6 border-b border-gray-100">
            <h1 className="text-2xl font-bold tracking-tight text-black">Pagamento PIX</h1>
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
                  Gerando PIX...
                </>
              ) : (
                'Gerar PIX'
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Payment view with QR code
  return (
    <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
      <div className="card w-full max-w-md">
        <header className="text-center mb-6">
          <h1 className="text-2xl font-bold tracking-tight text-black">
            {isApproved ? 'Pagamento Aprovado!' : 'Pague com PIX'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            {isApproved ? 'Você já pode liberar a saída' : 'Escaneie o QR Code ou copie o código'}
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

        {/* QR Code */}
        {!isApproved && (
          <div className="flex flex-col items-center gap-4 mb-6">
            <div className="p-4 bg-white border-2 border-gray-200 rounded-xl">
              {payment.qrCode.startsWith('data:') || payment.qrCode.length > 100 ? (
                // If qrCode is base64 image data
                <img
                  src={
                    payment.qrCode.startsWith('data:')
                      ? payment.qrCode
                      : `data:image/png;base64,${payment.qrCode}`
                  }
                  alt="QR Code PIX"
                  className="w-48 h-48"
                />
              ) : (
                // If qrCode is text payload, generate QR
                <QRCode value={payment.qrCode} size={192} level="M" />
              )}
            </div>

            {/* Copy PIX code */}
            <CopyButton text={payment.qrCode} />
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
