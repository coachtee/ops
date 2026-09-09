<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'briefcase',
			'title' => $q !== '' || $status !== '' ? 'No jobs match that' : 'No jobs yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'A job is the work itself — numbered J-0001 onwards once it syncs from the phone.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Job</th><th>Customer</th><th>Start</th><th>Due</th><th>Status</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $job): ?>
						<tr class="clickable" data-href="<?= site_url('jobs/'.$job['id']) ?>">
							<td>
								<div class="cell-title truncate"><?= html_escape($job['title']) ?></div>
								<div class="cell-sub"><?= ops_or_dash($job['number']) ?></div>
							</td>
							<td class="truncate"><?= html_escape($job['customer_name']) ?></td>
							<td><?= ops_date($job['start_date']) ?></td>
							<td>
								<?= ops_date($job['due_date']) ?>
								<?php if (!empty($job['due_date']) && $job['due_date'] < date('Y-m-d') && !in_array($job['status'], array('completed', 'cancelled'), TRUE)): ?>
									<div class="t-body-sm delta-down"><?= html_escape(ops_relative_day($job['due_date'])) ?></div>
								<?php endif; ?>
							</td>
							<td><?= ops_status_badge($job['status']) ?></td>
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
