<?php
$state_meta = array(
	'overdue' => array('tone' => 'danger', 'label' => 'Overdue'),
	'due_soon' => array('tone' => 'warning', 'label' => 'Due soon'),
	'upcoming' => array('tone' => 'neutral', 'label' => 'Upcoming'),
	'done' => array('tone' => 'success', 'label' => 'Done'),
);
?>

<?php $this->load->view('web/partials/page_head', array('title' => $title, 'subtitle' => $subtitle)); ?>

<div class="stat-grid mb-3">
	<div class="stat <?= (int) $overdue_count > 0 ? 'accent-danger' : 'accent-success' ?>">
		<div class="stat-label"><?= ops_icon('alert-triangle') ?> Overdue</div>
		<div class="stat-value"><?= (int) $overdue_count ?></div>
		<div class="stat-meta">Past the date you set</div>
	</div>
	<div class="stat <?= (int) $due_soon_count > 0 ? 'accent-warning' : 'accent-neutral' ?>">
		<div class="stat-label"><?= ops_icon('clock') ?> Due in 30 days</div>
		<div class="stat-value"><?= (int) $due_soon_count ?></div>
		<div class="stat-meta">Still open</div>
	</div>
</div>

<div class="card">
	<?php $this->load->view('web/partials/list_toolbar', array(
		'base_path' => $base_path, 'q' => $q, 'status' => $status, 'status_options' => $status_options,
		'search_placeholder' => $search_placeholder, 'total' => $total, 'carry' => $carry,
	)); ?>

	<?php if (empty($rows)): ?>
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'shield',
			'title' => $q !== '' || $status !== '' ? 'Nothing matches that' : 'No deadlines tracked',
			'text' => $q !== '' || $status !== ''
				? 'Try a different search or clear the filter.'
				: 'Add the SARS and CIPC dates you need to hit — VAT returns, PAYE, provisional tax, annual returns — and they show up here in date order.',
			'action' => ($q !== '' || $status !== '') ? '<a class="btn btn-outline" href="'.site_url($base_path).'">Clear filters</a>' : '',
		)); ?>
	<?php else: ?>
		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>What</th><th>Category</th><th>Due</th><th>Completed</th><th>Status</th></tr></thead>
				<tbody>
					<?php foreach ($rows as $item): ?>
						<?php $meta = $state_meta[$item['state']]; ?>
						<tr>
							<td>
								<div class="cell-title"><?= html_escape($item['title']) ?></div>
								<?php if (!empty($item['notes'])): ?>
									<div class="cell-sub truncate"><?= html_escape($item['notes']) ?></div>
								<?php endif; ?>
							</td>
							<td><span class="badge badge-neutral"><?= html_escape($item['category_label']) ?></span></td>
							<td>
								<?= ops_date($item['due_date']) ?>
								<?php if ($item['state'] !== 'done'): ?>
									<div class="cell-sub <?= $item['state'] === 'overdue' ? 'delta-down' : '' ?>"><?= html_escape(ops_relative_day($item['due_date'])) ?></div>
								<?php endif; ?>
							</td>
							<td><?= ops_date($item['completed_date']) ?></td>
							<td><span class="badge badge-<?= $meta['tone'] ?>"><?= $meta['label'] ?></span></td>
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

<div class="alert alert-warning mt-3">
	<?= ops_icon('alert-triangle') ?>
	<div>
		This is your own reminder list. OPS does not file or submit anything to SARS or CIPC, and does not
		know your real filing status with them — an item is only ever marked done because you ticked it off.
	</div>
</div>
