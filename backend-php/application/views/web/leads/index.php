<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="stat-grid mb-3">
	<?php
	$tones = array('new' => '', 'contacted' => '', 'quoted' => 'accent-warning', 'converted' => 'accent-success', 'lost' => 'accent-neutral');
	foreach ($status_options as $value => $label):
	?>
		<a class="stat <?= $tones[$value] ?>" href="<?= ops_query_url($base_path, array('status' => $value, 'page' => NULL, 'q' => NULL)) ?>" style="text-decoration:none;color:inherit;">
			<div class="stat-label"><?= html_escape($label) ?></div>
			<div class="stat-value"><?= (int) ($pipeline[$value] ?? 0) ?></div>
		</a>
	<?php endforeach; ?>
</div>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'target',
			'title' => $q !== '' || $status !== '' ? 'No leads match that' : 'No leads yet',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Leads captured in the app — a WhatsApp enquiry, a phone call, a walk-in — land here.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr><th>Name</th><th>Source</th><th>Phone</th><th>Follow-up</th><th>Status</th></tr>
				</thead>
				<tbody>
					<?php foreach ($rows as $lead): ?>
						<tr class="clickable" data-href="<?= site_url('leads/'.$lead['id']) ?>">
							<td>
								<div class="cell-primary">
									<span class="avatar avatar-sm avatar-neutral"><?= html_escape(ops_initials($lead['name'])) ?></span>
									<div>
										<div class="cell-title"><?= html_escape($lead['name']) ?></div>
										<?php if (!empty($lead['email'])): ?>
											<div class="cell-sub truncate"><?= html_escape($lead['email']) ?></div>
										<?php endif; ?>
									</div>
								</div>
							</td>
							<td><?= html_escape(ucfirst(str_replace('_', ' ', $lead['source']))) ?></td>
							<td><?= ops_or_dash($lead['phone']) ?></td>
							<td>
								<?php if (!empty($lead['follow_up_date'])): ?>
									<?= ops_date($lead['follow_up_date']) ?>
									<div class="t-body-sm subtle"><?= html_escape(ops_relative_day($lead['follow_up_date'])) ?></div>
								<?php else: ?>
									<span class="subtle">&mdash;</span>
								<?php endif; ?>
							</td>
							<td><?= ops_status_badge($lead['status']) ?></td>
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
