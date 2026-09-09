<?php $this->load->view('web/partials/page_head', array(
	'title' => 'Reports',
	'subtitle' => 'Cash-basis: revenue is money actually received, not invoices raised.',
	'actions' => '<a class="btn btn-outline" href="'.site_url('reports/export?months='.$months).'">'.ops_icon('download').' Export CSV</a>',
)); ?>

<div class="stat-grid mb-3">
	<div class="stat">
		<div class="stat-label"><?= ops_icon('trending-up') ?> Revenue (<?= (int) $months ?> months)</div>
		<div class="stat-value"><?= ops_money_compact($totals['revenue']) ?></div>
		<div class="stat-meta">Payments received</div>
	</div>
	<div class="stat accent-warning">
		<div class="stat-label"><?= ops_icon('wallet') ?> Expenses (<?= (int) $months ?> months)</div>
		<div class="stat-value"><?= ops_money_compact($totals['expenses']) ?></div>
		<div class="stat-meta">VAT-inclusive</div>
	</div>
	<div class="stat <?= $totals['profit'] >= 0 ? 'accent-success' : 'accent-danger' ?>">
		<div class="stat-label"><?= ops_icon('bar-chart') ?> Profit (<?= (int) $months ?> months)</div>
		<div class="stat-value"><?= ops_money_compact($totals['profit']) ?></div>
		<div class="stat-meta">Revenue minus expenses</div>
	</div>
</div>

<div class="card mb-3">
	<div class="card-head">
		<h2>Money in vs money out</h2>
		<div class="card-actions chip-row">
			<?php foreach (array(3, 6, 12, 24) as $option): ?>
				<a class="chip<?= (int) $months === $option ? ' active' : '' ?>"
				   href="<?= ops_query_url('reports', array('months' => $option)) ?>"><?= $option ?>m</a>
			<?php endforeach; ?>
		</div>
	</div>
	<div class="card-body">
		<?php $this->load->view('web/partials/chart_months', array('months' => $series)); ?>
	</div>
	<div class="table-wrap">
		<table class="table">
			<thead><tr><th>Month</th><th class="right">Revenue</th><th class="right">Expenses</th><th class="right">Profit</th></tr></thead>
			<tbody>
				<?php foreach (array_reverse($series) as $month): ?>
					<tr>
						<td class="strong"><?= html_escape(ops_month_label($month['month'])) ?></td>
						<td class="right"><?= ops_money($month['revenue']) ?></td>
						<td class="right"><?= ops_money($month['expenses']) ?></td>
						<td class="right <?= (float) $month['profit'] < 0 ? 'delta-down' : '' ?>"><?= ops_money($month['profit']) ?></td>
					</tr>
				<?php endforeach; ?>
			</tbody>
			<tfoot>
				<tr class="total">
					<td>Total</td>
					<td class="right"><?= ops_money($totals['revenue']) ?></td>
					<td class="right"><?= ops_money($totals['expenses']) ?></td>
					<td class="right"><?= ops_money($totals['profit']) ?></td>
				</tr>
			</tfoot>
		</table>
	</div>
</div>

<div class="grid split-7-5">
	<div class="card">
		<div class="card-head">
			<h2>Where the money went</h2>
			<div class="card-actions chip-row">
				<a class="chip<?= $period === 'this_month' ? ' active' : '' ?>" href="<?= ops_query_url('reports', array('period' => 'this_month')) ?>">This month</a>
				<a class="chip<?= $period === 'all_time' ? ' active' : '' ?>" href="<?= ops_query_url('reports', array('period' => 'all_time')) ?>">All time</a>
			</div>
		</div>
		<div class="card-body">
			<?php if (empty($categories)): ?>
				<p class="muted mb-0">No expenses in this period.</p>
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

	<div class="card">
		<div class="card-head"><h2>VAT position</h2></div>
		<div class="card-body">
			<form method="get" action="<?= site_url('reports') ?>" class="form-grid mb-2">
				<input type="hidden" name="months" value="<?= (int) $months ?>">
				<input type="hidden" name="period" value="<?= html_escape($period) ?>">
				<div class="field">
					<label class="field-label" for="since">From</label>
					<input class="input" type="date" id="since" name="since" value="<?= html_escape($since) ?>">
				</div>
				<div class="field">
					<label class="field-label" for="until">To</label>
					<input class="input" type="date" id="until" name="until" value="<?= html_escape($until) ?>">
				</div>
				<div class="span-2"><button class="btn btn-outline btn-block" type="submit">Recalculate</button></div>
			</form>

			<dl class="dl">
				<dt>VAT collected</dt><dd><?= ops_money($vat['vat_collected']) ?></dd>
				<dt>VAT paid</dt><dd><?= ops_money($vat['vat_paid']) ?></dd>
				<dt>Net position</dt><dd class="t-title-md"><?= ops_money($vat['net_vat_position']) ?></dd>
			</dl>

			<div class="alert alert-info mt-3">
				<?= ops_icon('info') ?>
				<div>
					For your own VAT201 prep with your accountant. Invoices still in draft or cancelled are
					excluded. OPS does not compute or submit a SARS liability.
				</div>
			</div>
		</div>
	</div>
</div>
