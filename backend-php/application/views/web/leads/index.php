<div class="ops-card">
	<div class="ops-card-header">
		<h2>Leads</h2>
		<span class="text-muted small"><?= count($leads) ?> total</span>
	</div>
	<div class="table-responsive">
		<table class="table ops-table mb-0">
			<thead><tr><th>Name</th><th>Phone</th><th>Source</th><th>Status</th><th>Follow-up</th></tr></thead>
			<tbody>
			<?php if (empty($leads)): ?>
				<tr><td colspan="5" class="text-muted text-center py-4">No leads yet — these sync in from the Android app.</td></tr>
			<?php endif; ?>
			<?php foreach ($leads as $lead): ?>
				<tr onclick="window.location='<?= site_url('leads/'.$lead['id']) ?>'">
					<td class="fw-semibold"><?= html_escape($lead['name']) ?></td>
					<td><?= ops_or_dash($lead['phone']) ?></td>
					<td><?= ucfirst($lead['source']) ?></td>
					<td><?= ops_status_badge($lead['status']) ?></td>
					<td><?= ops_date($lead['follow_up_date']) ?></td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table>
	</div>
</div>
