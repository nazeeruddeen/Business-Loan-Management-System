import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { AuthService } from '../auth.service';
import {
  AuditLogFilters,
  AuditLogResponse,
  CreateUserRequest,
  UserManagementService,
  UserSummaryResponse
} from '../user-management.service';

@Component({
  selector: 'app-user-admin',
  standalone: true,
  imports: [CommonModule, SharedModule],
  templateUrl: './user-admin.component.html',
  styleUrl: './user-admin.component.css'
})
export class UserAdminComponent {
  users: UserSummaryResponse[] = [];
  auditLogs: AuditLogResponse[] = [];
  loading = false;
  loadingAudit = false;
  saving = false;
  exportingAudit = false;
  errorMessage = '';
  successMessage = '';

  readonly roleOptions = ['ADMIN', 'LOAN_OFFICER', 'ANALYST', 'VIEWER'];
  readonly auditActionOptions = [
    'ALL',
    'CREATE_USER',
    'UPDATE_USER_ROLE',
    'UPDATE_USER_STATUS',
    'RESET_USER_PASSWORD'
  ];

  selectedRoles: Record<number, string> = {};
  currentUsername = '';

  createForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['VIEWER', [Validators.required]]
  });

  resetForm = this.fb.group({
    userId: [null as number | null, [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  auditFiltersForm = this.fb.group({
    limit: [50],
    action: ['ALL'],
    actor: [''],
    from: [''],
    to: ['']
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userManagementService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.currentUsername = this.authService.getUsername() ?? '';
    this.loadUsers();
    this.loadAuditLogs();
  }

  loadUsers(): void {
    this.loading = true;
    this.clearMessages();

    this.userManagementService.listUsers().subscribe({
      next: (users) => {
        this.users = [...users].sort((a, b) => a.username.localeCompare(b.username));
        this.selectedRoles = this.users.reduce((acc, user) => {
          acc[user.id] = user.role;
          return acc;
        }, {} as Record<number, string>);
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Failed to load users');
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  loadAuditLogs(): void {
    this.loadingAudit = true;
    const filters = this.buildAuditFilters();

    this.userManagementService.getAuditLogs(filters).subscribe({
      next: (logs) => {
        this.auditLogs = logs;
      },
      error: (_err) => {
        this.auditLogs = [];
      },
      complete: () => {
        this.loadingAudit = false;
      }
    });
  }

  applyAuditFilters(): void {
    this.loadAuditLogs();
  }

  clearAuditFilters(): void {
    this.auditFiltersForm.reset({
      limit: 50,
      action: 'ALL',
      actor: '',
      from: '',
      to: ''
    });
    this.loadAuditLogs();
  }

  exportAuditCsv(): void {
    this.exportingAudit = true;
    const filters = this.buildAuditFilters();

    this.userManagementService.exportAuditLogsCsv(filters).subscribe({
      next: (blob) => {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `security-audit-${timestamp}.csv`;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Failed to export audit logs');
      },
      complete: () => {
        this.exportingAudit = false;
      }
    });
  }

  createUser(): void {
    if (this.createForm.invalid || this.saving) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.clearMessages();

    const payload: CreateUserRequest = {
      username: (this.createForm.value.username ?? '').trim(),
      password: this.createForm.value.password ?? '',
      role: this.createForm.value.role ?? 'VIEWER'
    };

    this.userManagementService.createUser(payload).subscribe({
      next: (created) => {
        this.successMessage = `User "${created.username}" created successfully.`;
        this.createForm.reset({ username: '', password: '', role: 'VIEWER' });
        this.loadUsers();
        this.loadAuditLogs();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Failed to create user');
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  updateRole(user: UserSummaryResponse): void {
    const selectedRole = this.selectedRoles[user.id];
    if (!selectedRole || selectedRole === user.role || this.saving) {
      return;
    }

    this.saving = true;
    this.clearMessages();

    this.userManagementService.updateRole(user.id, selectedRole).subscribe({
      next: (updated) => {
        user.role = updated.role;
        this.successMessage = `Role updated for "${user.username}".`;
        this.loadAuditLogs();
      },
      error: (err) => {
        this.selectedRoles[user.id] = user.role;
        this.errorMessage = this.extractErrorMessage(err, 'Failed to update role');
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  toggleStatus(user: UserSummaryResponse): void {
    if (this.isCurrentUser(user) || this.saving) {
      return;
    }

    const nextStatus = !user.active;
    this.saving = true;
    this.clearMessages();

    this.userManagementService.updateStatus(user.id, nextStatus).subscribe({
      next: (updated) => {
        user.active = updated.active;
        this.successMessage = `Status updated for "${user.username}".`;
        this.loadAuditLogs();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Failed to update status');
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  prepareReset(user: UserSummaryResponse): void {
    this.resetForm.patchValue({
      userId: user.id,
      newPassword: ''
    });
  }

  resetPassword(): void {
    if (this.resetForm.invalid || this.saving) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const userId = this.resetForm.value.userId as number;
    const password = this.resetForm.value.newPassword ?? '';
    const targetUser = this.users.find((user) => user.id === userId);

    this.saving = true;
    this.clearMessages();

    this.userManagementService.resetPassword(userId, password).subscribe({
      next: () => {
        this.successMessage = `Password reset successful for "${targetUser?.username ?? userId}".`;
        this.resetForm.patchValue({ newPassword: '' });
        this.loadAuditLogs();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Failed to reset password');
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  isCurrentUser(user: UserSummaryResponse): boolean {
    return user.username.toLowerCase() === this.currentUsername.toLowerCase();
  }

  trackByUserId(_index: number, user: UserSummaryResponse): number {
    return user.id;
  }

  trackByAuditId(_index: number, log: AuditLogResponse): number {
    return log.id;
  }

  private buildAuditFilters(): AuditLogFilters {
    const raw = this.auditFiltersForm.value;
    const limit = Number(raw.limit ?? 50);
    const action = (raw.action ?? 'ALL').toString().trim().toUpperCase();
    const actor = (raw.actor ?? '').toString().trim();

    return {
      limit: Number.isFinite(limit) ? Math.max(1, Math.min(limit, 500)) : 50,
      action: action === 'ALL' ? undefined : action,
      actor: actor || undefined,
      from: this.normalizeDateTimeForApi((raw.from ?? '').toString()),
      to: this.normalizeDateTimeForApi((raw.to ?? '').toString())
    };
  }

  private normalizeDateTimeForApi(value: string): string | undefined {
    const trimmed = value.trim();
    if (!trimmed) {
      return undefined;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
      return `${trimmed}:00`;
    }

    return trimmed;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private extractErrorMessage(error: any, fallback: string): string {
    if (typeof error?.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    if (typeof error?.error?.message === 'string' && error.error.message.trim().length > 0) {
      return error.error.message;
    }
    if (typeof error?.message === 'string' && error.message.trim().length > 0) {
      return error.message;
    }
    return fallback;
  }
}
