<?php $this->load->view('web/partials/page_head', array(
	'title' => $title,
	'subtitle' => $subtitle,
	'actions' => '<div class="chip-row">'
		.'<span class="chip">'.ops_icon('calendar').' '.(int) $upcoming_count.' upcoming</span>'
		.((int) $follow_up_count > 0 ? '<a class="chip" href="'.ops_query_url($base_path, array('status' => 'needs_follow_up', 'page' => NULL)).'">'.ops_icon('alert-triangle').' '.(int) $follow_up_count.' need follow-up</a>' : '')
		.'</div>',
)); ?>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'calendar',
			'title' => $q !== '' || $status !== '' ? 'No visits match that' : 'Nothing scheduled',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'A visit is one scheduled attendance against a job — book them on the phone and they appear here.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>When</th><th>Job</th><th>Assigned to</th><th>Photo</th><th>Status</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $visit): ?>
						<tr class="clickable" data-href="<?= site_url('jobs/'.$visit['job_id']) ?>">
							<td>
								<div class="cell-title"><?= ops_date($visit['scheduled_date']) ?></div>
								<div class="cell-sub">
									<?= html_escape(ops_relative_day($visit['scheduled_date'])) ?>
									<?php if (!empty($visit['start_time'])): ?>
										&middot; <?= html_escape(substr($visit['start_time'], 0, 5)) ?>
									<?php endif; ?>
								</div>
							</td>
							<td>
								<div class="cell-title truncate"><?= html_escape($visit['job_title']) ?></div>
								<?php if (!empty($visit['job_number'])): ?>
									<div class="cell-sub"><?= html_escape($visit['job_number']) ?></div>
								<?php endif; ?>
							</td>
							<td><?= $visit['employee_name'] ? html_escape($visit['employee_name']) : '<span class="subtle">Unassigned</span>' ?></td>
							<td>
								<?php if (!empty($visit['photo_url'])): ?>
									<a href="<?= html_escape($visit['photo_url']) ?>" target="_blank" rel="noopener" class="chip"><?= ops_icon('eye') ?> View</a>
								<?php else: ?>
									<span class="subtle">&mdash;</span>
								<?php endif; ?>
							</td>
							<td><?= ops_status_badge($visit['status']) ?></td>
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
