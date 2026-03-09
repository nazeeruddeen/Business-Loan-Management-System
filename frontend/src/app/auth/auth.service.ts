import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, finalize, map, take, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from './auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly hostUrl = environment.hostname?.trim?.() ? environment.hostname.trim() : 'http://localhost:8080';
  private readonly accessTokenKey = 'auth_access_token';
  private readonly refreshTokenKey = 'auth_refresh_token';
  private readonly usernameKey = 'auth_username';
  private readonly roleKey = 'auth_role';

  private refreshing = false;
  private refreshedToken$ = new BehaviorSubject<string | null>(null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.hostUrl}/auth/login`, { username, password }).pipe(
      tap((response) => this.storeTokens(response))
    );
  }

  refreshAccessToken(): Observable<string> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearAuthData();
      return throwError(() => new Error('No refresh token available'));
    }

    if (this.refreshing) {
      return this.refreshedToken$.pipe(
        filter((token): token is string => token !== null),
        take(1)
      );
    }

    this.refreshing = true;
    this.refreshedToken$.next(null);

    return this.http.post<AuthResponse>(`${this.hostUrl}/auth/refresh`, { refreshToken }).pipe(
      tap((response) => {
        this.storeTokens(response);
        this.refreshedToken$.next(response.accessToken);
      }),
      map((response) => response.accessToken),
      catchError((error) => {
        this.clearAuthData();
        return throwError(() => error);
      }),
      finalize(() => {
        this.refreshing = false;
      })
    );
  }

  logout(redirect = true): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.hostUrl}/auth/logout`, { refreshToken }, {
        headers: new HttpHeaders({ 'Content-Type': 'application/json' })
      }).subscribe({
        next: () => {},
        error: () => {}
      });
    }

    this.clearAuthData();
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  handleUnauthorized(): void {
    this.clearAuthData();
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  getUsername(): string | null {
    return localStorage.getItem(this.usernameKey);
  }

  getRole(): string | null {
    return localStorage.getItem(this.roleKey);
  }

  private storeTokens(response: AuthResponse): void {
    localStorage.setItem(this.accessTokenKey, response.accessToken);
    localStorage.setItem(this.refreshTokenKey, response.refreshToken);
    localStorage.setItem(this.usernameKey, response.username);
    localStorage.setItem(this.roleKey, response.role);
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.usernameKey);
    localStorage.removeItem(this.roleKey);
  }
}


