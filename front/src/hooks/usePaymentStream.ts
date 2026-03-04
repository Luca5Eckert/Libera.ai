import { useState, useEffect, useRef, useMemo } from 'react';
import type { PaymentStreamStatus } from '../types';
import { apiClient } from '../api/client';

interface UsePaymentStreamReturn {
  status: PaymentStreamStatus;
  isApproved: boolean;
  error: string | null;
  reconnect: () => void;
}

/**
 * Reconnection backoff configuration.
 * Uses exponential backoff strategy: 500ms → 1s → 2s → 5s
 */
const SSE_RECONNECT_CONFIG = {
  /** Delays in milliseconds for each reconnection attempt */
  delays: [500, 1000, 2000, 5000],
  /** Maximum number of reconnection attempts before giving up */
  get maxAttempts() {
    return this.delays.length;
  },
} as const;

// Check SSE support once at module level
const isSSESupported = typeof EventSource !== 'undefined';

export function usePaymentStream(paymentId: string | null): UsePaymentStreamReturn {
  const [status, setStatus] = useState<PaymentStreamStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [triggerReconnect, setTriggerReconnect] = useState(0);
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectAttemptRef = useRef(0);
  const approvedRef = useRef(false);

  // Reset when paymentId changes
  useEffect(() => {
    if (!paymentId) {
      approvedRef.current = false;
      reconnectAttemptRef.current = 0;
    }
  }, [paymentId]);

  useEffect(() => {
    // Early return for no paymentId or no SSE support
    if (!paymentId || !isSSESupported) {
      return;
    }

    setStatus('connecting');
    setError(null);

    const url = apiClient.getPaymentStreamUrl(paymentId);
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      setStatus('waiting');
      reconnectAttemptRef.current = 0;
    };

    // Handle named 'payment-status' events from the backend SSE stream
    const handlePaymentStatus = (event: MessageEvent) => {
      const isPaid = event.data === 'true';
      if (isPaid) {
        approvedRef.current = true;
        setStatus('approved');
        eventSource.close();
      } else {
        setStatus('waiting');
      }
    };

    eventSource.addEventListener('payment-status', handlePaymentStatus);

    eventSource.onerror = () => {
      eventSource.close();
      eventSourceRef.current = null;

      // Don't reconnect if already approved
      if (approvedRef.current) return;

      const attemptIndex = Math.min(
        reconnectAttemptRef.current,
        SSE_RECONNECT_CONFIG.delays.length - 1
      );
      const delay = SSE_RECONNECT_CONFIG.delays[attemptIndex];

      reconnectAttemptRef.current += 1;

      if (reconnectAttemptRef.current <= SSE_RECONNECT_CONFIG.maxAttempts) {
        setStatus('connecting');
        setTimeout(() => {
          if (!approvedRef.current) {
            setTriggerReconnect((prev) => prev + 1);
          }
        }, delay);
      } else {
        setStatus('error');
        setError('Falha na conexão. Clique para tentar novamente.');
      }
    };

    return () => {
      eventSource.removeEventListener('payment-status', handlePaymentStatus);
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [paymentId, triggerReconnect]);

  const reconnect = () => {
    reconnectAttemptRef.current = 0;
    setTriggerReconnect((prev) => prev + 1);
  };

  // Compute effective status and error based on paymentId and SSE support
  const result = useMemo(() => {
    if (!paymentId) {
      return { status: 'idle' as PaymentStreamStatus, error: null };
    }
    if (!isSSESupported) {
      return { status: 'error' as PaymentStreamStatus, error: 'SSE não suportado neste navegador' };
    }
    return { status, error };
  }, [paymentId, status, error]);

  return {
    status: result.status,
    isApproved: result.status === 'approved',
    error: result.error,
    reconnect,
  };
}
