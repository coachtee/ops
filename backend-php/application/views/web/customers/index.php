<div class="ops-card">
	<div class="ops-card-header">
		<h2>Customers</h2>
		<span class="text-muted small"><?= count($customers) ?> total</span>
	</div>
	<div class="table-responsive">
		<table class="table ops-table mb-0">
			<thead><tr><th>Name</th><th>Type</th><th>Phone</th><th>Email</th><th>City</th></tr></thead>
			<tbody>
			<?php if (empty($customers)): ?>
				<tr><td colspan="5" class="text-muted text-center py-4">No customers yet — these sync in from the Android app.</td></tr>
			<?php endif; ?>
			<?php foreach ($customers as $customer): ?>
				<tr onclick="window.location='<?= site_url('customers/'.$customer['id']) ?>'">
					<td class="fw-semibold"><?= html_escape($customer['name']) ?></td>
					<td><?= ucfirst($customer['customer_type']) ?></td>
					<td><?= ops_or_dash($customer['phone']) ?></td>
					<td><?= ops_or_dash($customer['email']) ?></td>
					<td><?= ops_or_dash($customer['city']) ?></td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table>
	</div>
</div>
