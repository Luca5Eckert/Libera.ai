import { useState } from 'react';
import { apiClient } from '../api/client';
import type { AccessExitResponse } from '../types';
import { LoadingSpinner } from '../components/LoadingSpinner';

export function ExitPage() {
  const [accessCode, setAccessCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exitData, setExitData] = useState<AccessExitResponse | null>(null);

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
      const response = await apiClient.exitAccess({ code });
      setExitData(response);
    } catch (err) {
      let message = 'Erro ao liberar saída';
      if (err && typeof err === 'object' && 'message' in err) {
        const errorObj = err as { message: string; status?: number };
        if (errorObj.status === 400) {
          message = 'Código inválido ou acesso não encontrado';
        } else if (errorObj.message.includes('Payment')) {
          message = 'Pagamento não confirmado para este código. Efetue o pagamento primeiro.';
        } else {
          message = errorObj.message;
        }
      }
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setExitData(null);
    setAccessCode('');
    setError(null);
  };

  // Format date/time
  const formatDate = (isoString: string | null) => {
    if (!isoString) return '--';
    const date = new Date(isoString + 'Z');
    return date.toLocaleString('pt-BR', {
      timeZone: 'America/Sao_Paulo',
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Calculate time spent
  const calculateTimeSpent = (entryIso: string | null, exitIso: string | null) => {
    if (!entryIso || !exitIso) return '--';
    const entry = new Date(entryIso + 'Z');
    const exit = new Date(exitIso + 'Z');
    const diffMs = exit.getTime() - entry.getTime();

    if (diffMs < 0) return 'Erro no cálculo';

    const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffHrs === 0) {
      return `${diffMins} min`;
    }
    return `${diffHrs}h e ${diffMins} min`;
  };

  // Initial form view
  if (!exitData) {
    return (
      <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
        <div className="card w-full max-w-md">
          <header className="text-center mb-8 pb-6 border-b border-gray-100">
            <h1 className="text-2xl font-bold tracking-tight text-black">Terminal de Saída</h1>
            <p className="text-sm text-gray-500 mt-1">Validação de ticket de estacionamento</p>
          </header>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label
                htmlFor="code-input"
                className="block text-xs font-semibold uppercase tracking-wider text-gray-600 mb-2"
              >
                Código do Ticket
              </label>
              <input
                type="number"
                id="code-input"
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
                  Processando...
                </>
              ) : (
                'Liberar Cancela'
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Success view
  return (
    <div className="min-h-[calc(100vh-4rem)] bg-gray-50 flex items-center justify-center p-4">
      <div className="card w-full max-w-md">
        {/* Success icon */}
        <div className="flex justify-center mb-4">
          <div className="w-16 h-16 bg-black text-white rounded-full flex items-center justify-center">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>

        <h2 className="text-2xl font-bold text-black text-center mb-1">Cancela Aberta</h2>
        <p className="text-sm text-gray-500 text-center mb-8">Pode prosseguir com o veículo.</p>

        {/* Details */}
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-4 text-sm mb-8">
          <div className="flex justify-between border-b border-gray-200 pb-2">
            <span className="text-gray-500">Ticket ID</span>
            <span className="font-bold text-black">#{String(exitData.id).padStart(4, '0')}</span>
          </div>
          <div className="flex justify-between border-b border-gray-200 pb-2">
            <span className="text-gray-500">Entrada</span>
            <span className="font-medium text-black">{formatDate(exitData.entryDate)}</span>
          </div>
          <div className="flex justify-between border-b border-gray-200 pb-2">
            <span className="text-gray-500">Saída</span>
            <span className="font-medium text-black">{formatDate(exitData.exitDate)}</span>
          </div>
          <div className="flex justify-between pt-1">
            <span className="font-bold text-black">Tempo Total</span>
            <span className="font-bold text-black">
              {calculateTimeSpent(exitData.entryDate, exitData.exitDate)}
            </span>
          </div>
        </div>

        <button onClick={handleReset} className="btn-secondary">
          Validar Novo Ticket
        </button>
      </div>
    </div>
  );
}
