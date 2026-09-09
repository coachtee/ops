<?php $this->load->view('web/partials/page_head', array(
	'title' => $title,
	'subtitle' => $subtitle,
	'actions' => '<div class="kv-inline"><span class="k">Received this month</span> <strong class="num">'.ops_money($month_total).'</strong></div>',
)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'credit-card',
			'title' => $q !== '' || $status !== '' ? 'No payments match that' : 'No payments recorded',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Every payment you record on the phone lands here, and it is what the profit figures count as revenue.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Date</th><th>Customer</th><th>Invoice</th><th>Method</th><th>Reference</th><th class="right">Amount</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $payment): ?>
						<tr<?= $payment['invoice_id'] ? ' class="clickable" data-href="'.site_url('invoices/'.$payment['invoice_id']).'"' : '' ?>>
							<td><?= ops_date($payment['paid_date']) ?></td>
							<td class="truncate"><?= html_escape($payment['customer_name']) ?></td>
							<td>
								<?php if ($payment['invoice_number']): ?>
									<span class="strong"><?= html_escape($payment['invoice_number']) ?></span>
								<?php else: ?>
									<span class="badge badge-neutral">On account</span>
								<?php endif; ?>
							</td>
							<td><span class="badge badge-neutral badge-plain"><?= html_escape(strtoupper($payment['method'])) ?></span></td>
							<td><?= ops_or_dash($payment['reference']) ?></td>
							<td class="right strong"><?= ops_money($payment['amount']) ?></td>
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
