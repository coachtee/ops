package com.ops.app.ui.navigation

/**
 * Route table for the whole app. Screens map 1:1 onto DISCOVERY.md section
 * 10's screen list. Nullable path segments (e.g. "no lead yet", "editing not
 * creating") use the [NONE] sentinel rather than Navigation-Compose's more
 * ceremony-heavy nullable-argument config — each destination's own code
 * treats [NONE] as "absent".
 */
object OpsDestinations {
    const val NONE = "_"

    const val SPLASH = "splash"
    const val BUSINESS_SETUP = "business_setup"

    // Bottom nav destinations
    const val HOME = "home"
    const val LEADS = "leads"
    /** Base path (no args) — for matching the current route to highlight the
     * Customers bottom nav item regardless of [pickMode]. The real,
     * navigable route is [CUSTOMERS_PICKABLE] below. */
    const val CUSTOMERS_BASE = "customers"
    const val MONEY = "money"

    const val LEAD_NEW = "lead_new"
    const val LEAD_DETAIL = "lead_detail/{leadId}"
    fun leadDetail(id: String) = "lead_detail/$id"

    const val CUSTOMER_NEW = "customer_new"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    fun customerDetail(id: String) = "customer_detail/$id"

    /** The Customers list doubles as a customer PICKER when a Home quick
     * action (New quote / New invoice / Record payment) needs a customer
     * first — [pickMode] is one of "none" (normal browsing, bottom nav),
     * "quote", "invoice", "payment". Reuses one screen instead of adding a
     * separate picker screen for what the IA lists as a single destination. */
    const val CUSTOMERS_PICKABLE = "customers/{pickMode}"
    fun customers(pickMode: String = "none") = "customers/$pickMode"

    /** customerId is always known; leadId/quoteId are [NONE] when absent. */
    const val QUOTE_EDIT = "quote_edit/{customerId}/{leadId}/{quoteId}"
    fun quoteEditNew(customerId: String, leadId: String? = null) =
        "quote_edit/$customerId/${leadId ?: NONE}/$NONE"
    fun quoteEditExisting(customerId: String, quoteId: String) =
        "quote_edit/$customerId/$NONE/$quoteId"

    const val QUOTE_PREVIEW = "quote_preview/{quoteId}"
    fun quotePreview(id: String) = "quote_preview/$id"

    const val JOB_DETAIL = "job_detail/{jobId}"
    fun jobDetail(id: String) = "job_detail/$id"

    /** jobId/quoteId/invoiceId are [NONE] when absent (new invoice from scratch,
     * or not sourced from a job/quote). */
    const val INVOICE_EDIT = "invoice_edit/{customerId}/{jobId}/{quoteId}/{invoiceId}"
    fun invoiceEditNew(customerId: String, jobId: String? = null, quoteId: String? = null) =
        "invoice_edit/$customerId/${jobId ?: NONE}/${quoteId ?: NONE}/$NONE"
    fun invoiceEditExisting(customerId: String, invoiceId: String) =
        "invoice_edit/$customerId/$NONE/$NONE/$invoiceId"

    const val INVOICE_PREVIEW = "invoice_preview/{invoiceId}"
    fun invoicePreview(id: String) = "invoice_preview/$id"

    const val RECORD_PAYMENT = "record_payment/{customerId}/{invoiceId}"
    fun recordPayment(customerId: String, invoiceId: String? = null) =
        "record_payment/$customerId/${invoiceId ?: NONE}"

    /** expenseId is [NONE] when creating a new expense — one screen serves
     * both create and edit/view, same convention as quote/invoice edit. */
    const val EXPENSE_EDIT = "expense_edit/{expenseId}"
    fun expenseEditNew() = "expense_edit/$NONE"
    fun expenseEditExisting(id: String) = "expense_edit/$id"

    /** Suppliers list — reached from the Money tab, not a bottom nav item of
     * its own (see MoneyScreen). */
    const val SUPPLIERS = "suppliers"

    /** supplierId is [NONE] when creating a new supplier — one screen serves
     * both create and edit/view, same convention as expense edit. */
    const val SUPPLIER_EDIT = "supplier_edit/{supplierId}"
    fun supplierEditNew() = "supplier_edit/$NONE"
    fun supplierEditExisting(id: String) = "supplier_edit/$id"

    const val SYNC_STATUS = "sync_status"
    const val BUSINESS_PROFILE = "business_profile"

    /** Treat the [NONE] sentinel as a real null, everywhere a route reads a path segment. */
    fun String?.orNull(): String? = if (this == null || this == NONE) null else this
}
