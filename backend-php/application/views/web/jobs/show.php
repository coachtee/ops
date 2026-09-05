<div class="mb-3">
	<a href="<?= site_url('jobs') ?>" class="text-decoration-none small"><i class="fa-solid fa-arrow-left"></i> Back to jobs</a>
</div>

<div class="row g-3">
	<div class="col-lg-5">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2><?= ops_or_dash($job['number']) ?> — <?= html_escape($job['title']) ?></h2>
				<?= ops_status_badge($job['status']) ?>
			</div>
			<div class="ops-card-body">
				<div class="ops-detail-row"><div class="ops-detail-label">Customer</div><div class="ops-detail-value"><a href="<?= site_url('customers/'.$job['customer_id']) ?>"><?= html_escape($job['customer_name']) ?></a></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Start date</div><div class="ops-detail-value"><?= ops_date($job['start_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Due date</div><div class="ops-detail-value"><?= ops_date($job['due_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Completed</div><div class="ops-detail-value"><?= ops_date($job['completed_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Description</div><div class="ops-detail-value"><?= ops_or_dash($job['description']) ?></div></div>
			</div>
		</div>
	</div>

	<div class="col-lg-7">
		<div class="ops-card">
			<div class="ops-card-header"><h2>Visits</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Scheduled</th><th>Status</th><th>Notes</th></tr></thead>
					<tbody>
					<?php if (empty($visits)): ?><tr><td colspan="3" class="text-muted text-center py-3">No visits scheduled.</td></tr><?php endif; ?>
					<?php foreach ($visits as $visit): ?>
						<tr>
							<td><?= ops_date($visit['scheduled_date']) ?></td>
							<td><?= ops_status_badge($visit['status']) ?></td>
							<td><?= ops_or_dash($visit['notes']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
