<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Registers every syncable model — mirrors backend/sync/registry.py's
 * registry.register("model_key", Model, Serializer) call in the Django
 * backend this replaces. Adding a new resource to sync push/pull is meant
 * to stay a one-line change here (plus its own Model class), same promise
 * the Django version's registry doc comment made.
 *
 * Order matters for iteration during GET pull only (Sync::pull_get() loops
 * over this array) — for POST push, Sync::push_post() sorts the incoming
 * batch by SYNC_MODEL_APPLY_ORDER (see that file), not by this array's
 * order, mirroring backend/sync/services.py's MODEL_APPLY_ORDER exactly.
 */
$config['sync_registry'] = array(
	'lead' => 'Lead_model',
	'customer' => 'Customer_model',
	'quote' => 'Quote_model',
	'quote_line_item' => 'Quote_line_item_model',
	'job' => 'Job_model',
	'invoice' => 'Invoice_model',
	'invoice_line_item' => 'Invoice_line_item_model',
	'payment' => 'Payment_model',
	'supplier' => 'Supplier_model',
	'expense' => 'Expense_model',
	'employee' => 'Employee_model',
	'payslip' => 'Payslip_model',
	'compliance_item' => 'Compliance_item_model',
	'visit' => 'Visit_model',
);
