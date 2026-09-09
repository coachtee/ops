<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="stat-grid mb-3">
	<div class="stat accent-warning">
		<div class="stat-label"><?= ops_icon('wallet') ?> Spent this month</div>
		<div class="stat-value"><?= ops_money_compact($month_total) ?></div>
		<div class="stat-meta">VAT-inclusive, as actually paid</div>
	</div>
	<div class="stat accent-neutral">
		<div class="stat-label"><?= ops_icon('receipt') ?> Input VAT this month</div>
		<div class="stat-value"><?= ops_money_compact($month_vat) ?></div>
		<div class="stat-meta">Extracted from the total, never added on top</div>
	</div>
</div>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'wallet',
			'title' => $q !== '' || $status !== '' ? 'No expenses match that' : 'No expenses captured',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Snap a receipt on the phone and the expense — with its VAT split out — appears here.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Date</th><th>What</th><th>Category</th><th>Supplier</th><th class="right">VAT</th><th class="right">Amount</th><th>Receipt</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $expense): ?>
						<tr>
							<td><?= ops_date($expense['date']) ?></td>
							<td>
								<div class="cell-title truncate"><?= ops_or_dash($expense['description']) ?></div>
								<?php if (!empty($expense['job_title'])): ?>
									<div class="cell-sub truncate">Job: <?= html_escape($expense['job_title']) ?></div>
								<?php endif; ?>
							</td>
							<td><span class="badge badge-neutral"><?= html_escape($expense['category_label']) ?></span></td>
							<td class="truncate"><?= $expense['supplier_name'] ? html_escape($expense['supplier_name']) : '<span class="subtle">&mdash;</span>' ?></td>
							<td class="right"><?= (float) $expense['vat_amount'] > 0 ? ops_money($expense['vat_amount']) : '<span class="subtle">&mdash;</span>' ?></td>
							<td class="right strong"><?= ops_money($expense['amount']) ?></td>
							<td>
								<?php if (!empty($expense['receipt_image_url'])): ?>
									<a href="<?= html_escape($expense['receipt_image_url']) ?>" target="_blank" rel="noopener" class="chip"><?= ops_icon('eye') ?> View</a>
								<?php else: ?>
									<span class="subtle">&mdash;</span>
								<?php endif; ?>
							</td>
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
