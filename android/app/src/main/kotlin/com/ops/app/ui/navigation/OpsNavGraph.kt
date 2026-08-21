package com.ops.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ops.app.ui.businesssetup.BusinessSetupScreen
import com.ops.app.ui.customers.CustomerDetailScreen
import com.ops.app.ui.customers.CustomerListScreen
import com.ops.app.ui.customers.NewCustomerScreen
import com.ops.app.ui.employees.EmployeeEditScreen
import com.ops.app.ui.employees.EmployeeListScreen
import com.ops.app.ui.employees.PayslipEditScreen
import com.ops.app.ui.expenses.ExpenseEditScreen
import com.ops.app.ui.home.HomeScreen
import com.ops.app.ui.invoices.InvoiceEditScreen
import com.ops.app.ui.invoices.InvoicePreviewScreen
import com.ops.app.ui.jobs.JobDetailScreen
import com.ops.app.ui.leads.LeadDetailScreen
import com.ops.app.ui.leads.LeadListScreen
import com.ops.app.ui.leads.NewLeadScreen
import com.ops.app.ui.money.MoneyScreen
import com.ops.app.ui.payments.RecordPaymentScreen
import com.ops.app.ui.quotes.QuoteEditScreen
import com.ops.app.ui.quotes.QuotePreviewScreen
import com.ops.app.ui.settings.BusinessProfileScreen
import com.ops.app.ui.splash.SplashScreen
import com.ops.app.ui.suppliers.SupplierEditScreen
import com.ops.app.ui.suppliers.SupplierListScreen
import com.ops.app.ui.syncstatus.SyncStatusScreen

/**
 * The whole app's navigation graph — one [NavHost], one route per screen in
 * DISCOVERY.md section 10. See [OpsDestinations] for the route table.
 */
@Composable
fun rememberOpsNavController(): NavHostController = rememberNavController()

@Composable
fun OpsNavGraph(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    NavHost(navController = navController, startDestination = OpsDestinations.SPLASH, modifier = modifier) {

        composable(OpsDestinations.SPLASH) {
            SplashScreen(
                onNavigateHome = {
                    navController.navigate(OpsDestinations.HOME) {
                        popUpTo(OpsDestinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateBusinessSetup = {
                    navController.navigate(OpsDestinations.BUSINESS_SETUP) {
                        popUpTo(OpsDestinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(OpsDestinations.BUSINESS_SETUP) {
            BusinessSetupScreen(
                onSetupComplete = {
                    navController.navigate(OpsDestinations.HOME) {
                        popUpTo(OpsDestinations.BUSINESS_SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(OpsDestinations.HOME) {
            HomeScreen(
                onOpenSyncStatus = { navController.navigate(OpsDestinations.SYNC_STATUS) },
                onOpenSettings = { navController.navigate(OpsDestinations.BUSINESS_PROFILE) },
                onOpenLead = { navController.navigate(OpsDestinations.leadDetail(it)) },
                onOpenJob = { navController.navigate(OpsDestinations.jobDetail(it)) },
                onNewLead = { navController.navigate(OpsDestinations.LEAD_NEW) },
                onNewCustomer = { navController.navigate(OpsDestinations.CUSTOMER_NEW) },
                onPickCustomerForQuote = { navController.navigate(OpsDestinations.customers("quote")) },
                onPickCustomerForInvoice = { navController.navigate(OpsDestinations.customers("invoice")) },
                onPickCustomerForPayment = { navController.navigate(OpsDestinations.customers("payment")) },
                onNewExpense = { navController.navigate(OpsDestinations.expenseEditNew()) },
            )
        }

        composable(OpsDestinations.LEADS) {
            LeadListScreen(
                onOpenLead = { navController.navigate(OpsDestinations.leadDetail(it)) },
                onNewLead = { navController.navigate(OpsDestinations.LEAD_NEW) },
            )
        }

        composable(OpsDestinations.LEAD_NEW) {
            NewLeadScreen(
                onBack = { navController.popBackStack() },
                onSaved = { leadId ->
                    navController.popBackStack()
                    navController.navigate(OpsDestinations.leadDetail(leadId))
                },
            )
        }

        composable(
            route = OpsDestinations.LEAD_DETAIL,
            arguments = listOf(navArgument("leadId") { type = NavType.StringType }),
        ) {
            LeadDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenCustomer = { navController.navigate(OpsDestinations.customerDetail(it)) },
                onCreateQuote = { customerId, leadId -> navController.navigate(OpsDestinations.quoteEditNew(customerId, leadId)) },
            )
        }

        composable(
            route = OpsDestinations.CUSTOMERS_PICKABLE,
            arguments = listOf(navArgument("pickMode") { type = NavType.StringType; defaultValue = "none" }),
        ) { backStackEntry ->
            val pickMode = backStackEntry.arguments?.getString("pickMode") ?: "none"
            CustomerListScreen(
                isPicking = pickMode != "none",
                onOpenCustomer = { customerId ->
                    when (pickMode) {
                        "quote" -> navController.navigate(OpsDestinations.quoteEditNew(customerId))
                        "invoice" -> navController.navigate(OpsDestinations.invoiceEditNew(customerId))
                        "payment" -> navController.navigate(OpsDestinations.recordPayment(customerId))
                        else -> navController.navigate(OpsDestinations.customerDetail(customerId))
                    }
                },
                onNewCustomer = { navController.navigate(OpsDestinations.CUSTOMER_NEW) },
            )
        }

        composable(OpsDestinations.CUSTOMER_NEW) {
            NewCustomerScreen(
                onBack = { navController.popBackStack() },
                onSaved = { customerId ->
                    navController.popBackStack()
                    navController.navigate(OpsDestinations.customerDetail(customerId))
                },
            )
        }

        composable(
            route = OpsDestinations.CUSTOMER_DETAIL,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) {
            CustomerDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenQuote = { navController.navigate(OpsDestinations.quotePreview(it)) },
                onOpenJob = { navController.navigate(OpsDestinations.jobDetail(it)) },
                onOpenInvoice = { navController.navigate(OpsDestinations.invoicePreview(it)) },
                onNewQuote = { customerId -> navController.navigate(OpsDestinations.quoteEditNew(customerId)) },
                onNewInvoice = { customerId -> navController.navigate(OpsDestinations.invoiceEditNew(customerId)) },
                onRecordPayment = { customerId -> navController.navigate(OpsDestinations.recordPayment(customerId)) },
            )
        }

        composable(
            route = OpsDestinations.QUOTE_EDIT,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("leadId") { type = NavType.StringType },
                navArgument("quoteId") { type = NavType.StringType },
            ),
        ) {
            QuoteEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { quoteId ->
                    navController.navigate(OpsDestinations.quotePreview(quoteId)) {
                        popUpTo(OpsDestinations.QUOTE_EDIT) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = OpsDestinations.QUOTE_PREVIEW,
            arguments = listOf(navArgument("quoteId") { type = NavType.StringType }),
        ) {
            QuotePreviewScreen(
                onBack = { navController.popBackStack() },
                onJobReady = { jobId ->
                    navController.navigate(OpsDestinations.jobDetail(jobId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = OpsDestinations.JOB_DETAIL,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
        ) {
            JobDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenInvoice = { navController.navigate(OpsDestinations.invoicePreview(it)) },
                onCreateInvoice = { customerId, jobId, quoteId ->
                    navController.navigate(OpsDestinations.invoiceEditNew(customerId, jobId, quoteId))
                },
            )
        }

        composable(
            route = OpsDestinations.INVOICE_EDIT,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("jobId") { type = NavType.StringType },
                navArgument("quoteId") { type = NavType.StringType },
                navArgument("invoiceId") { type = NavType.StringType },
            ),
        ) {
            InvoiceEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { invoiceId ->
                    navController.navigate(OpsDestinations.invoicePreview(invoiceId)) {
                        popUpTo(OpsDestinations.INVOICE_EDIT) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = OpsDestinations.INVOICE_PREVIEW,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
        ) {
            InvoicePreviewScreen(
                onBack = { navController.popBackStack() },
                onRecordPayment = { customerId, invoiceId -> navController.navigate(OpsDestinations.recordPayment(customerId, invoiceId)) },
            )
        }

        composable(
            route = OpsDestinations.RECORD_PAYMENT,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("invoiceId") { type = NavType.StringType },
            ),
        ) {
            RecordPaymentScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(OpsDestinations.MONEY) {
            MoneyScreen(
                onOpenInvoice = { navController.navigate(OpsDestinations.invoicePreview(it)) },
                onOpenExpense = { navController.navigate(OpsDestinations.expenseEditExisting(it)) },
                onNewExpense = { navController.navigate(OpsDestinations.expenseEditNew()) },
                onOpenSuppliers = { navController.navigate(OpsDestinations.SUPPLIERS) },
            )
        }

        composable(
            route = OpsDestinations.EXPENSE_EDIT,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType }),
        ) {
            ExpenseEditScreen(onBack = { navController.popBackStack() })
        }

        composable(OpsDestinations.SUPPLIERS) {
            SupplierListScreen(
                onBack = { navController.popBackStack() },
                onOpenSupplier = { navController.navigate(OpsDestinations.supplierEditExisting(it)) },
                onNewSupplier = { navController.navigate(OpsDestinations.supplierEditNew()) },
            )
        }

        composable(
            route = OpsDestinations.SUPPLIER_EDIT,
            arguments = listOf(navArgument("supplierId") { type = NavType.StringType }),
        ) {
            SupplierEditScreen(
                onBack = { navController.popBackStack() },
                onOpenExpense = { navController.navigate(OpsDestinations.expenseEditExisting(it)) },
            )
        }

        composable(OpsDestinations.EMPLOYEES) {
            EmployeeListScreen(
                onBack = { navController.popBackStack() },
                onOpenEmployee = { navController.navigate(OpsDestinations.employeeEditExisting(it)) },
                onNewEmployee = { navController.navigate(OpsDestinations.employeeEditNew()) },
            )
        }

        composable(
            route = OpsDestinations.EMPLOYEE_EDIT,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType }),
        ) {
            EmployeeEditScreen(
                onBack = { navController.popBackStack() },
                // employeeId here is always the real, current one — passed
                // up from the screen's own live uiState, not the route arg
                // (which is still the NONE sentinel until this employee's
                // first save, even after that save resolves a real id).
                onOpenPayslip = { employeeId, payslipId ->
                    navController.navigate(OpsDestinations.payslipEditExisting(employeeId, payslipId))
                },
                onNewPayslip = { employeeId ->
                    navController.navigate(OpsDestinations.payslipEditNew(employeeId))
                },
            )
        }

        composable(
            route = OpsDestinations.PAYSLIP_EDIT,
            arguments = listOf(
                navArgument("employeeId") { type = NavType.StringType },
                navArgument("payslipId") { type = NavType.StringType },
            ),
        ) {
            PayslipEditScreen(onBack = { navController.popBackStack() })
        }

        composable(OpsDestinations.SYNC_STATUS) {
            SyncStatusScreen(onBack = { navController.popBackStack() })
        }

        composable(OpsDestinations.BUSINESS_PROFILE) {
            BusinessProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenEmployees = { navController.navigate(OpsDestinations.EMPLOYEES) },
                onLoggedOut = {
                    navController.navigate(OpsDestinations.BUSINESS_SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
