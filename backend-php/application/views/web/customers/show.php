<div class="mb-3">
	<a href="<?= site_url('customers') ?>" class="text-decoration-none small"><i class="fa-solid fa-arrow-left"></i> Back to customers</a>
</div>

<div class="row g-3">
	<div class="col-lg-5">
		<div class="ops-card">
			<div class="ops-card-header"><h2><?= html_escape($customer['name']) ?></h2></div>
			<div class="ops-card-body">
				<div class="ops-detail-row"><div class="ops-detail-label">Type</div><div class="ops-detail-value"><?= ucfirst($customer['customer_type']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Phone</div><div class="ops-detail-value"><?= ops_or_dash($customer['phone']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Email</div><div class="ops-detail-value"><?= ops_or_dash($customer['email']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Address</div><div class="ops-detail-value"><?= ops_or_dash(trim($customer['address_line1'].' '.$customer['suburb'].' '.$customer['city'])) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Province</div><div class="ops-detail-value"><?= ops_or_dash($customer['province']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Notes</div><div class="ops-detail-value"><?= ops_or_dash($customer['notes']) ?></div></div>
			</div>
		</div>
	</div>

	<div class="col-lg-7">
		<div class="ops-card">
			<div class="ops-card-header"><h2>Quotes</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<tbody>
					<?php if (empty($quotes)): ?><tr><td class="text-muted text-center py-3">None yet.</td></tr><?php endif; ?>
					<?php foreach ($quotes as $quote): ?>
						<tr onclick="window.location='<?= site_url('quotes/'.$quote['id']) ?>'">
							<td><?= ops_or_dash($quote['number']) ?></td>
							<td><?= ops_money($quote['total']) ?></td>
							<td><?= ops_status_badge($quote['status']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>

		<div class="ops-card">
			<div class="ops-card-header"><h2>Jobs</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<tbody>
					<?php if (empty($jobs)): ?><tr><td class="text-muted text-center py-3">None yet.</td></tr><?php endif; ?>
					<?php foreach ($jobs as $job): ?>
						<tr onclick="window.location='<?= site_url('jobs/'.$job['id']) ?>'">
							<td><?= ops_or_dash($job['number']) ?></td>
							<td><?= html_escape($job['title']) ?></td>
							<td><?= ops_status_badge($job['status']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>

		<div class="ops-card">
			<div class="ops-card-header"><h2>Invoices</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<tbody>
					<?php if (empty($invoices)): ?><tr><td class="text-muted text-center py-3">None yet.</td></tr><?php endif; ?>
					<?php foreach ($invoices as $invoice): ?>
						<tr onclick="window.location='<?= site_url('invoices/'.$invoice['id']) ?>'">
							<td><?= ops_or_dash($invoice['number']) ?></td>
							<td><?= ops_money($invoice['total']) ?></td>
							<td><?= ops_status_badge($invoice['status']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
