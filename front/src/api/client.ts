import type {
  CreatePaymentRequest,
  PaymentResponse,
  AccessExitRequest,
  AccessExitResponse,
  ApiError,
} from '../types';

// API base URL - in dev mode uses Vite proxy, in production uses env variable
const getApiUrl = (): string => {
  // In production (docker), use the environment variable
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  // In development, use Vite proxy
  return '/api';
};

const API_URL = getApiUrl();

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...options.headers,
    };

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error: ApiError = {
        message: `Request failed: ${response.statusText}`,
        status: response.status,
      };

      try {
        const body = await response.json();
        error.message = body.message || body.error || error.message;
      } catch {
        // Ignore JSON parse error
      }

      throw error;
    }

    return response.json();
  }

  // Payment endpoints
  async createPayment(request: CreatePaymentRequest): Promise<PaymentResponse> {
    return this.request<PaymentResponse>('/payments', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  getPaymentStreamUrl(paymentId: string): string {
    return `${this.baseUrl}/payments/stream/${paymentId}`;
  }

  // Access endpoints
  async exitAccess(request: AccessExitRequest): Promise<AccessExitResponse> {
    return this.request<AccessExitResponse>('/access/exit', {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }
}

export const apiClient = new ApiClient(API_URL);
