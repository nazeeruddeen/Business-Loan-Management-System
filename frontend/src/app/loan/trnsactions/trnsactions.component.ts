import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { environment } from '../../../environments/environment';
import { AuthorizationService } from '../../auth/authorization.service';

@Component({
  selector: 'app-trnsactions',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './trnsactions.component.html',
  styleUrl: './trnsactions.component.css'
})
export class TrnsactionsComponent {
  data: any[] = [];
  file!: File;
  id: string | null = '';
  isEditing = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  canEdit = false;

  transactionsForm: FormGroup;
  private readonly apiBase = environment.hostname?.trim?.() ? environment.hostname.trim() : 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
    private authorizationService: AuthorizationService
  ) {
    this.transactionsForm = this.fb.group({
      transactions: this.fb.array([])
    });
  }

  get transactionsFormArray(): FormArray {
    return this.transactionsForm.get('transactions') as FormArray;
  }

  getTransactionGroup(index: number): FormGroup {
    const group = this.transactionsFormArray.at(index) as FormGroup;
    return group || new FormGroup({});
  }

  onFileChange(event: Event) {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to upload transactions.';
      return;
    }

    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.file = target.files[0];
      this.clearMessages();
    }
  }

  submit() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to upload transactions.';
      return;
    }

    if (!this.file) {
      this.errorMessage = 'Please select a file first';
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    const formData = new FormData();
    formData.append('file', this.file, this.file.name);

    this.http.post<any[]>(this.apiBase + '/loans/sales/readTransactionsCsv', formData).subscribe(
      response => {
        this.data = response;
        this.populateForm(response);
        this.isLoading = false;
        this.successMessage = 'File processed successfully!';
        this.cdr.detectChanges();
      },
      error => {
        this.isLoading = false;
        this.errorMessage = this.extractErrorMessage(error, 'Error occurred while processing file');
        this.cdr.detectChanges();
      }
    );
  }

  populateForm(transactions: any[]) {
    this.transactionsFormArray.clear();
    transactions.forEach(transaction => {
      this.transactionsFormArray.push(this.createTransactionGroup(transaction));
    });
  }

  createTransactionGroup(transaction: any = {}): FormGroup {
    return this.fb.group({
      transactionDate: [this.toDateTimeLocalInput(transaction.transactionDate), Validators.required],
      activity: [transaction.activity || '', Validators.required],
      instrument: [transaction.instrument || '', Validators.required],
      txnId: [transaction.txnId || '', Validators.required],
      comment: [transaction.comment || ''],
      debtAmt: [transaction.debtAmt || 0, [Validators.required, Validators.min(0)]],
      creditAmt: [transaction.creditAmt || 0, [Validators.required, Validators.min(0)]],
      transactionBreakup: [transaction.transactionBreakup || ''],
      transactionStatus: [transaction.transactionStatus || '', Validators.required]
    });
  }

  saveData() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to save transactions.';
      return;
    }

    if (this.transactionsFormArray.invalid) {
      this.errorMessage = 'Please fix validation errors before saving';
      this.markFormGroupTouched(this.transactionsForm);
      return;
    }

    this.id = new URLSearchParams(window.location.search).get('id');
    if (!this.id) {
      this.errorMessage = 'Application ID not found in URL';
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    const formData = this.transactionsFormArray.value.map((transaction: any) => ({
      ...transaction,
      transactionDate: this.toIsoDateTime(transaction.transactionDate)
    }));

    this.http.post<any[]>(this.apiBase + '/loans/sales/saveTxnsData/' + this.id, formData).subscribe(
      response => {
        this.isLoading = false;
        this.successMessage = 'Data saved successfully!';
        this.data = response;
        this.isEditing = false;
        this.cdr.detectChanges();
      },
      error => {
        this.isLoading = false;
        this.errorMessage = this.extractErrorMessage(error, 'Error occurred while saving data');
        this.cdr.detectChanges();
      }
    );
  }

  fetchData(appId: number) {
    this.isLoading = true;
    this.clearMessages();

    this.http.get<any[]>(this.apiBase + '/loans/getTxnsData/' + appId).subscribe(
      response => {
        this.data = response;
        this.populateForm(response);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error => {
        this.isLoading = false;
        this.errorMessage = 'Error occurred while fetching data';
        this.cdr.detectChanges();
      }
    );
  }

  editForm() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to edit transactions.';
      return;
    }

    this.isEditing = !this.isEditing;
    if (this.isEditing && this.data.length > 0) {
      this.populateForm(this.data);
    }
  }

  addNewTransaction() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to add transactions.';
      return;
    }

    this.transactionsFormArray.push(this.createTransactionGroup());
    this.isEditing = true;
  }

  removeTransaction(index: number) {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to remove transactions.';
      return;
    }

    this.transactionsFormArray.removeAt(index);
  }

  cancelEdit() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to edit transactions.';
      return;
    }

    this.isEditing = false;
    if (this.data.length > 0) {
      this.populateForm(this.data);
    }
  }

  private toDateTimeLocalInput(value: any): string {
    if (!value) {
      return '';
    }

    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(trimmed)) {
        return trimmed.slice(0, 16);
      }
      if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
        return `${trimmed}T00:00`;
      }
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private toIsoDateTime(value: string): string {
    if (!value || !value.trim()) {
      return value;
    }

    const trimmed = value.trim();

    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      return `${trimmed}T00:00:00`;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
      return `${trimmed}:00`;
    }

    return trimmed;
  }

  clearMessages() {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private extractErrorMessage(error: any, fallback: string): string {
    if (typeof error?.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    if (typeof error?.message === 'string' && error.message.trim().length > 0) {
      return error.message;
    }
    if (typeof error?.error?.message === 'string' && error.error.message.trim().length > 0) {
      return error.error.message;
    }
    return fallback;
  }

  markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      } else if (control instanceof FormArray) {
        control.controls.forEach(arrayControl => {
          if (arrayControl instanceof FormGroup) {
            this.markFormGroupTouched(arrayControl);
          }
        });
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'success':
      case 'completed':
        return 'bg-success';
      case 'pending':
      case 'processing':
        return 'bg-warning';
      case 'failed':
      case 'error':
        return 'bg-danger';
      case 'cancelled':
        return 'bg-secondary';
      default:
        return 'bg-info';
    }
  }

  ngOnInit(): void {
    this.canEdit = this.authorizationService.canEditTransactions();
    this.id = new URLSearchParams(window.location.search).get('id');
    if (this.id) {
      this.fetchData(Number(this.id));
    }
  }
}
