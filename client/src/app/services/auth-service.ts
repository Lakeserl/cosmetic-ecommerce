import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, map, catchError, tap, throwError } from 'rxjs';


export interface User {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  firstName: string;
  lastName: string;
  avatarUrl?: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  status: string;
  roles: string[];
  createdAt: string;
  lastLoginAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/auth';
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private tokenRefreshTimer?: any;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadStoredAuth();
    this.setupTokenRefresh();
  }

  // Registration flow
  register(data: {
    username: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/register`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  verifyRegistration(data: {
    username: string;
    otp: string;
  }): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API_URL}/register/verify-otp`, data)
      .pipe(
        map(response => response.data),
        tap(authResponse => this.handleAuthSuccess(authResponse)),
        catchError(this.handleError)
      );
  }

  // Login flow
  login(data: {
    username: string;
    password: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/login`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  verifyLogin(data: {
    username: string;
    otp: string;
  }): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API_URL}/login/verify-otp`, data)
      .pipe(
        map(response => response.data),
        tap(authResponse => this.handleAuthSuccess(authResponse)),
        catchError(this.handleError)
      );
  }

  // Token management
  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.logout();
      return throwError('No refresh token available');
    }

    return this.http.post<ApiResponse<any>>(`${this.API_URL}/token/refresh`, { refreshToken })
      .pipe(
        map(response => response.data),
        tap(tokenResponse => {
          this.setTokens(tokenResponse.accessToken, tokenResponse.refreshToken);
          this.setupTokenRefresh();
        }),
        catchError(error => {
          this.logout();
          return this.handleError(error);
        })
      );
  }

  // OTP operations
  sendOtp(data: {
    username: string;
    purpose: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/otp/send`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  // Password operations
  forgotPassword(data: { username: string }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/password/forgot`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  resetPassword(data: {
    username: string;
    otp: string;
    newPassword: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/password/reset`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  changePassword(data: {
    currentPassword: string;
    newPassword: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.API_URL}/password/change`, data)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  // Logout
  logout(): void {
    const refreshToken = this.getRefreshToken();
    const accessToken = this.getToken();
    
    if (refreshToken || accessToken) {
      this.http.post<ApiResponse<any>>(`${this.API_URL}/logout`, {
        refreshToken,
        accessToken
      }).subscribe();
    }

    this.clearTokens();
    this.currentUserSubject.next(null);
    this.clearTokenRefreshTimer();
    this.router.navigate(['/login']);
  }

  // Token utilities
  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken() && !!this.currentUserSubject.value;
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // Private methods
  private handleAuthSuccess(authResponse: AuthResponse): void {
    this.setTokens(authResponse.accessToken, authResponse.refreshToken);
    this.currentUserSubject.next(authResponse.user);
    this.setupTokenRefresh();
  }

  private setTokens(accessToken: string, refreshToken: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  private clearTokens(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  private loadStoredAuth(): void {
    const token = this.getToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const now = Date.now() / 1000;
        
        if (payload.exp > now) {
          // Token still valid, you might want to fetch current user info
          this.fetchCurrentUser().subscribe();
        } else {
          this.refreshToken().subscribe();
        }
      } catch (error) {
        this.clearTokens();
      }
    }
  }

  private setupTokenRefresh(): void {
    this.clearTokenRefreshTimer();
    
    const token = this.getToken();
    if (!token) return;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000;
      const now = Date.now();
      
      // Refresh 2 minutes before expiry
      const refreshTime = exp - now - (2 * 60 * 1000);
      
      if (refreshTime > 0) {
        this.tokenRefreshTimer = setTimeout(() => {
          this.refreshToken().subscribe();
        }, refreshTime);
      }
    } catch (error) {
      console.error('Error setting up token refresh:', error);
    }
  }

  private clearTokenRefreshTimer(): void {
    if (this.tokenRefreshTimer) {
      clearTimeout(this.tokenRefreshTimer);
      this.tokenRefreshTimer = null;
    }
  }

  private fetchCurrentUser(): Observable<User> {
    return this.http.get<ApiResponse<User>>('/api/users/me')
      .pipe(
        map(response => response.data),
        tap(user => this.currentUserSubject.next(user)),
        catchError(error => {
          this.logout();
          return throwError(error);
        })
      );
  }

  private handleError(error: any): Observable<never> {
    console.error('Auth service error:', error);
    let errorMessage = 'An error occurred';
    
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(errorMessage);
  }
}
