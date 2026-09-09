<?php
$delta = function ($now, $before) {
	$now = (float) $now;
	$before = (float) $before;
	if ($before <= 0) {
		return $now > 0 ? array('dir' => 'up', 'text' => 'first activity this month') : NULL;
	}
	$pct = (($now - $before) / $before) * 100;
	if (abs($pct) < 0.5) {
		return array('dir' => 'flat', 'text' => 'level with last month');
	}
	return array(
		'dir' => $pct > 0 ? 'up' : 'down',
		'text' => ($pct > 0 ? '+' : '').number_format($pct, 0).'% vs last month',
	);
};
$rev_delta = $prev_month ? $delta($this_month['revenue'], $prev_month['revenue']) : NULL;
$exp_delta = $prev_month ? $delta($this_month['expenses'], $prev_month['expenses']) : NULL;
$profit = (float) $this_month['profit'];
$attention_items = array_filter(array(
	$attention['overdue_invoices'] ? array('icon' => 'alert-triangle', 'tone' => 'danger', 'label' => $attention['overdue_invoices'].' overdue invoice'.($attention['overdue_invoices'] === 1 ? '' : 's'), 'meta' => ops_money($attention['overdue_amount']).' past due', 'url' => 'invoices?status=sent') : NULL,
	$attention['leads_to_chase'] ? array('icon' => 'target', 'tone' => 'warning', 'label' => $attention['leads_to_chase'].' lead'.($attention['leads_to_chase'] === 1 ? '' : 's').' to chase', 'meta' => 'New or contacted, not quoted yet', 'url' => 'leads?status=new') : NULL,
	$attention['quotes_awaiting'] ? array('icon' => 'file-text', 'tone' => 'info', 'label' => $attention['quotes_awaiting'].' quote'.($attention['quotes_awaiting'] === 1 ? '' : 's').' awaiting an answer', 'meta' => 'Sent, no decision yet', 'url' => 'quotes?status=sent') : NULL,
	$attention['visits_follow_up'] ? array('icon' => 'calendar', 'tone' => 'warning', 'label' => $attention['visits_follow_up'].' visit'.($attention['visits_follow_up'] === 1 ? '' : 's').' need follow-up', 'meta' => 'Marked on site', 'url' => 'schedule?status=needs_follow_up') : NULL,
	$attention['compliance_overdue'] ? array('icon' => 'shield', 'tone' => 'danger', 'label' => $attention['compliance_overdue'].' compliance deadline'.($attention['compliance_overdue'] === 1 ? '' : 's').' passed', 'meta' => 'Your own reminder list', 'url' => 'compliance') : NULL,
));
// Every meta string above is either a literal we control or ops_money()
// output; escape the literals once, here, so the view can print meta raw.
foreach ($attention_items as &$_item) {
	if (strpos($_item['meta'], 'R&nbsp;') === FALSE) {
		$_item['meta'] = html_escape($_item['meta']);
	}
}
unset($_item);
?>

<?php $this->load->view('web/partials/page_head', array(
	'title' => 'Dashboard',
	'subtitle' => html_escape($business['name']).' &middot; '.date('F Y'),
	'actions' => '<a class="btn btn-outline" href="'.site_url('reports').'">'.ops_icon('bar-chart').' Reports</a>',
)); ?>

<?php if (!$has_any_data): ?>
	<div class="card">
		<div class="empty">
			<div class="empty-icon"><?= ops_icon('smartphone') ?></div>
			<h3>Nothing has synced yet</h3>
			<p>
				This panel is the read-side of your business — leads, quotes, jobs, invoices and
				expenses all arrive here from the OPS app on your phone, the moment it next syncs.
				Capture your first customer or expense on the phone and it'll show up here.
			</p>
			<a class="btn btn-primary" href="<?= site_url('settings') ?>"><?= ops_icon('building') ?> Check your business profile</a>
		</div>
	</div>
<?php endif; ?>

<div class="stat-grid mb-3">
	<div class="stat">
		<div class="stat-label"><?= ops_icon('trending-up') ?> Revenue this month</div>
		<div class="stat-value"><?= ops_money_compact($this_month['revenue']) ?></div>
		<?php if ($rev_delta): ?>
			<div class="stat-meta">
				<span class="delta-<?= $rev_delta['dir'] === 'down' ? 'down' : 'up' ?>"><?= html_escape($rev_delta['text']) ?></span>
			</div>
		<?php else: ?>
			<div class="stat-meta">Payments received, cash basis</div>
		<?php endif; ?>
	</div>

	<div class="stat accent-warning">
		<div class="stat-label"><?= ops_icon('wallet') ?> Expenses this month</div>
		<div class="stat-value"><?= ops_money_compact($this_month['expenses']) ?></div>
		<?php if ($exp_delta): ?>
			<div class="stat-meta">
				<span class="<?= $exp_delta['dir'] === 'up' ? 'delta-down' : 'delta-up' ?>"><?= html_escape($exp_delta['text']) ?></span>
			</div>
		<?php else: ?>
			<div class="stat-meta">VAT-inclusive, as paid</div>
		<?php endif; ?>
	</div>

	<div class="stat <?= $profit >= 0 ? 'accent-success' : 'accent-danger' ?>">
		<div class="stat-label"><?= ops_icon('bar-chart') ?> Profit this month</div>
		<div class="stat-value"><?= ops_money_compact($profit) ?></div>
		<div class="stat-meta">Revenue minus expenses</div>
	</div>

	<div class="stat <?= $attention['overdue_invoices'] ? 'accent-danger' : 'accent-neutral' ?>">
		<div class="stat-label"><?= ops_icon('receipt') ?> Outstanding</div>
		<div class="stat-value"><?= ops_money_compact($outstanding) ?></div>
		<div class="stat-meta">
			<?php if ($attention['overdue_invoices']): ?>
				<span class="delta-down"><?= (int) $attention['overdue_invoices'] ?> overdue</span>
			<?php else: ?>
				Nothing past its due date
			<?php endif; ?>
		</div>
	</div>
</div>

<div class="grid split-7-5 mb-3">
	<div class="card">
		<div class="card-head">
			<h2>Money in vs money out</h2>
			<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('reports') ?>">Full report <?= ops_icon('arrow-right') ?></a></div>
		</div>
		<div class="card-body">
			<?php $this->load->view('web/partials/chart_months', array('months' => $series)); ?>
		</div>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr><th>Month</th><th class="right">Revenue</th><th class="right">Expenses</th><th class="right">Profit</th></tr>
				</thead>
				<tbody>
					<?php foreach (array_reverse($series) as $m): ?>
						<tr>
							<td class="strong"><?= html_escape(ops_month_label($m['month'])) ?></td>
							<td class="right"><?= ops_money($m['revenue']) ?></td>
							<td class="right"><?= ops_money($m['expenses']) ?></td>
							<td class="right <?= (float) $m['profit'] < 0 ? 'delta-down' : '' ?>"><?= ops_money($m['profit']) ?></td>
						</tr>
					<?php endforeach; ?>
				</tbody>
			</table>
		</div>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Needs you</h2></div>
			<?php if (empty($attention_items)): ?>
				<div class="card-body">
					<div class="row" style="gap:10px;">
						<span class="badge badge-success badge-plain"><?= ops_icon('check') ?></span>
						<div>
							<div class="t-title-sm">Nothing outstanding</div>
							<div class="t-body-sm muted">No overdue invoices, unchased leads or missed deadlines.</div>
						</div>
					</div>
				</div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<tbody>
						<?php foreach ($attention_items as $item): ?>
							<tr class="clickable" data-href="<?= site_url($item['url']) ?>">
								<td style="width:36px;">
									<span class="badge badge-<?= $item['tone'] ?> badge-plain" style="padding:6px;"><?= ops_icon($item['icon']) ?></span>
								</td>
								<td>
									<div class="t-title-sm"><?= html_escape($item['label']) ?></div>
									<?php /* meta is pre-rendered markup (ops_money emits a non-breaking
									         space entity), so it is built escaped above, not here. */ ?>
									<div class="t-body-sm muted"><?= $item['meta'] ?></div>
								</td>
								<td class="right subtle" style="width:24px;"><?= ops_icon('chevron-right') ?></td>
							</tr>
						<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>

		<div class="card">
			<div class="card-head">
				<h2>Top spend this month</h2>
				<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('expenses') ?>">All expenses</a></div>
			</div>
			<div class="card-body">
				<?php if (empty($categories)): ?>
					<p class="muted t-body-sm mb-0">No expenses captured this month yet.</p>
				<?php else: ?>
					<?php $top = (float) $categories[0]['total']; ?>
					<div class="bar-list">
						<?php foreach ($categories as $cat): ?>
							<div>
								<div class="bar-list-head">
									<span><?= html_escape($category_labels[$cat['category']] ?? $cat['category']) ?></span>
									<span class="bar-list-amount"><?= ops_money($cat['total']) ?></span>
								</div>
								<div class="bar-track"><span style="width:<?= round(ops_percent($cat['total'], $top), 1) ?>%"></span></div>
							</div>
						<?php endforeach; ?>
					</div>
				<?php endif; ?>
			</div>
		</div>
	</div>
</div>

<div class="grid split-7-5">
	<div class="card">
		<div class="card-head">
			<h2>Recent invoices</h2>
			<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('invoices') ?>">View all <?= ops_icon('arrow-right') ?></a></div>
		</div>
		<?php if (empty($recent_invoices)): ?>
			<?php $this->load->view('web/partials/empty_state', array(
				'icon' => 'receipt', 'title' => 'No invoices yet',
				'text' => 'Invoices you raise on the phone appear here once they sync.',
			)); ?>
		<?php else: ?>
			<div class="table-wrap">
				<table class="table">
					<thead><tr><th>Number</th><th>Customer</th><th class="right">Total</th><th class="right">Outstanding</th><th>Status</th></tr></thead>
					<tbody>
						<?php foreach ($recent_invoices as $invoice): ?>
							<tr class="clickable" data-href="<?= site_url('invoices/'.$invoice['id']) ?>">
								<td class="strong"><?= ops_or_dash($invoice['number']) ?></td>
								<td class="truncate"><?= html_escape($invoice['customer_name']) ?></td>
								<td class="right"><?= ops_money($invoice['total']) ?></td>
								<td class="right"><?= ops_money((float) $invoice['total'] - (float) $invoice['amount_paid']) ?></td>
								<td><?= ops_status_badge($invoice['status']) ?></td>
							</tr>
						<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		<?php endif; ?>
	</div>

	<div>
		<div class="card">
			<div class="card-head">
				<h2>Next visits</h2>
				<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('schedule') ?>">Schedule</a></div>
			</div>
			<?php if (empty($upcoming_visits)): ?>
				<div class="card-body"><p class="muted t-body-sm mb-0">Nothing booked from today onwards.</p></div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<tbody>
						<?php foreach ($upcoming_visits as $visit): ?>
							<tr class="clickable" data-href="<?= site_url('jobs/'.$visit['job_id']) ?>">
								<td>
									<div class="t-title-sm truncate"><?= html_escape($visit['job_title']) ?></div>
									<div class="t-body-sm muted"><?= ops_date($visit['scheduled_date']) ?> &middot; <?= html_escape(ops_relative_day($visit['scheduled_date'])) ?></div>
								</td>
								<td class="right"><?= ops_status_badge($visit['status']) ?></td>
							</tr>
						<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>

		<div class="card">
			<div class="card-head">
				<h2>Latest leads</h2>
				<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('leads') ?>">All leads</a></div>
			</div>
			<?php if (empty($recent_leads)): ?>
				<div class="card-body"><p class="muted t-body-sm mb-0">No leads captured yet.</p></div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<tbody>
						<?php foreach ($recent_leads as $lead): ?>
							<tr class="clickable" data-href="<?= site_url('leads/'.$lead['id']) ?>">
								<td>
									<div class="cell-primary">
										<span class="avatar avatar-sm avatar-neutral"><?= html_escape(ops_initials($lead['name'])) ?></span>
										<div>
											<div class="cell-title truncate"><?= html_escape($lead['name']) ?></div>
											<div class="cell-sub"><?= ucfirst(str_replace('_', ' ', $lead['source'])) ?></div>
										</div>
									</div>
								</td>
								<td class="right"><?= ops_status_badge($lead['status']) ?></td>
							</tr>
						<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>
	</div>
</div>
