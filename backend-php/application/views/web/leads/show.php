<div class="mb-3">
	<a href="<?= site_url('leads') ?>" class="text-decoration-none small"><i class="fa-solid fa-arrow-left"></i> Back to leads</a>
</div>

<div class="row g-3">
	<div class="col-lg-6">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2><?= html_escape($lead['name']) ?></h2>
				<?= ops_status_badge($lead['status']) ?>
			</div>
			<div class="ops-card-body">
				<div class="ops-detail-row"><div class="ops-detail-label">Phone</div><div class="ops-detail-value"><?= ops_or_dash($lead['phone']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Email</div><div class="ops-detail-value"><?= ops_or_dash($lead['email']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Source</div><div class="ops-detail-value"><?= ucfirst($lead['source']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Follow-up date</div><div class="ops-detail-value"><?= ops_date($lead['follow_up_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Enquiry</div><div class="ops-detail-value"><?= ops_or_dash($lead['enquiry']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Notes</div><div class="ops-detail-value"><?= ops_or_dash($lead['notes']) ?></div></div>
				<?php if ($converted_customer): ?>
				<div class="ops-detail-row"><div class="ops-detail-label">Converted to</div><div class="ops-detail-value">
					<a href="<?= site_url('customers/'.$converted_customer['id']) ?>"><?= html_escape($converted_customer['name']) ?></a>
				</div></div>
				<?php endif; ?>
			</div>
		</div>
	</div>
</div>
