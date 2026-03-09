import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { environment } from '../../../environments/environment';
import { AuthorizationService } from '../../auth/authorization.service';

@Component({
  selector: 'app-salesreport',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './salesreport.component.html',
  styleUrl: './salesreport.component.css'
})
export class SalesreportComponent {
  data: any[] = [];
  file!: File;
  id: string | null = '';
  isEditing = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  canEdit = false;

  salesForm: FormGroup;
  private readonly apiBase = environment.hostname?.trim?.() ? environment.hostname.trim() : 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
    private authorizationService: AuthorizationService
  ) {
    this.salesForm = this.fb.group({
      reports: this.fb.array([])
    });
  }

  get reportsFormArray(): FormArray {
    return this.salesForm.get('reports') as FormArray;
  }

  getReportGroup(index: number): FormGroup {
    const group = this.reportsFormArray.at(index) as FormGroup;
    return group || new FormGroup({});
  }

  onFileChange(event: Event) {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to upload sales reports.';
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
      this.errorMessage = 'You do not have permission to upload sales reports.';
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

    this.http.post<any[]>(this.apiBase + '/loans/readExcel', formData).subscribe(
      response => {
        const sanitized = this.normalizeUploadedReports(response ?? []);
        this.data = sanitized;
        this.populateForm(sanitized);
        this.isLoading = false;
        this.successMessage = sanitized.length > 0
          ? 'File processed successfully!'
          : 'File processed, but no valid sales rows were found.';
        this.cdr.detectChanges();
      },
      error => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Error occurred while processing file';
        this.cdr.detectChanges();
      }
    );
  }

  populateForm(reports: any[]) {
    this.reportsFormArray.clear();
    reports.forEach(report => {
      this.reportsFormArray.push(this.createReportGroup(report));
    });
  }

  createReportGroup(report: any = {}): FormGroup {
    return this.fb.group({
      date: [report.date || '', [Validators.required, Validators.pattern(/^\d{2}\/\d{2}\/\d{4}$/)]],
      orderno: [report.orderno || '', [Validators.required, Validators.maxLength(50)]],
      invoiceno: [report.invoiceno || '', [Validators.required, Validators.maxLength(50)]],
      partyName: [report.partyName || '', [Validators.required, Validators.maxLength(100)]],
      partyPhoneNum: [report.partyPhoneNum || '', [Validators.pattern(/^[0-9]{10}$/)]],
      totalAmount: [report.totalAmount || null, [Validators.required, Validators.min(0.01)]],
      recievedOrPaidAmount: [report.recievedOrPaidAmount || null, [Validators.min(0)]],
      balanceAmount: [report.balanceAmount || null, [Validators.min(0)]]
    });
  }

  saveData() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to save sales reports.';
      return;
    }

    if (this.reportsFormArray.invalid) {
      this.errorMessage = 'Please fix validation errors before saving';
      this.markFormGroupTouched(this.salesForm);
      return;
    }

    this.id = new URLSearchParams(window.location.search).get('id');
    if (!this.id) {
      this.errorMessage = 'Application ID not found in URL';
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    const formData = this.reportsFormArray.value;

    this.http.post<any[]>(this.apiBase + '/loans/saveSalesReport/' + this.id, formData).subscribe(
      response => {
        this.isLoading = false;
        this.successMessage = 'Data saved successfully!';
        this.data = response;
        this.isEditing = false;
        this.cdr.detectChanges();
      },
      error => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Error occurred while saving data';
        this.cdr.detectChanges();
      }
    );
  }

  fetchData(appId: number) {
    this.isLoading = true;
    this.clearMessages();

    this.http.get<any[]>(this.apiBase + '/loans/getSalesReportDetails/' + appId).subscribe(
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
      this.errorMessage = 'You do not have permission to edit sales reports.';
      return;
    }

    this.isEditing = !this.isEditing;
    if (this.isEditing && this.data.length > 0) {
      this.populateForm(this.data);
    }
  }

  addNewReport() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to add sales records.';
      return;
    }

    this.reportsFormArray.push(this.createReportGroup());
    this.isEditing = true;
  }

  removeReport(index: number) {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to remove sales records.';
      return;
    }

    this.reportsFormArray.removeAt(index);
  }

  cancelEdit() {
    if (!this.canEdit) {
      this.errorMessage = 'You do not have permission to edit sales reports.';
      return;
    }

    this.isEditing = false;
    if (this.data.length > 0) {
      this.populateForm(this.data);
    }
  }

  clearMessages() {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private normalizeUploadedReports(reports: any[]): any[] {
    return reports.filter((report) => this.isSalesDataRow(report));
  }

  private isSalesDataRow(report: any): boolean {
    const date = String(report?.date ?? '').trim();
    const order = String(report?.orderno ?? '').trim();
    const invoice = String(report?.invoiceno ?? '').trim();
    const party = String(report?.partyName ?? '').trim();
    const phone = String(report?.partyPhoneNum ?? '').trim();

    const combined = `${date} ${order} ${invoice} ${party} ${phone}`.toUpperCase();

    if (!combined) {
      return false;
    }

    if (combined.includes('PHONE NO') || combined.includes('EMAIL ID')) {
      return false;
    }

    if (combined.includes('ORDER NO') && combined.includes('INVOICE')) {
      return false;
    }

    return !!(date || order || invoice || party);
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

  ngOnInit(): void {
    this.canEdit = this.authorizationService.canEditSalesReport();
    this.id = new URLSearchParams(window.location.search).get('id');
    if (this.id) {
      this.fetchData(Number(this.id));
    }
  }
}
