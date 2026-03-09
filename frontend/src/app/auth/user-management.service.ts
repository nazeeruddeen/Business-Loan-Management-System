import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserSummaryResponse {
  id: number;
  username: string;
  role: 'ADMIN' | 'LOAN_OFFICER' | 'ANALYST' | 'VIEWER' | string;
  active: boolean;
}

export interface AuditLogResponse {
  id: number;
  action: string;
  actorUsername: string;
  targetUserId: number | null;
  targetUsername: string | null;
  details: string;
  createdAt: string;
}

export interface AuditLogFilters {
  limit?: number;
  action?: string;
  actor?: string;
  from?: string;
  to?: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  role: 'ADMIN' | 'LOAN_OFFICER' | 'ANALYST' | 'VIEWER' | string;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private readonly hostUrl = environment.hostname?.trim?.() ? environment.hostname.trim() : 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  listUsers(): Observable<UserSummaryResponse[]> {
    return this.http.get<UserSummaryResponse[]>(`${this.hostUrl}/auth/users`);
  }

  createUser(payload: CreateUserRequest): Observable<UserSummaryResponse> {
    return this.http.post<UserSummaryResponse>(`${this.hostUrl}/auth/users`, payload);
  }

  updateRole(id: number, role: string): Observable<UserSummaryResponse> {
    return this.http.patch<UserSummaryResponse>(`${this.hostUrl}/auth/users/${id}/role`, { role });
  }

  updateStatus(id: number, active: boolean): Observable<UserSummaryResponse> {
    return this.http.patch<UserSummaryResponse>(`${this.hostUrl}/auth/users/${id}/status`, { active });
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.hostUrl}/auth/users/${id}/password`, { newPassword });
  }

  getAuditLogs(filters: AuditLogFilters = {}): Observable<AuditLogResponse[]> {
    return this.http.get<AuditLogResponse[]>(`${this.hostUrl}/auth/audit`, {
      params: this.toHttpParams(filters)
    });
  }

  exportAuditLogsCsv(filters: AuditLogFilters = {}): Observable<Blob> {
    return this.http.get(`${this.hostUrl}/auth/audit/export`, {
      params: this.toHttpParams(filters),
      responseType: 'blob'
    });
  }

  private toHttpParams(filters: AuditLogFilters): HttpParams {
    let params = new HttpParams();

    if (filters.limit != null) {
      params = params.set('limit', String(filters.limit));
    }
    if (filters.action) {
      params = params.set('action', filters.action);
    }
    if (filters.actor) {
      params = params.set('actor', filters.actor);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }

    return params;
  }
}
