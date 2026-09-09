<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'users',
			'title' => $q !== '' || $status !== '' ? 'No customers match that' : 'No customers yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Customers you add on the phone — or convert from a lead — appear here.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr><th>Name</th><th>Contact</th><th>Where</th><th class="right">Invoiced</th><th class="right">Outstanding</th></tr>
				</thead>
				<tbody>
					<?php foreach ($rows as $customer): ?>
						<tr class="clickable" data-href="<?= site_url('customers/'.$customer['id']) ?>">
							<td>
								<div class="cell-primary">
									<span class="avatar avatar-sm"><?= html_escape(ops_initials($customer['name'])) ?></span>
									<div>
										<div class="cell-title"><?= html_escape($customer['name']) ?></div>
										<div class="cell-sub"><?= $customer['customer_type'] === 'company' ? 'Company' : 'Individual' ?></div>
									</div>
								</div>
							</td>
							<td>
								<?= ops_or_dash($customer['phone']) ?>
								<?php if (!empty($customer['email'])): ?>
									<div class="cell-sub truncate"><?= html_escape($customer['email']) ?></div>
								<?php endif; ?>
							</td>
							<td><?= ops_or_dash(trim($customer['suburb'].($customer['suburb'] && $customer['city'] ? ', ' : '').$customer['city'])) ?></td>
							<td class="right"><?= ops_money($customer['invoiced']) ?></td>
							<td class="right">
								<?php if ((float) $customer['outstanding'] > 0): ?>
									<span class="delta-down"><?= ops_money($customer['outstanding']) ?></span>
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
