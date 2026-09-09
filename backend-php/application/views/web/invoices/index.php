<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="stat-grid mb-3">
	<div class="stat <?= (float) $sum_outstanding > 0 ? 'accent-warning' : 'accent-success' ?>">
		<div class="stat-label"><?= ops_icon('clock') ?> Outstanding</div>
		<div class="stat-value"><?= ops_money_compact($sum_outstanding) ?></div>
		<div class="stat-meta">Across all unpaid invoices</div>
	</div>
	<div class="stat <?= (int) $overdue_count > 0 ? 'accent-danger' : 'accent-neutral' ?>">
		<div class="stat-label"><?= ops_icon('alert-triangle') ?> Past due date</div>
		<div class="stat-value"><?= ops_money_compact($overdue_amount) ?></div>
		<div class="stat-meta"><?= (int) $overdue_count ?> invoice<?= (int) $overdue_count === 1 ? '' : 's' ?></div>
	</div>
	<div class="stat accent-neutral">
		<div class="stat-label"><?= ops_icon('receipt') ?> Billed (lifetime)</div>
		<div class="stat-value"><?= ops_money_compact($sum_billed) ?></div>
		<div class="stat-meta">Excludes drafts and cancelled</div>
	</div>
</div>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'receipt',
			'title' => $q !== '' || $status !== '' ? 'No invoices match that' : 'No invoices yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Invoices are numbered INV-0001 onwards the first time they sync from the phone.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr><th>Number</th><th>Customer</th><th>Issued</th><th>Due</th><th class="right">Total</th><th class="right">Outstanding</th><th>Status</th></tr>
				</thead>
				<tbody>
					<?php foreach ($rows as $invoice): ?>
						<?php $is_overdue = !empty($invoice['due_date']) && $invoice['due_date'] < date('Y-m-d')
							&& !in_array($invoice['status'], array('paid', 'draft', 'cancelled'), TRUE); ?>
						<tr class="clickable" data-href="<?= site_url('invoices/'.$invoice['id']) ?>">
							<td class="strong"><?= ops_or_dash($invoice['number']) ?></td>
							<td class="truncate"><?= html_escape($invoice['customer_name']) ?></td>
							<td><?= ops_date($invoice['issue_date']) ?></td>
							<td>
								<?= ops_date($invoice['due_date']) ?>
								<?php if ($is_overdue): ?>
									<div class="t-body-sm delta-down"><?= html_escape(ops_relative_day($invoice['due_date'])) ?></div>
								<?php endif; ?>
							</td>
							<td class="right"><?= ops_money($invoice['total']) ?></td>
							<td class="right">
								<?php if ($invoice['outstanding'] > 0): ?>
									<strong><?= ops_money($invoice['outstanding']) ?></strong>
								<?php else: ?>
									<span class="subtle">&mdash;</span>
								<?php endif; ?>
							</td>
							<td><?= ops_status_badge($is_overdue && $invoice['status'] !== 'overdue' ? 'overdue' : $invoice['status']) ?></td>
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
