<div class="ops-card">
	<div class="ops-card-header">
		<h2>Jobs</h2>
		<span class="text-muted small"><?= count($jobs) ?> total</span>
	</div>
	<div class="table-responsive">
		<table class="table ops-table mb-0">
			<thead><tr><th>Number</th><th>Title</th><th>Customer</th><th>Due date</th><th>Status</th></tr></thead>
			<tbody>
			<?php if (empty($jobs)): ?>
				<tr><td colspan="5" class="text-muted text-center py-4">No jobs yet — these sync in from the Android app.</td></tr>
			<?php endif; ?>
			<?php foreach ($jobs as $job): ?>
				<tr onclick="window.location='<?= site_url('jobs/'.$job['id']) ?>'">
					<td class="fw-semibold"><?= ops_or_dash($job['number']) ?></td>
					<td><?= html_escape($job['title']) ?></td>
					<td><?= html_escape($job['customer_name']) ?></td>
					<td><?= ops_date($job['due_date']) ?></td>
					<td><?= ops_status_badge($job['status']) ?></td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table>
	</div>
</div>
