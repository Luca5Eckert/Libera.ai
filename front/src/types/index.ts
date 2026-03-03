// Payment types
export interface CreatePaymentRequest {
  accessCode: number;
}

export interface PaymentResponse {
  amount: number;
  linkPayment: string;
  paymentId: string;
}

// Access types
export interface AccessExitRequest {
  code: number;
}

export interface AccessExitResponse {
  id: number;
  entryDate: string;
  exitDate: string;
}

// SSE Stream status
export type PaymentStreamStatus =
  | 'idle'
  | 'connecting'
  | 'waiting'
  | 'approved'
  | 'error';

// API Error
export interface ApiError {
  message: string;
  status?: number;
}
