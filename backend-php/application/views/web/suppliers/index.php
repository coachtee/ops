<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'truck',
			'title' => $q !== '' ? 'No suppliers match that' : 'No suppliers yet',
			'text' => $q !== ''
				? 'Try a different search.'
				: 'Add the merchants and subcontractors you buy from, and their expenses group up here.',
			'action' => $q !== '' ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear search</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Supplier</th><th>Contact</th><th>Phone</th><th class="right">Expenses</th><th class="right">Total spent</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $supplier): ?>
						<tr class="clickable" data-href="<?= site_url('suppliers/'.$supplier['id']) ?>">
							<td>
								<div class="cell-primary">
									<span class="avatar avatar-sm avatar-neutral"><?= html_escape(ops_initials($supplier['name'])) ?></span>
									<div class="cell-title"><?= html_escape($supplier['name']) ?></div>
								</div>
							</td>
							<td><?= ops_or_dash($supplier['contact_person']) ?></td>
							<td><?= ops_or_dash($supplier['phone']) ?></td>
							<td class="right"><?= (int) $supplier['expense_count'] ?></td>
							<td class="right strong"><?= ops_money($supplier['spent']) ?></td>
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
