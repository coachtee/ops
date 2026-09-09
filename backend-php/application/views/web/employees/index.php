<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'badge',
			'title' => $q !== '' || $status !== '' ? 'No employees match that' : 'No employees yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Add your staff on the phone to assign them to visits and record their payslips.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Name</th><th>Role</th><th>Phone</th><th>Started</th><th class="right">Agreed rate</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $employee): ?>
						<tr class="clickable" data-href="<?= site_url('employees/'.$employee['id']) ?>">
							<td>
								<div class="cell-primary">
									<span class="avatar avatar-sm avatar-neutral"><?= html_escape(ops_initials($employee['name'])) ?></span>
									<div>
										<div class="cell-title"><?= html_escape($employee['name']) ?></div>
										<?php if (!empty($employee['email'])): ?>
											<div class="cell-sub truncate"><?= html_escape($employee['email']) ?></div>
										<?php endif; ?>
									</div>
								</div>
							</td>
							<td><?= ops_or_dash($employee['role']) ?></td>
							<td><?= ops_or_dash($employee['phone']) ?></td>
							<td><?= ops_date($employee['start_date']) ?></td>
							<td class="right">
								<strong><?= ops_money($employee['pay_rate']) ?></strong>
								<div class="cell-sub"><?= html_escape($employee['pay_rate_type']) ?></div>
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
