import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { BusinessServicingComponent } from './business-servicing.component';

describe('BusinessServicingComponent', () => {
  let fixture: ComponentFixture<BusinessServicingComponent>;
  let component: BusinessServicingComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusinessServicingComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessServicingComponent);
    component = fixture.componentInstance;
    component.repaymentForm = new FormGroup({
      amount: new FormControl(1000),
      paymentMode: new FormControl('UPI'),
      transactionReference: new FormControl('TXN-1'),
      paymentDate: new FormControl('2026-04-04'),
      notes: new FormControl('')
    });
    component.reportForm = new FormGroup({
      from: new FormControl('2026-04-01'),
      to: new FormControl('2026-04-30'),
      size: new FormControl(10)
    });
    component.paymentModes = ['UPI', 'NEFT'] as any;
    component.accounts = [
      {
        id: 10,
        accountNumber: 'BL-1001',
        borrowerName: 'Asha Rao',
        principalAmount: 250000,
        outstandingPrincipal: 175000,
        status: 'ACTIVE',
        nextDueDate: '2026-04-10'
      } as any
    ];
    component.selectedAccount = component.accounts[0];
    component.report = {
      disbursedCount: 3,
      totalPrincipalDisbursed: 500000,
      totalOutstandingPrincipal: 300000,
      items: []
    } as any;
    fixture.detectChanges();
  });

  it('emits repayment when the record button is clicked', () => {
    spyOn(component.recordRepayment, 'emit');

    const button = Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((element) => (element as HTMLButtonElement).textContent?.includes('Record repayment')) as HTMLButtonElement;

    button.click();

    expect(component.recordRepayment.emit).toHaveBeenCalled();
  });

  it('emits account selection from the servicing list', () => {
    spyOn(component.selectAccount, 'emit');

    const card = fixture.nativeElement.querySelector('.list-card--selectable') as HTMLElement;
    card.click();

    expect(component.selectAccount.emit).toHaveBeenCalledWith(component.accounts[0]);
  });
});
