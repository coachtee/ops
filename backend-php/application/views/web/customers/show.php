<?php
$address = array_filter(array($customer['address_line1'], $customer['address_line2'], $customer['suburb'], $customer['city'], $customer['province'], $customer['postal_code']));
?>

<?php $this->load->view('web/partials/page_head', array(
	'title' => $customer['name'],
	'subtitle' => ($customer['customer_type'] === 'company' ? 'Company' : 'Individual').' &middot; customer since '.ops_date($customer['created_at']),
	'actions' => '<a class="btn btn-outline" href="'.site_url('customers').'">'.ops_icon('arrow-left').' All customers</a>',
)); ?>

<div class="stat-grid mb-3">
	<div class="stat">
		<div class="stat-label"><?= ops_icon('receipt') ?> Invoiced (lifetime)</div>
		<div class="stat-value"><?= ops_money_compact($invoiced) ?></div>
		<div class="stat-meta">Excludes drafts and cancelled</div>
	</div>
	<div class="stat <?= $outstanding > 0 ? 'accent-warning' : 'accent-success' ?>">
		<div class="stat-label"><?= ops_icon('clock') ?> Outstanding</div>
		<div class="stat-value"><?= ops_money_compact($outstanding) ?></div>
		<div class="stat-meta"><?= $outstanding > 0 ? 'Still owed to you' : 'All settled' ?></div>
	</div>
	<div class="stat accent-neutral">
		<div class="stat-label"><?= ops_icon('briefcase') ?> Jobs</div>
		<div class="stat-value"><?= count($jobs) ?></div>
		<div class="stat-meta"><?= count($quotes) ?> quote<?= count($quotes) === 1 ? '' : 's' ?> raised</div>
	</div>
</div>

<div class="grid split-4-8">
	<div class="card">
		<div class="card-head"><h2>Contact</h2></div>
		<div class="card-body">
			<dl class="dl">
				<dt>Phone</dt><dd><?= $customer['phone'] ? '<a href="tel:'.html_escape($customer['phone']).'">'.html_escape($customer['phone']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
				<dt>Email</dt><dd><?= $customer['email'] ? '<a href="mailto:'.html_escape($customer['email']).'">'.html_escape($customer['email']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
				<dt>Address</dt><dd><?= $address ? nl2br(html_escape(implode("\n", $address))) : '<span class="subtle">&mdash;</span>' ?></dd>
				<?php if (!empty($customer['notes'])): ?>
					<dt>Notes</dt><dd class="muted" style="font-weight:400;"><?= nl2br(html_escape($customer['notes'])) ?></dd>
				<?php endif; ?>
			</dl>
		</div>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Invoices</h2><div class="card-actions result-count"><?= count($invoices) ?></div></div>
			<?php if (empty($invoices)): ?>
				<div class="card-body"><p class="muted t-body-sm mb-0">Nothing invoiced to this customer yet.</p></div>
			<?php else: ?>
				<div class="table-wrap">
					<table class="table">
						<thead><tr><th>Number</th><th>Issued</th><th class="right">Total</th><th class="right">Paid</th><th>Status</th></tr></thead>
						<tbody>
							<?php foreach ($invoices as $invoice): ?>
								<tr class="clickable" data-href="<?= site_url('invoices/'.$invoice['id']) ?>">
									<td class="strong"><?= ops_or_dash($invoice['number']) ?></td>
									<td><?= ops_date($invoice['issue_date']) ?></td>
									<td class="right"><?= ops_money($invoice['total']) ?></td>
									<td class="right"><?= ops_money($invoice['amount_paid']) ?></td>
									<td><?= ops_status_badge($invoice['status']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php endif; ?>
		</div>

		<div class="grid grid-2">
			<div class="card">
				<div class="card-head"><h2>Quotes</h2></div>
				<?php if (empty($quotes)): ?>
					<div class="card-body"><p class="muted t-body-sm mb-0">None yet.</p></div>
				<?php else: ?>
					<div class="table-wrap">
						<table class="table">
							<tbody>
								<?php foreach ($quotes as $quote): ?>
									<tr class="clickable" data-href="<?= site_url('quotes/'.$quote['id']) ?>">
										<td class="strong"><?= ops_or_dash($quote['number']) ?></td>
										<td class="right"><?= ops_money($quote['total']) ?></td>
										<td class="right"><?= ops_status_badge($quote['status']) ?></td>
									</tr>
								<?php endforeach; ?>
							</tbody>
						</table>
					</div>
				<?php endif; ?>
			</div>

			<div class="card">
				<div class="card-head"><h2>Jobs</h2></div>
				<?php if (empty($jobs)): ?>
					<div class="card-body"><p class="muted t-body-sm mb-0">None yet.</p></div>
				<?php else: ?>
					<div class="table-wrap">
						<table class="table">
							<tbody>
								<?php foreach ($jobs as $job): ?>
									<tr class="clickable" data-href="<?= site_url('jobs/'.$job['id']) ?>">
										<td>
											<div class="cell-title truncate"><?= html_escape($job['title']) ?></div>
											<div class="cell-sub"><?= ops_or_dash($job['number']) ?></div>
										</td>
										<td class="right"><?= ops_status_badge($job['status']) ?></td>
									</tr>
								<?php endforeach; ?>
							</tbody>
						</table>
					</div>
				<?php endif; ?>
			</div>
		</div>

		<?php if (!empty($payments)): ?>
			<div class="card">
				<div class="card-head"><h2>Payments received</h2></div>
				<div class="table-wrap">
					<table class="table">
						<thead><tr><th>Date</th><th>Method</th><th>Reference</th><th class="right">Amount</th></tr></thead>
						<tbody>
							<?php foreach (array_slice($payments, 0, 10) as $payment): ?>
								<tr>
									<td><?= ops_date($payment['paid_date']) ?></td>
									<td><?= html_escape(strtoupper($payment['method'])) ?></td>
									<td><?= ops_or_dash($payment['reference']) ?></td>
									<td class="right strong"><?= ops_money($payment['amount']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			</div>
		<?php endif; ?>
	</div>
</div>
