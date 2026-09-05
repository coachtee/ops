<div class="row g-3 mb-1">
	<div class="col-6 col-lg-3">
		<div class="ops-stat-card">
			<div class="ops-stat-label">Revenue this month</div>
			<div class="ops-stat-value"><?= ops_money($revenue) ?></div>
		</div>
	</div>
	<div class="col-6 col-lg-3">
		<div class="ops-stat-card">
			<div class="ops-stat-label">Expenses this month</div>
			<div class="ops-stat-value"><?= ops_money($expenses) ?></div>
		</div>
	</div>
	<div class="col-6 col-lg-3">
		<div class="ops-stat-card">
			<div class="ops-stat-label">Profit this month</div>
			<div class="ops-stat-value"><?= ops_money($profit) ?></div>
		</div>
	</div>
	<div class="col-6 col-lg-3">
		<div class="ops-stat-card">
			<div class="ops-stat-label">Outstanding</div>
			<div class="ops-stat-value"><?= ops_money($outstanding) ?></div>
		</div>
	</div>
</div>

<div class="row g-3 mt-1">
	<div class="col-lg-7">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2>Recent invoices</h2>
				<a href="<?= site_url('invoices') ?>" class="small">View all</a>
			</div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Number</th><th>Customer</th><th>Total</th><th>Status</th></tr></thead>
					<tbody>
					<?php if (empty($recent_invoices)): ?>
						<tr><td colspan="4" class="text-muted text-center py-4">No invoices yet.</td></tr>
					<?php endif; ?>
					<?php foreach ($recent_invoices as $invoice): ?>
						<tr onclick="window.location='<?= site_url('invoices/'.$invoice['id']) ?>'">
							<td><?= ops_or_dash($invoice['number']) ?></td>
							<td><?= html_escape($invoice['customer_name']) ?></td>
							<td><?= ops_money($invoice['total']) ?></td>
							<td><?= ops_status_badge($invoice['status']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>

	<div class="col-lg-5">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2>Open quotes</h2>
			</div>
			<div class="ops-card-body">
				<div class="ops-stat-value"><?= (int) $open_quotes ?></div>
				<div class="text-muted small">draft or sent, awaiting a decision</div>
			</div>
		</div>

		<div class="ops-card">
			<div class="ops-card-header">
				<h2>Recent leads</h2>
				<a href="<?= site_url('leads') ?>" class="small">View all</a>
			</div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Name</th><th>Status</th></tr></thead>
					<tbody>
					<?php if (empty($recent_leads)): ?>
						<tr><td colspan="2" class="text-muted text-center py-4">No leads yet.</td></tr>
					<?php endif; ?>
					<?php foreach ($recent_leads as $lead): ?>
						<tr onclick="window.location='<?= site_url('leads/'.$lead['id']) ?>'">
							<td><?= html_escape($lead['name']) ?></td>
							<td><?= ops_status_badge($lead['status']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
