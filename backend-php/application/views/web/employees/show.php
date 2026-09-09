<?php $this->load->view('web/partials/page_head', array(
	'title' => $employee['name'],
	'subtitle' => $employee['role'] ? html_escape($employee['role']) : 'Employee',
	'actions' => '<a class="btn btn-outline" href="'.site_url('employees').'">'.ops_icon('arrow-left').' All employees</a>',
)); ?>

<div class="grid split-4-8">
	<div>
		<div class="stat mb-3">
			<div class="stat-label"><?= ops_icon('banknote') ?> Paid to date</div>
			<div class="stat-value"><?= ops_money_compact($paid_total) ?></div>
			<div class="stat-meta">Net pay across <?= count($payslips) ?> payslip<?= count($payslips) === 1 ? '' : 's' ?></div>
		</div>

		<div class="card">
			<div class="card-head"><h2>Details</h2></div>
			<div class="card-body">
				<dl class="dl">
					<dt>Role</dt><dd><?= ops_or_dash($employee['role']) ?></dd>
					<dt>Phone</dt><dd><?= $employee['phone'] ? '<a href="tel:'.html_escape($employee['phone']).'">'.html_escape($employee['phone']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<dt>Email</dt><dd><?= $employee['email'] ? '<a href="mailto:'.html_escape($employee['email']).'">'.html_escape($employee['email']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<dt>Started</dt><dd><?= ops_date($employee['start_date']) ?></dd>
					<dt>Agreed rate</dt><dd><?= ops_money($employee['pay_rate']) ?> <span class="muted" style="font-weight:400;">(<?= html_escape($employee['pay_rate_type']) ?>)</span></dd>
				</dl>
				<div class="alert alert-info mt-3">
					<?= ops_icon('info') ?>
					<div>The agreed rate is a reminder of what was arranged — it is never used to calculate a payslip automatically.</div>
				</div>
			</div>
		</div>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Payslips</h2><div class="card-actions result-count"><?= count($payslips) ?></div></div>
			<?php if (empty($payslips)): ?>
				<div class="card-body"><p class="muted t-body-sm mb-0">No payslips recorded for this person yet.</p></div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<thead><tr><th>Period</th><th class="right">Gross</th><th class="right">Deductions</th><th class="right">Net</th><th>Paid</th></tr></thead>
						<tbody>
							<?php foreach ($payslips as $payslip): ?>
								<tr>
									<td><?= ops_date($payslip['period_start']) ?> &ndash; <?= ops_date($payslip['period_end']) ?></td>
									<td class="right"><?= ops_money($payslip['gross_pay']) ?></td>
									<td class="right"><?= ops_money($payslip['deductions']) ?></td>
									<td class="right strong"><?= ops_money($payslip['net_pay']) ?></td>
									<td><?= $payslip['paid_date'] ? ops_date($payslip['paid_date']) : '<span class="badge badge-warning">Unpaid</span>' ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>

		<?php if (!empty($visits)): ?>
			<div class="card">
				<div class="card-head"><h2>Recent visits</h2></div>
				<div class="table-wrap">
					<table class="table">
						<tbody>
							<?php foreach ($visits as $visit): ?>
								<tr class="clickable" data-href="<?= site_url('jobs/'.$visit['job_id']) ?>">
									<td>
										<div class="cell-title truncate"><?= html_escape($visit['job_title']) ?></div>
										<div class="cell-sub"><?= ops_date($visit['scheduled_date']) ?></div>
									</td>
									<td class="right"><?= ops_status_badge($visit['status']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			</div>
		<?php endif; ?>
	</div>
</div>
