<?php $margin = $billed - $spent; ?>

<?php $this->load->view('web/partials/page_head', array(
	'title' => $job['number'] ? $job['number'].' · '.$job['title'] : $job['title'],
	'subtitle' => 'Job for '.html_escape($customer['name'] ?? '—'),
	'actions' => '<a class="btn btn-outline" href="'.site_url('jobs').'">'.ops_icon('arrow-left').' All jobs</a>',
)); ?>

<div class="stat-grid mb-3">
	<div class="stat accent-neutral">
		<div class="stat-label"><?= ops_icon('briefcase') ?> Status</div>
		<div class="stat-value" style="font-size:18px;line-height:30px;"><?= ops_status_badge($job['status']) ?></div>
		<div class="stat-meta"><?= count($visits) ?> visit<?= count($visits) === 1 ? '' : 's' ?> scheduled</div>
	</div>
	<div class="stat">
		<div class="stat-label"><?= ops_icon('receipt') ?> Billed</div>
		<div class="stat-value"><?= ops_money_compact($billed) ?></div>
		<div class="stat-meta"><?= count($invoices) ?> invoice<?= count($invoices) === 1 ? '' : 's' ?></div>
	</div>
	<div class="stat accent-warning">
		<div class="stat-label"><?= ops_icon('wallet') ?> Spent on this job</div>
		<div class="stat-value"><?= ops_money_compact($spent) ?></div>
		<div class="stat-meta"><?= count($expenses) ?> expense<?= count($expenses) === 1 ? '' : 's' ?></div>
	</div>
	<div class="stat <?= $margin >= 0 ? 'accent-success' : 'accent-danger' ?>">
		<div class="stat-label"><?= ops_icon('bar-chart') ?> Margin so far</div>
		<div class="stat-value"><?= ops_money_compact($margin) ?></div>
		<div class="stat-meta">Billed minus job expenses</div>
	</div>
</div>

<div class="grid split-5-7">
	<div class="card">
		<div class="card-head"><h2>Job details</h2></div>
		<div class="card-body">
			<dl class="dl">
				<dt>Customer</dt>
				<dd><?= $customer ? '<a href="'.site_url('customers/'.$customer['id']).'">'.html_escape($customer['name']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
				<dt>Number</dt><dd><?= ops_or_dash($job['number']) ?></dd>
				<dt>Start date</dt><dd><?= ops_date($job['start_date']) ?></dd>
				<dt>Due date</dt><dd><?= ops_date($job['due_date']) ?></dd>
				<dt>Completed</dt><dd><?= ops_date($job['completed_date']) ?></dd>
			</dl>
			<?php if (!empty($job['description'])): ?>
				<div class="t-overline mt-3 mb-1">Description</div>
				<p class="t-body mb-0" style="white-space:pre-wrap;"><?= html_escape($job['description']) ?></p>
			<?php endif; ?>
		</div>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Visits</h2><div class="card-actions result-count"><?= count($visits) ?></div></div>
			<?php if (empty($visits)): ?>
				<div class="card-body"><p class="muted t-body-sm mb-0">No site visits scheduled against this job.</p></div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<thead><tr><th>Date</th><th>Time</th><th>Assigned</th><th>Status</th></tr></thead>
						<tbody>
							<?php foreach ($visits as $visit): ?>
								<tr>
									<td>
										<?= ops_date($visit['scheduled_date']) ?>
										<div class="cell-sub"><?= html_escape(ops_relative_day($visit['scheduled_date'])) ?></div>
									</td>
									<td><?= $visit['start_time'] ? html_escape(substr($visit['start_time'], 0, 5)) : '<span class="subtle">&mdash;</span>' ?></td>
									<td><?= $visit['employee_name'] ? html_escape($visit['employee_name']) : '<span class="subtle">Unassigned</span>' ?></td>
									<td><?= ops_status_badge($visit['status']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>

		<div class="grid grid-2">
			<div class="card">
				<div class="card-head"><h2>Expenses</h2></div>
				<?php if (empty($expenses)): ?>
					<div class="card-body"><p class="muted t-body-sm mb-0">Nothing charged to this job.</p></div>
				<?php else: ?>
					<div class="table-wrap">
						<table class="table">
							<tbody>
								<?php foreach (array_slice($expenses, 0, 8) as $expense): ?>
									<tr>
										<td>
											<div class="cell-title truncate"><?= ops_or_dash($expense['description']) ?></div>
											<div class="cell-sub"><?= ops_date($expense['date']) ?></div>
										</td>
										<td class="right strong"><?= ops_money($expense['amount']) ?></td>
									</tr>
								<?php endforeach; ?>
							</tbody>
						</table>
					</div>
				<?php endif; ?>
			</div>

			<div class="card">
				<div class="card-head"><h2>Invoices</h2></div>
				<?php if (empty($invoices)): ?>
					<div class="card-body"><p class="muted t-body-sm mb-0">Not invoiced yet.</p></div>
				<?php else: ?>
					<div class="table-wrap">
						<table class="table">
							<tbody>
								<?php foreach ($invoices as $invoice): ?>
									<tr class="clickable" data-href="<?= site_url('invoices/'.$invoice['id']) ?>">
										<td class="strong"><?= ops_or_dash($invoice['number']) ?></td>
										<td class="right"><?= ops_money($invoice['total']) ?></td>
										<td class="right"><?= ops_status_badge($invoice['status']) ?></td>
									</tr>
								<?php endforeach; ?>
							</tbody>
						</table>
					</div>
				<?php endif; ?>
			</div>
		</div>
	</div>
</div>
