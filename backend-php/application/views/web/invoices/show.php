<div class="mb-3">
	<a href="<?= site_url('invoices') ?>" class="text-decoration-none small"><i class="fa-solid fa-arrow-left"></i> Back to invoices</a>
</div>

<div class="row g-3">
	<div class="col-lg-4">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2><?= ops_or_dash($invoice['number']) ?></h2>
				<?= ops_status_badge($invoice['status']) ?>
			</div>
			<div class="ops-card-body">
				<div class="ops-detail-row"><div class="ops-detail-label">Customer</div><div class="ops-detail-value"><a href="<?= site_url('customers/'.$invoice['customer_id']) ?>"><?= html_escape($invoice['customer_name']) ?></a></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Issue date</div><div class="ops-detail-value"><?= ops_date($invoice['issue_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Due date</div><div class="ops-detail-value"><?= ops_date($invoice['due_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Amount paid</div><div class="ops-detail-value"><?= ops_money($invoice['amount_paid']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Outstanding</div><div class="ops-detail-value"><?= ops_money((float) $invoice['total'] - (float) $invoice['amount_paid']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Notes</div><div class="ops-detail-value"><?= ops_or_dash($invoice['notes']) ?></div></div>
			</div>
		</div>
	</div>

	<div class="col-lg-8">
		<div class="ops-card">
			<div class="ops-card-header"><h2>Line items</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Description</th><th>Qty</th><th>Unit price</th><th>Line total</th></tr></thead>
					<tbody>
					<?php if (empty($line_items)): ?><tr><td colspan="4" class="text-muted text-center py-3">No line items.</td></tr><?php endif; ?>
					<?php foreach ($line_items as $item): ?>
						<tr>
							<td><?= html_escape($item['description']) ?></td>
							<td><?= $item['quantity'] ?></td>
							<td><?= ops_money($item['unit_price']) ?></td>
							<td><?= ops_money($item['line_total']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
					<tfoot>
						<tr><td colspan="3" class="text-end text-muted">Subtotal</td><td><?= ops_money($invoice['subtotal']) ?></td></tr>
						<tr><td colspan="3" class="text-end text-muted">Discount</td><td><?= ops_money($invoice['discount_amount']) ?></td></tr>
						<tr><td colspan="3" class="text-end text-muted">VAT</td><td><?= ops_money($invoice['vat_amount']) ?></td></tr>
						<tr class="fw-bold"><td colspan="3" class="text-end">Total</td><td><?= ops_money($invoice['total']) ?></td></tr>
					</tfoot>
				</table>
			</div>
		</div>

		<div class="ops-card">
			<div class="ops-card-header"><h2>Payments</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Date</th><th>Method</th><th>Reference</th><th>Amount</th></tr></thead>
					<tbody>
					<?php if (empty($payments)): ?><tr><td colspan="4" class="text-muted text-center py-3">No payments recorded.</td></tr><?php endif; ?>
					<?php foreach ($payments as $payment): ?>
						<tr>
							<td><?= ops_date($payment['paid_date']) ?></td>
							<td><?= strtoupper($payment['method']) ?></td>
							<td><?= ops_or_dash($payment['reference']) ?></td>
							<td><?= ops_money($payment['amount']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
