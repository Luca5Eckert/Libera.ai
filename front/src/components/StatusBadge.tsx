import { clsx } from 'clsx';
import type { PaymentStreamStatus } from '../types';

interface StatusBadgeProps {
  status: PaymentStreamStatus;
}

const statusConfig: Record<PaymentStreamStatus, { label: string; className: string }> = {
  idle: {
    label: 'Aguardando...',
    className: 'bg-gray-50 text-gray-700 border-gray-200',
  },
  connecting: {
    label: 'Conectando...',
    className: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  },
  waiting: {
    label: 'Aguardando pagamento',
    className: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  },
  approved: {
    label: 'Pagamento aprovado',
    className: 'bg-green-50 text-green-700 border-green-200',
  },
  error: {
    label: 'Erro na conexão',
    className: 'bg-red-50 text-red-700 border-red-200',
  },
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium border',
        config.className
      )}
    >
      {(status === 'connecting' || status === 'waiting') && (
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-yellow-400 opacity-75" />
          <span className="relative inline-flex rounded-full h-2 w-2 bg-yellow-500" />
        </span>
      )}
      {status === 'approved' && (
        <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
          <path
            fillRule="evenodd"
            d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
            clipRule="evenodd"
          />
        </svg>
      )}
      {config.label}
    </span>
  );
}
