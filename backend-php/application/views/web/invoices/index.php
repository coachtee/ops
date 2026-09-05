<div class="ops-card">
	<div class="ops-card-header">
		<h2>Invoices</h2>
		<span class="text-muted small"><?= count($invoices) ?> total</span>
	</div>
	<div class="table-responsive">
		<table class="table ops-table mb-0">
			<thead><tr><th>Number</th><th>Customer</th><th>Issue date</th><th>Total</th><th>Paid</th><th>Status</th></tr></thead>
			<tbody>
			<?php if (empty($invoices)): ?>
				<tr><td colspan="6" class="text-muted text-center py-4">No invoices yet — these sync in from the Android app.</td></tr>
			<?php endif; ?>
			<?php foreach ($invoices as $invoice): ?>
				<tr onclick="window.location='<?= site_url('invoices/'.$invoice['id']) ?>'">
					<td class="fw-semibold"><?= ops_or_dash($invoice['number']) ?></td>
					<td><?= html_escape($invoice['customer_name']) ?></td>
					<td><?= ops_date($invoice['issue_date']) ?></td>
					<td><?= ops_money($invoice['total']) ?></td>
					<td><?= ops_money($invoice['amount_paid']) ?></td>
					<td><?= ops_status_badge($invoice['status']) ?></td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table>
	</div>
</div>
