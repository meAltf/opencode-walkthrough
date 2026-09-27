export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  timestamp: string;
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue: unknown;
}

export interface OnboardingRequest {
  email: string;
  fullName: string;
  password: string;
  dateOfBirth: string;
  termsAccepted: boolean;
}

export type OnboardingStatus = 'PENDING' | 'COMPLETED';

export interface OnboardedUser {
  id: string;
  email: string;
  fullName: string;
  dateOfBirth: string;
  status: OnboardingStatus;
  createdAt: string;
}

export class OnboardingApiError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly fieldErrors: FieldError[],
    readonly status: number,
  ) {
    super(message);
    this.name = 'OnboardingApiError';
  }
}
