<div class="ops-card">
	<div class="ops-card-header">
		<h2>Quotes</h2>
		<span class="text-muted small"><?= count($quotes) ?> total</span>
	</div>
	<div class="table-responsive">
		<table class="table ops-table mb-0">
			<thead><tr><th>Number</th><th>Customer</th><th>Issue date</th><th>Total</th><th>Status</th></tr></thead>
			<tbody>
			<?php if (empty($quotes)): ?>
				<tr><td colspan="5" class="text-muted text-center py-4">No quotes yet — these sync in from the Android app.</td></tr>
			<?php endif; ?>
			<?php foreach ($quotes as $quote): ?>
				<tr onclick="window.location='<?= site_url('quotes/'.$quote['id']) ?>'">
					<td class="fw-semibold"><?= ops_or_dash($quote['number']) ?></td>
					<td><?= html_escape($quote['customer_name']) ?></td>
					<td><?= ops_date($quote['issue_date']) ?></td>
					<td><?= ops_money($quote['total']) ?></td>
					<td><?= ops_status_badge($quote['status']) ?></td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table>
	</div>
</div>
