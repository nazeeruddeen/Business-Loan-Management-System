import { expect, Page, test } from '@playwright/test';

const frontendBaseUrl = process.env.BUSINESS_E2E_BASE_URL ?? 'http://127.0.0.1:4300';
const apiBaseUrl = process.env.BUSINESS_E2E_API_BASE_URL ?? 'http://127.0.0.1:8010';
const username = process.env.BUSINESS_E2E_USERNAME ?? 'admin';
const password = requiredEnv('BUSINESS_E2E_PASSWORD');

test.describe('Business loan golden path', () => {
  test('admin can apply for and approve a business loan', async ({ page }) => {
    const suffix = Date.now().toString();
    const borrowerName = `Golden Path Industries ${suffix}`;
    const contactPerson = `Operator ${suffix}`;
    const businessPan = `GPTLP${suffix.slice(-4)}A`;
    const productCode = `GLD-${suffix.slice(-6)}`;
    const productName = `Golden Path Term Loan ${suffix.slice(-4)}`;
    const purpose = `Working capital bridge ${suffix}`;

    await login(page);

    await page.getByTestId('business-tab-borrowers').click();
    await page.getByTestId('business-borrower-name').fill(borrowerName);
    await page.getByLabel('Contact person').fill(contactPerson);
    await page.getByTestId('business-borrower-pan').fill(businessPan);
    await page.getByTestId('business-borrower-email').fill(`golden-${suffix}@example.com`);
    await page.getByTestId('business-borrower-phone').fill(`9${suffix.slice(-9)}`);
    await page.getByTestId('business-borrower-industry').fill('Manufacturing');
    await page.getByTestId('business-borrower-turnover').fill('5000000');
    await page.getByTestId('business-borrower-income').fill('450000');
    await page.getByTestId('business-address-line-one').fill('42 Golden Square');
    await page.getByTestId('business-address-city').fill('Mumbai');
    await page.getByTestId('business-address-state').fill('Maharashtra');
    await page.getByTestId('business-address-postal-code').fill('400001');
    await page.getByTestId('business-address-country').fill('India');
    await page.getByTestId('business-create-borrower').click();
    await expect(page.getByTestId('business-notice')).toContainText('created', { ignoreCase: true });

    for (const documentType of ['PAN_CARD', 'BUSINESS_REGISTRATION', 'BANK_STATEMENT']) {
      await page.getByTestId('business-document-type').selectOption(documentType);
      await page.getByTestId('business-document-file-name').fill(`${documentType.toLowerCase()}-${suffix}.pdf`);
      await page.getByTestId('business-document-file-reference').fill(`s3://business-loan/e2e/${documentType.toLowerCase()}-${suffix}.pdf`);
      await page.getByTestId('business-add-document').click();
      await expect(page.getByTestId(`business-document-card-${documentType}`)).toBeVisible();
      await page.getByTestId(`business-verify-document-${documentType}`).click();
      await expect(page.getByTestId('business-notice')).toContainText('marked as VERIFIED');
    }

    await page.getByTestId('business-tab-products').click();
    await page.getByTestId('business-product-code').fill(productCode);
    await page.getByTestId('business-product-name').fill(productName);
    await page.getByTestId('business-product-min-amount').fill('100000');
    await page.getByTestId('business-product-max-amount').fill('1000000');
    await page.getByTestId('business-product-interest-rate').fill('11.5');
    await page.getByTestId('business-product-tenure').fill('36');
    await page.getByTestId('business-product-criteria').fill('High-income manufacturing customers');
    await page.getByTestId('business-create-product').click();
    await expect(page.getByTestId('business-notice')).toContainText(productCode);

    const borrower = await apiJson<{ items: Array<{ id: number }> }>(
      page,
      'GET',
      `/api/v1/borrowers?businessPan=${businessPan}&page=0&size=5`
    );
    const product = await apiJson<Array<{ id: number; productCode: string }>>(
      page,
      'GET',
      `/api/v1/loan-products?name=${encodeURIComponent(productName)}`
    );
    const reviewers = await apiJson<Array<{ id: number; username: string; role: string; active: boolean }>>(
      page,
      'GET',
      '/auth/users'
    );

    const borrowerId = borrower.items[0]?.id;
    const productId = product.find((item) => item.productCode === productCode)?.id;
    const reviewerId = reviewers.find((item) => item.role === 'REVIEWER' && item.active)?.id;

    expect(borrowerId, 'expected created borrower id').toBeTruthy();
    expect(productId, 'expected created loan product id').toBeTruthy();
    expect(reviewerId, 'expected active reviewer id').toBeTruthy();

    await page.getByTestId('business-tab-applications').click();
    await page.getByTestId('business-eligibility-borrower-id').fill(String(borrowerId));
    await page.getByTestId('business-eligibility-product-id').fill(String(productId));
    await page.getByTestId('business-eligibility-requested-amount').fill('250000');
    await page.getByTestId('business-eligibility-tenure').fill('24');
    await page.getByTestId('business-evaluate-eligibility').click();
    await expect(page.getByText('Eligible')).toBeVisible();

    await page.getByTestId('business-application-borrower-id').fill(String(borrowerId));
    await page.getByTestId('business-application-product-id').fill(String(productId));
    await page.getByTestId('business-application-requested-amount').fill('250000');
    await page.getByTestId('business-application-tenure').fill('24');
    await page.getByTestId('business-application-purpose').fill(purpose);
    await page.getByTestId('business-create-application').click();
    await expect(page.getByTestId('business-notice')).toContainText('created in draft state');

    await page.getByTestId('business-tab-approval').click();
    await expect(page.getByText(borrowerName)).toBeVisible();
    await expect(page.getByTestId('business-selected-status')).toHaveText('DRAFT');

    await page.getByTestId('business-submit-application').click();
    await expect(page.getByTestId('business-selected-status')).toHaveText('SUBMITTED');

    await page.getByTestId('business-reviewer-select').selectOption(String(reviewerId));
    await page.getByTestId('business-assign-reviewer').click();
    await expect(page.getByTestId('business-selected-status')).toHaveText('UNDER_REVIEW');

    await page.getByTestId('business-decision-status').selectOption('APPROVED');
    await page.getByTestId('business-decision-remarks').fill('Approved during Playwright golden-path validation');
    await page.getByTestId('business-save-decision').click();
    await expect(page.getByTestId('business-selected-status')).toHaveText('APPROVED');
  });
});

async function login(page: Page): Promise<void> {
  await page.goto(`${frontendBaseUrl}/overview`);
  await page.getByTestId('business-login-username').fill(username);
  await page.getByTestId('business-login-password').fill(password);
  await page.getByTestId('business-login-submit').click();
  await expect(page.getByTestId('business-tab-dashboard')).toBeVisible();
  await expect(page.getByTestId('business-notice')).toContainText(/Loading|refreshed/i);
}

async function apiJson<T>(page: Page, method: 'GET' | 'POST' | 'PATCH', path: string, data?: unknown): Promise<T> {
  const response = await page.request.fetch(`${apiBaseUrl}${path}`, {
    method,
    data,
    headers: data ? { 'Content-Type': 'application/json' } : undefined
  });
  expect(response.ok(), `${method} ${path} should succeed`).toBeTruthy();
  return (await response.json()) as T;
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}
