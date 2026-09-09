<?php $this->load->view('web/partials/page_head', array(
	'title' => $title,
	'subtitle' => $subtitle,
	'actions' => '<div class="kv-inline"><span class="k">Open quote value</span> <strong class="num">'.ops_money($open_value).'</strong> <span class="subtle">('.(int) $open_count.')</span></div>',
)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'file-text',
			'title' => $q !== '' || $status !== '' ? 'No quotes match that' : 'No quotes yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Quotes get their number (Q-0001, Q-0002…) the first time they sync from the phone.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr><th>Number</th><th>Customer</th><th>Issued</th><th>Valid until</th><th class="right">Total</th><th>Status</th></tr>
				</thead>
				<tbody>
					<?php foreach ($rows as $quote): ?>
						<tr class="clickable" data-href="<?= site_url('quotes/'.$quote['id']) ?>">
							<td class="strong"><?= ops_or_dash($quote['number']) ?></td>
							<td class="truncate"><?= html_escape($quote['customer_name']) ?></td>
							<td><?= ops_date($quote['issue_date']) ?></td>
							<td><?= ops_date($quote['valid_until']) ?></td>
							<td class="right strong"><?= ops_money($quote['total']) ?></td>
							<td><?= ops_status_badge($quote['status']) ?></td>
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
