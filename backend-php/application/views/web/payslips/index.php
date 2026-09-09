<?php $this->load->view('web/partials/page_head', array(
	'title' => $title,
	'subtitle' => $subtitle,
	'actions' => '<div class="kv-inline"><span class="k">Net paid this month</span> <strong class="num">'.ops_money($month_net).'</strong> <span class="subtle">('.(int) $month_count.')</span></div>',
)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'banknote',
			'title' => $q !== '' ? 'No payslips match that' : 'No payslips yet',
			'text' => $q !== ''
				? 'Try a different search.'
				: 'Record what you paid each person, per period, on the phone. Net pay is worked out as gross minus deductions.',
			'action' => $q !== '' ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear search</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Employee</th><th>Period</th><th class="right">Gross</th><th class="right">Deductions</th><th class="right">Net pay</th><th>Paid</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $payslip): ?>
						<tr class="clickable" data-href="<?= site_url('employees/'.$payslip['employee_id']) ?>">
							<td>
								<div class="cell-primary">
									<span class="avatar avatar-sm avatar-neutral"><?= html_escape(ops_initials($payslip['employee_name'])) ?></span>
									<div class="cell-title truncate"><?= html_escape($payslip['employee_name']) ?></div>
								</div>
							</td>
							<td><?= ops_date($payslip['period_start']) ?> &ndash; <?= ops_date($payslip['period_end']) ?></td>
							<td class="right"><?= ops_money($payslip['gross_pay']) ?></td>
							<td class="right">
								<?= ops_money($payslip['deductions']) ?>
								<?php if (!empty($payslip['deductions_note'])): ?>
									<div class="cell-sub truncate"><?= html_escape($payslip['deductions_note']) ?></div>
								<?php endif; ?>
							</td>
							<td class="right strong"><?= ops_money($payslip['net_pay']) ?></td>
							<td><?= $payslip['paid_date'] ? ops_date($payslip['paid_date']) : '<span class="badge badge-warning">Unpaid</span>' ?></td>
						</tr>
					<?php endforeach; ?>
				</tbody>
			</table>
		</div>
	<?php endif; ?>

	<?php $this->load->view('web/partials/pager', array(
		'page' => $page, 'pages' => $pages, 'total' => $total, 'base_path' => $base_path,
	)); ?>
</div>

<div class="alert alert-info mt-3">
	<?= ops_icon('info') ?>
	<div>
		OPS records what you paid — it does not calculate PAYE, UIF or SDL, and does not submit anything to SARS.
		Gross pay and deductions are whatever you (or your bookkeeper) entered.
	</div>
</div>
