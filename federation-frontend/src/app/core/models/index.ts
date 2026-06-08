// ═══════════════════════════════════════════════════════════════════════════
// User & Auth Models
// ═══════════════════════════════════════════════════════════════════════════

export enum UserRole {
  ADMIN           = 'ROLE_ADMIN',
  FEDERATION_STAFF = 'ROLE_FEDERATION_STAFF',
  CLUB_MANAGER    = 'ROLE_CLUB_MANAGER',
  ATHLETE         = 'ROLE_ATHLETE',
  PUBLIC          = 'ROLE_PUBLIC',
}

export enum UserStatus {
  ACTIVE              = 'ACTIVE',
  INACTIVE            = 'INACTIVE',
  SUSPENDED           = 'SUSPENDED',
  PENDING_VERIFICATION = 'PENDING_VERIFICATION',
}

export interface User {
  id:         string;
  email:      string;
  username:   string;
  firstName:  string;
  lastName:   string;
  fullName?:  string;
  role:       UserRole;
  status:     UserStatus;
  phone?:     string;
  avatarUrl?: string;
  lastLogin?: string;
  createdAt:  string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password:        string;
}

export interface RegisterRequest {
  email:      string;
  username:   string;
  password:   string;
  firstName:  string;
  lastName:   string;
}

export interface AuthResponse {
  accessToken:  string;
  refreshToken: string;
  tokenType:    string;
  expiresIn:    number;     // seconds
  userId:       string;
  email:        string;
  username:     string;
  role:         UserRole;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword:     string;
  confirmPassword: string;
}

// ═══════════════════════════════════════════════════════════════════════════
// Generic API Response Wrapper
// ═══════════════════════════════════════════════════════════════════════════

export interface ApiResponse<T = any> {
  success:   boolean;
  data:      T;
  message?:  string;
  status?:   number;
  path?:     string;
  timestamp: string;
}

export interface ValidationError {
  field:         string;
  rejectedValue: any;
  message:       string;
}

export interface ValidationErrorResponse {
  status:  number;
  message: string;
  errors:  ValidationError[];
  path:    string;
}

export interface PagedResponse<T> {
  content:       T[];
  page:          number;
  size:          number;
  totalElements: number;
  totalPages:    number;
  last:          boolean;
  first:         boolean;
}

// ═══════════════════════════════════════════════════════════════════════════
// Utility Types
// ═══════════════════════════════════════════════════════════════════════════

export interface SelectOption<T = string> {
  value: T;
  label: string;
}

export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

export interface NavItem {
  label:    string;
  path:     string;
  icon:     string;
  roles?:   UserRole[];
  children?: NavItem[];
}
