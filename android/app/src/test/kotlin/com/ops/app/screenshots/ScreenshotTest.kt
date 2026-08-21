package com.ops.app.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.ComplianceItemEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.data.local.entities.SupplierEntity
import com.ops.app.data.sync.SyncChipState
import com.ops.app.ui.businesssetup.BusinessSetupContent
import com.ops.app.ui.businesssetup.BusinessSetupForm
import com.ops.app.ui.businesssetup.BusinessSetupMode
import com.ops.app.ui.businesssetup.BusinessSetupUiState
import com.ops.app.ui.compliance.ComplianceListContent
import com.ops.app.ui.customers.CustomerDetailContent
import com.ops.app.ui.customers.CustomerDetailUiState
import com.ops.app.ui.employees.EmployeeListContent
import com.ops.app.ui.employees.PayslipEditContent
import com.ops.app.ui.employees.PayslipEditUiState
import com.ops.app.ui.expenses.ExpenseEditContent
import com.ops.app.ui.expenses.ExpenseEditUiState
import com.ops.app.ui.home.HomeContent
import com.ops.app.ui.home.HomeUiState
import com.ops.app.ui.invoices.InvoiceEditContent
import com.ops.app.ui.invoices.InvoiceEditUiState
import com.ops.app.ui.invoices.InvoiceLineItemRow
import com.ops.app.ui.invoices.InvoicePreviewContent
import com.ops.app.ui.invoices.InvoicePreviewUiState
import com.ops.app.ui.jobs.JobDetailContent
import com.ops.app.ui.jobs.JobDetailUiState
import com.ops.app.ui.leads.LeadDetailContent
import com.ops.app.ui.leads.LeadListFilter
import com.ops.app.ui.leads.LeadListScreenContent
import com.ops.app.ui.leads.LeadListUiState
import com.ops.app.ui.money.MoneyContent
import com.ops.app.ui.money.MoneyUiState
import com.ops.app.ui.payments.RecordPaymentContent
import com.ops.app.ui.payments.RecordPaymentUiState
import com.ops.app.ui.quotes.QuoteEditContent
import com.ops.app.ui.quotes.QuoteEditUiState
import com.ops.app.ui.quotes.QuoteLineItemRow
import com.ops.app.ui.quotes.QuotePreviewContent
import com.ops.app.ui.quotes.QuotePreviewUiState
import com.ops.app.ui.reports.ExpenseCategoryRow
import com.ops.app.ui.reports.MonthlyProfitRow
import com.ops.app.ui.reports.ReportsContent
import com.ops.app.ui.reports.ReportsUiState
import com.ops.app.ui.settings.BusinessProfileContent
import com.ops.app.ui.suppliers.SupplierListContent
import com.ops.app.ui.theme.OpsTheme
import com.ops.coredomain.ComplianceCategory
import com.ops.coredomain.CustomerType
import com.ops.coredomain.ExpenseCategory
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.JobStatus
import com.ops.coredomain.LeadSource
import com.ops.coredomain.LeadStatus
import com.ops.coredomain.PayRateType
import com.ops.coredomain.PaymentMethod
import com.ops.coredomain.QuoteStatus
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Renders the REAL production screen Composables (each screen's own
 * `XContent` function — the exact code the app ships, minus only the
 * Hilt/Room/WorkManager wiring that a JVM screenshot test can't provide)
 * with hand-built, realistic South African small-business demo data, at a
 * realistic phone resolution. This is a visual pack, not a functional
 * test: it proves what the UI looks like, not that taps/sync/persistence
 * work — see android/README.md's "visual vs functional" note and the
 * milestone report that shipped alongside it.
 *
 * Demo data below is Thabo's Plumbing & Maintenance — the same fictional
 * Cape Town plumbing business `backend/seed_demo.py` seeds for the
 * Android demo script, kept consistent here so the screenshot pack reads
 * as one coherent business, not disconnected fixtures per screen.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.Light.NoActionBar")

    private val now = "2026-08-21T09:00:00Z"

    // ---- Shared demo fixtures -------------------------------------------------

    private val business = BusinessEntity(
        id = "biz-1",
        name = "Thabo's Plumbing & Maintenance",
        tradingName = "Thabo's Plumbing",
        registrationNumber = "2019/123456/07",
        taxNumber = "9012345678",
        vatNumber = "",
        isVatRegistered = false,
        industry = "other",
        phone = "+27 82 123 4567",
        email = "thabo@thabosplumbing.co.za",
        addressLine1 = "12 Vygie Street",
        addressLine2 = "",
        suburb = "Delft",
        city = "Cape Town",
        province = "western_cape",
        postalCode = "7100",
        logoUrl = null,
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val customer = CustomerEntity(
        id = "cust-1",
        name = "Nomvula Dlamini",
        customerType = CustomerType.INDIVIDUAL.wire,
        phone = "+27 71 234 5678",
        email = "nomvula.dlamini@gmail.com",
        addressLine1 = "45 Protea Avenue",
        addressLine2 = "",
        suburb = "Bellville",
        city = "Cape Town",
        province = "western_cape",
        postalCode = "7530",
        notes = "Prefers WhatsApp. Access via the side gate.",
        sourceLeadId = "lead-1",
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val leadFollowUp = LeadEntity(
        id = "lead-2",
        name = "Sipho Mahlangu",
        phone = "+27 73 555 1122",
        email = "sipho.m@gmail.com",
        source = LeadSource.WHATSAPP.wire,
        enquiry = "Burst pipe under the kitchen sink, needs someone urgently.",
        notes = "Called back, available Thursday afternoon.",
        status = LeadStatus.CONTACTED.wire,
        followUpDate = "2026-08-21",
        convertedCustomerId = null,
        updatedAt = now,
        syncState = SyncState.PENDING,
    )

    private val leadNew = LeadEntity(
        id = "lead-3",
        name = "Aisha Adams",
        phone = "+27 84 990 2211",
        email = "",
        source = LeadSource.FACEBOOK.wire,
        enquiry = "Quote for re-piping a small bathroom renovation.",
        notes = "",
        status = LeadStatus.NEW.wire,
        followUpDate = "2026-08-22",
        convertedCustomerId = null,
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val job = JobEntity(
        id = "job-1",
        customerId = "cust-1",
        quoteId = "quote-1",
        number = "JOB-0001",
        title = "Geyser replacement — Dlamini residence",
        description = "",
        status = JobStatus.IN_PROGRESS.wire,
        startDate = "2026-08-19",
        dueDate = "2026-08-23",
        completedDate = null,
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val quote = QuoteEntity(
        id = "quote-1",
        customerId = "cust-1",
        leadId = "lead-1",
        number = "QUO-0001",
        status = QuoteStatus.SENT.wire,
        issueDate = "2026-08-18",
        validUntil = "2026-09-01",
        notes = "Includes removal and disposal of the old geyser.",
        terms = "50% deposit on acceptance, balance on completion.",
        isVatApplicable = false,
        discountAmount = "0.00",
        subtotal = "4500.00",
        vatAmount = "0.00",
        total = "4500.00",
        sentAt = "2026-08-18T10:00:00Z",
        acceptedAt = null,
        declinedAt = null,
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val quoteLineItems = listOf(
        QuoteLineItemEntity(id = "qli-1", quoteId = "quote-1", description = "150L geyser, supply and install", quantity = "1.00", unitPrice = "3800.00", lineTotal = "3800.00", sortOrder = 0, updatedAt = now, syncState = SyncState.SYNCED),
        QuoteLineItemEntity(id = "qli-2", quoteId = "quote-1", description = "Call-out and labour", quantity = "1.00", unitPrice = "700.00", lineTotal = "700.00", sortOrder = 1, updatedAt = now, syncState = SyncState.SYNCED),
    )

    private val invoice = InvoiceEntity(
        id = "inv-1",
        customerId = "cust-1",
        jobId = "job-1",
        quoteId = "quote-1",
        number = "INV-0001",
        status = InvoiceStatus.PARTIALLY_PAID.wire,
        issueDate = "2026-08-20",
        dueDate = "2026-09-03",
        notes = "",
        terms = "Payment due within 14 days.",
        isVatApplicable = false,
        discountAmount = "0.00",
        subtotal = "4500.00",
        vatAmount = "0.00",
        total = "4500.00",
        amountPaid = "2250.00",
        sentAt = "2026-08-20T14:00:00Z",
        updatedAt = now,
        syncState = SyncState.SYNCED,
    )

    private val invoiceLineItems = listOf(
        InvoiceLineItemEntity(id = "ili-1", invoiceId = "inv-1", description = "150L geyser, supply and install", quantity = "1.00", unitPrice = "3800.00", lineTotal = "3800.00", sortOrder = 0, updatedAt = now, syncState = SyncState.SYNCED),
        InvoiceLineItemEntity(id = "ili-2", invoiceId = "inv-1", description = "Call-out and labour", quantity = "1.00", unitPrice = "700.00", lineTotal = "700.00", sortOrder = 1, updatedAt = now, syncState = SyncState.SYNCED),
    )

    private val payment = PaymentEntity(
        id = "pay-1", customerId = "cust-1", invoiceId = "inv-1", amount = "2250.00",
        method = PaymentMethod.EFT.wire, reference = "EFT ref 88213", paidDate = "2026-08-20",
        notes = "50% deposit", updatedAt = now, syncState = SyncState.SYNCED,
    )

    private val suppliers = listOf(
        SupplierEntity(id = "sup-1", name = "Cape Plumbing Supplies", contactPerson = "Riaan Botha", phone = "+27 21 555 0101", email = "sales@capeplumbing.co.za", notes = "Best prices on geysers.", updatedAt = now, syncState = SyncState.SYNCED),
        SupplierEntity(id = "sup-2", name = "BuildIt Delft", contactPerson = "", phone = "+27 21 555 0199", email = "", notes = "", updatedAt = now, syncState = SyncState.SYNCED),
    )

    private val expense = ExpenseEntity(
        id = "exp-1", jobId = "job-1", supplierId = "sup-1",
        category = ExpenseCategory.MATERIALS_STOCK.wire, description = "150L geyser and fittings",
        amount = "2300.00", isVatApplicable = true, vatAmount = "300.00", date = "2026-08-19",
        receiptUrl = null, localReceiptPath = null, receiptSyncState = ReceiptSyncState.NONE,
        receiptSyncError = null, updatedAt = now, syncState = SyncState.SYNCED,
    )

    private val expenseFuel = ExpenseEntity(
        id = "exp-2", jobId = null, supplierId = null,
        category = ExpenseCategory.FUEL_TRAVEL.wire, description = "Diesel — bakkie",
        amount = "650.00", isVatApplicable = true, vatAmount = "84.78", date = "2026-08-18",
        receiptUrl = null, localReceiptPath = null, receiptSyncState = ReceiptSyncState.NONE,
        receiptSyncError = null, updatedAt = now, syncState = SyncState.SYNCED,
    )

    private val employees = listOf(
        EmployeeEntity(id = "emp-1", name = "Bongani Sithole", role = "Plumber's assistant", phone = "+27 78 222 3344", email = "", payRateType = PayRateType.HOURLY.wire, payRate = "85.00", startDate = "2025-03-01", notes = "", updatedAt = now, syncState = SyncState.SYNCED),
    )

    private val complianceItems = listOf(
        ComplianceItemEntity(id = "ci-1", category = ComplianceCategory.PAYE_UIF_SDL.wire, title = "PAYE / UIF / SDL — July 2026", dueDate = "2026-08-07", completedDate = "2026-08-05", isRecurring = true, notes = "", updatedAt = now, syncState = SyncState.SYNCED),
        ComplianceItemEntity(id = "ci-2", category = ComplianceCategory.PAYE_UIF_SDL.wire, title = "PAYE / UIF / SDL — August 2026", dueDate = "2026-09-07", completedDate = null, isRecurring = true, notes = "", updatedAt = now, syncState = SyncState.PENDING),
        ComplianceItemEntity(id = "ci-3", category = ComplianceCategory.CIPC_ANNUAL_RETURN.wire, title = "CIPC annual return", dueDate = "2026-11-30", completedDate = null, isRecurring = true, notes = "", updatedAt = now, syncState = SyncState.SYNCED),
    )

    // ---- Screens ----------------------------------------------------------

    @Test
    fun `01 business setup`() {
        paparazzi.snapshot(name = "01-business-setup") {
            OpsTheme {
                BusinessSetupContent(
                    uiState = BusinessSetupUiState(
                        mode = BusinessSetupMode.CREATE,
                        step = 0,
                        form = BusinessSetupForm(
                            firstName = "Thabo", lastName = "Nkosi", email = "thabo@thabosplumbing.co.za",
                            password = "********", businessName = "Thabo's Plumbing & Maintenance",
                            tradingName = "Thabo's Plumbing", industry = "other",
                            businessPhone = "+27 82 123 4567", businessEmail = "info@thabosplumbing.co.za",
                        ),
                    ),
                    onSetMode = {}, onUpdateForm = {}, onNextStep = {}, onPreviousStep = {},
                    onPickLogo = {}, onSubmitCreate = {}, onSubmitSignIn = {},
                    onUpdateSignInEmail = {}, onUpdateSignInPassword = {},
                )
            }
        }
    }

    @Test
    fun `02 home dashboard`() {
        paparazzi.snapshot(name = "02-home-dashboard") {
            OpsTheme {
                HomeContent(
                    uiState = HomeUiState(
                        businessName = business.name,
                        moneyInThisMonth = BigDecimal("5100.00"),
                        moneyOutThisMonth = BigDecimal("2950.00"),
                        outstandingTotal = BigDecimal("2250.00"),
                        leadsNeedingFollowUp = listOf(leadFollowUp),
                        activeJobs = listOf(job),
                        upcomingComplianceItem = complianceItems[1].copy(dueDate = "2026-08-28"),
                    ),
                    syncChipState = SyncChipState.Synced,
                    onOpenSyncStatus = {}, onOpenSettings = {}, onOpenLead = {}, onOpenJob = {},
                    onOpenCompliance = {},
                    onNewLead = {}, onNewCustomer = {}, onPickCustomerForQuote = {},
                    onPickCustomerForInvoice = {}, onPickCustomerForPayment = {}, onNewExpense = {},
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun `03 leads`() {
        paparazzi.snapshot(name = "03-leads") {
            OpsTheme {
                LeadListScreenContent(
                    uiState = LeadListUiState(filter = LeadListFilter.ALL, leads = listOf(leadFollowUp, leadNew)),
                    onFilterChange = {}, onOpenLead = {}, onNewLead = {},
                )
            }
        }
    }

    @Test
    fun `04 lead detail`() {
        paparazzi.snapshot(name = "04-lead-detail") {
            OpsTheme {
                LeadDetailContent(
                    lead = leadFollowUp, onBack = {}, onUpdateStatus = {}, onUpdateFollowUpDate = {},
                    onUpdateNotes = {}, onConvertToCustomer = {}, onOpenCustomer = {}, onCreateQuote = {},
                )
            }
        }
    }

    @Test
    fun `05 customer`() {
        paparazzi.snapshot(name = "05-customer") {
            OpsTheme {
                CustomerDetailContent(
                    uiState = CustomerDetailUiState(
                        customer = customer, quotes = listOf(quote), jobs = listOf(job),
                        invoices = listOf(invoice), outstandingTotal = BigDecimal("2250.00"),
                    ),
                    onBack = {}, onOpenQuote = {}, onOpenJob = {}, onOpenInvoice = {},
                    onNewQuote = {}, onNewInvoice = {}, onRecordPayment = {}, onUpdateNotes = {},
                )
            }
        }
    }

    @Test
    fun `06 quote`() {
        paparazzi.snapshot(name = "06-quote") {
            OpsTheme {
                QuotePreviewContent(
                    uiState = QuotePreviewUiState(quote = quote, lineItems = quoteLineItems, customer = customer, business = business),
                    onBack = {}, onMarkSent = {}, onMarkDeclined = {}, onMarkAccepted = {},
                )
            }
        }
    }

    @Test
    fun `07 invoice`() {
        paparazzi.snapshot(name = "07-invoice") {
            OpsTheme {
                InvoicePreviewContent(
                    uiState = InvoicePreviewUiState(invoice = invoice, lineItems = invoiceLineItems, payments = listOf(payment), customer = customer, business = business),
                    onBack = {}, onRecordPayment = { _, _ -> }, onMarkSent = {},
                )
            }
        }
    }

    @Test
    fun `08 money in and out`() {
        paparazzi.snapshot(name = "08-money-in-and-out") {
            OpsTheme {
                MoneyContent(
                    uiState = MoneyUiState(
                        outstandingInvoices = listOf(invoice),
                        paymentsReceived = listOf(payment),
                        expenses = listOf(expense, expenseFuel),
                        customerNames = mapOf(customer.id to customer.name),
                    ),
                    onOpenInvoice = {}, onOpenExpense = {}, onNewExpense = {}, onOpenSuppliers = {},
                )
            }
        }
    }

    @Test
    fun `09 expense`() {
        paparazzi.snapshot(name = "09-expense") {
            OpsTheme {
                ExpenseEditContent(
                    uiState = ExpenseEditUiState(
                        expenseId = expense.id, jobId = expense.jobId, supplierId = expense.supplierId,
                        category = expense.category, description = expense.description, amount = expense.amount,
                        isVatApplicable = true, date = expense.date, syncState = SyncState.SYNCED, isLoading = false,
                    ),
                    jobs = listOf(job), suppliers = suppliers,
                    onBack = {}, onUpdate = {}, onSave = {}, onDelete = {}, onRetryReceiptUpload = {},
                    onTakePhoto = {}, onChoosePhoto = {},
                )
            }
        }
    }

    @Test
    fun `10 expense receipt capture`() {
        paparazzi.snapshot(name = "10-expense-receipt-capture") {
            OpsTheme {
                ExpenseEditContent(
                    uiState = ExpenseEditUiState(
                        expenseId = expense.id, jobId = expense.jobId, supplierId = expense.supplierId,
                        category = expense.category, description = expense.description, amount = expense.amount,
                        isVatApplicable = true, date = expense.date,
                        localReceiptPath = "/data/user/0/com.ops.app/files/receipts/${expense.id}.jpg",
                        receiptSyncState = ReceiptSyncState.PENDING,
                        syncState = SyncState.SYNCED, isLoading = false,
                    ),
                    jobs = listOf(job), suppliers = suppliers,
                    onBack = {}, onUpdate = {}, onSave = {}, onDelete = {}, onRetryReceiptUpload = {},
                    onTakePhoto = {}, onChoosePhoto = {},
                )
            }
        }
    }

    @Test
    fun `11 suppliers`() {
        paparazzi.snapshot(name = "11-suppliers") {
            OpsTheme {
                SupplierListContent(suppliers = suppliers, onBack = {}, onOpenSupplier = {}, onNewSupplier = {})
            }
        }
    }

    @Test
    fun `12 employees`() {
        paparazzi.snapshot(name = "12-employees") {
            OpsTheme {
                EmployeeListContent(employees = employees, onBack = {}, onOpenEmployee = {}, onNewEmployee = {})
            }
        }
    }

    @Test
    fun `13 payslip`() {
        paparazzi.snapshot(name = "13-payslip") {
            OpsTheme {
                PayslipEditContent(
                    uiState = PayslipEditUiState(
                        payslipId = "payslip-1", employeeId = "emp-1", employeeName = "Bongani Sithole",
                        periodStart = "2026-08-11", periodEnd = "2026-08-17", grossPay = "1700.00",
                        deductions = "150.00", deductionsNote = "UIF", paidDate = "2026-08-18",
                        syncState = SyncState.SYNCED, isLoading = false,
                    ),
                    onBack = {}, onUpdate = {}, onMarkPaidToday = {}, onSave = {}, onDelete = {},
                )
            }
        }
    }

    @Test
    fun `14 compliance`() {
        paparazzi.snapshot(name = "14-compliance") {
            OpsTheme {
                ComplianceListContent(complianceItems = complianceItems, onBack = {}, onOpenItem = {}, onNewItem = {})
            }
        }
    }

    @Test
    fun `15 reports`() {
        val thisMonth = YearMonth.of(2026, 8)
        paparazzi.snapshot(name = "15-reports") {
            OpsTheme {
                ReportsContent(
                    ReportsUiState(
                        monthlyProfit = (5 downTo 0).map { offset ->
                            val month = thisMonth.minusMonths(offset.toLong())
                            if (offset == 0) {
                                MonthlyProfitRow(month = month, revenue = BigDecimal("5100.00"), expenses = BigDecimal("2950.00"))
                            } else {
                                MonthlyProfitRow(month = month, revenue = BigDecimal("4200.00"), expenses = BigDecimal("2100.00"))
                            }
                        },
                        expenseCategoriesThisMonth = listOf(
                            ExpenseCategoryRow(category = ExpenseCategory.MATERIALS_STOCK.wire, total = BigDecimal("2300.00")),
                            ExpenseCategoryRow(category = ExpenseCategory.FUEL_TRAVEL.wire, total = BigDecimal("650.00")),
                        ),
                        vatCollectedThisMonth = BigDecimal("0.00"),
                        vatPaidThisMonth = BigDecimal("384.78"),
                    ),
                )
            }
        }
    }

    @Test
    fun `16 settings and profile`() {
        paparazzi.snapshot(name = "16-settings-and-profile") {
            OpsTheme {
                BusinessProfileContent(
                    business = business, isSaving = false, errorMessage = null, pendingLogoBytes = null,
                    onBack = {}, onOpenEmployees = {}, onOpenCompliance = {}, onPickLogo = {},
                    onSave = {}, onLogout = {},
                )
            }
        }
    }

    // ---- Phase 2: the LEAD -> CUSTOMER -> QUOTE -> JOB -> INVOICE -> PAYMENT
    // commercial flow's fast-entry screens, which weren't split into a
    // stateless Content composable before this pass (see android/README.md).

    @Test
    fun `17 quote edit`() {
        paparazzi.snapshot(name = "17-quote-edit") {
            OpsTheme {
                QuoteEditContent(
                    uiState = QuoteEditUiState(
                        quoteId = null,
                        customerId = customer.id,
                        customerName = customer.name,
                        issueDate = "2026-08-21",
                        validUntil = "2026-09-04",
                        isVatApplicable = false,
                        discountAmount = "0.00",
                        lineItems = listOf(
                            QuoteLineItemRow(description = "150L geyser, supply and install", quantity = "1", unitPrice = "3800.00"),
                            QuoteLineItemRow(description = "Call-out and labour", quantity = "1", unitPrice = "700.00"),
                        ),
                        notes = "Includes removal and disposal of the old geyser.",
                        terms = "50% deposit on acceptance, balance on completion.",
                    ),
                    onBack = {}, onUpdate = {}, onUpdateLineItem = { _, _ -> }, onAddLineItem = {},
                    onRemoveLineItem = {}, onSave = {},
                )
            }
        }
    }

    @Test
    fun `18 job detail`() {
        paparazzi.snapshot(name = "18-job-detail") {
            OpsTheme {
                JobDetailContent(
                    uiState = JobDetailUiState(
                        job = job, customer = customer, quote = quote,
                        invoices = listOf(invoice), payments = listOf(payment), expenses = listOf(expense),
                    ),
                    onBack = {}, onOpenCustomer = {}, onOpenQuote = {}, onOpenInvoice = {},
                    onCreateInvoice = { _, _, _ -> }, onUpdateStatus = {}, onUpdateDates = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun `19 invoice edit`() {
        paparazzi.snapshot(name = "19-invoice-edit") {
            OpsTheme {
                InvoiceEditContent(
                    uiState = InvoiceEditUiState(
                        invoiceId = null,
                        customerId = customer.id,
                        jobId = job.id,
                        quoteId = quote.id,
                        customerName = customer.name,
                        issueDate = "2026-08-21",
                        dueDate = "2026-09-04",
                        isVatApplicable = false,
                        discountAmount = "0.00",
                        lineItems = listOf(
                            InvoiceLineItemRow(description = "150L geyser, supply and install", quantity = "1", unitPrice = "3800.00"),
                            InvoiceLineItemRow(description = "Call-out and labour", quantity = "1", unitPrice = "700.00"),
                        ),
                        terms = "Payment due within 14 days.",
                    ),
                    onBack = {}, onUpdate = {}, onUpdateLineItem = { _, _ -> }, onAddLineItem = {},
                    onRemoveLineItem = {}, onSave = {},
                )
            }
        }
    }

    @Test
    fun `20 record payment`() {
        paparazzi.snapshot(name = "20-record-payment") {
            OpsTheme {
                RecordPaymentContent(
                    uiState = RecordPaymentUiState(
                        customerId = customer.id,
                        invoiceId = invoice.id,
                        customerName = customer.name,
                        invoiceNumber = invoice.number,
                        invoiceTotal = BigDecimal(invoice.total),
                        alreadyPaid = BigDecimal(invoice.amountPaid),
                        outstandingOnInvoice = BigDecimal("2250.00"),
                        amount = "2250.00",
                        method = PaymentMethod.EFT.wire,
                        paidDate = "2026-08-21",
                    ),
                    onBack = {}, onUpdate = {}, onSave = {},
                )
            }
        }
    }

    @Test
    fun `21 leads empty state`() {
        paparazzi.snapshot(name = "21-leads-empty-state") {
            OpsTheme {
                LeadListScreenContent(
                    uiState = LeadListUiState(filter = LeadListFilter.NEEDS_FOLLOW_UP, leads = emptyList()),
                    onFilterChange = {}, onOpenLead = {}, onNewLead = {},
                )
            }
        }
    }

    @Test
    fun `22 customer empty state`() {
        paparazzi.snapshot(name = "22-customer-empty-state") {
            OpsTheme {
                CustomerDetailContent(
                    uiState = CustomerDetailUiState(
                        customer = customer.copy(id = "cust-2", name = "New customer, no history yet"),
                        quotes = emptyList(), jobs = emptyList(), invoices = emptyList(),
                        outstandingTotal = BigDecimal.ZERO,
                    ),
                    onBack = {}, onOpenQuote = {}, onOpenJob = {}, onOpenInvoice = {},
                    onNewQuote = {}, onNewInvoice = {}, onRecordPayment = {}, onUpdateNotes = {},
                )
            }
        }
    }
}
