<?php
$outstanding = (float) $invoice['total'] - (float) $invoice['amount_paid'];
$paid_pct = ops_percent($invoice['amount_paid'], $invoice['total']);
// Paying more than the invoice total is legitimate (a rounded-up EFT, a
// deposit applied to the wrong invoice). Showing "outstanding -R1 437.50"
// reads as a bug; name it for what it is instead.
$overpaid = $outstanding < 0;
$balance_label = $overpaid ? 'Overpaid by' : 'Outstanding';
$balance_amount = abs($outstanding);
?>

<?php $this->load->view('web/partials/page_head', array(
	'title' => $invoice['number'] ?: 'Draft invoice',
	'subtitle' => 'Invoice for '.html_escape($customer['name'] ?? '—'),
	'actions' =>
		'<button type="button" class="btn btn-outline no-print" onclick="window.print()">'.ops_icon('printer').' Print</button>'
		.'<a class="btn btn-outline no-print" href="'.site_url('invoices').'">'.ops_icon('arrow-left').' All invoices</a>',
)); ?>

<div class="print-head mb-3">
	<div class="t-title-lg"><?= html_escape($business['name']) ?></div>
	<div class="muted"><?= html_escape(trim($business['phone'].' '.$business['email'])) ?></div>
	<?php if (!empty($business['vat_number'])): ?>
		<div class="muted">VAT no. <?= html_escape($business['vat_number']) ?></div>
	<?php endif; ?>
</div>

<div class="grid" style="grid-template-columns:minmax(0,8fr) minmax(0,4fr);">
	<div class="card">
		<div class="record-hero">
			<div>
				<div class="record-title"><?= html_escape($invoice['number'] ?: 'Draft invoice') ?></div>
				<div class="record-sub">
					Issued <?= ops_date($invoice['issue_date']) ?>
					<?php if (!empty($invoice['due_date'])): ?>
						&middot; due <?= ops_date($invoice['due_date']) ?> (<?= html_escape(ops_relative_day($invoice['due_date'])) ?>)
					<?php endif; ?>
				</div>
			</div>
			<div class="record-side">
				<div class="record-amount"><?= ops_money($invoice['total']) ?></div>
				<div class="mt-1"><?= ops_status_badge($invoice['status']) ?></div>
			</div>
		</div>

		<div class="table-wrap">
			<table class="table">
				<thead><tr><th>Description</th><th class="right">Qty</th><th class="right">Unit price</th><th class="right">Line total</th></tr></thead>
				<tbody>
					<?php if (empty($line_items)): ?>
						<tr><td colspan="4" class="muted" style="text-align:center;padding:26px;">No line items on this invoice.</td></tr>
					<?php endif; ?>
					<?php foreach ($line_items as $item): ?>
						<tr>
							<td><?= html_escape($item['description']) ?></td>
							<td class="right"><?= rtrim(rtrim($item['quantity'], '0'), '.') ?></td>
							<td class="right"><?= ops_money($item['unit_price']) ?></td>
							<td class="right strong"><?= ops_money($item['line_total']) ?></td>
						</tr>
					<?php endforeach; ?>
				</tbody>
				<tfoot>
					<tr><td colspan="3" class="right muted">Subtotal</td><td class="right"><?= ops_money($invoice['subtotal']) ?></td></tr>
					<?php if ((float) $invoice['discount_amount'] > 0): ?>
						<tr><td colspan="3" class="right muted">Discount</td><td class="right">&minus; <?= ops_money($invoice['discount_amount']) ?></td></tr>
					<?php endif; ?>
					<tr><td colspan="3" class="right muted">VAT <?= $invoice['is_vat_applicable'] ? '(15%)' : '(not applicable)' ?></td><td class="right"><?= ops_money($invoice['vat_amount']) ?></td></tr>
					<tr class="total"><td colspan="3" class="right">Total</td><td class="right"><?= ops_money($invoice['total']) ?></td></tr>
					<tr><td colspan="3" class="right muted">Paid</td><td class="right"><?= ops_money($invoice['amount_paid']) ?></td></tr>
					<tr class="total"><td colspan="3" class="right"><?= $balance_label ?></td><td class="right"><?= ops_money($balance_amount) ?></td></tr>
				</tfoot>
			</table>
		</div>

		<?php if (!empty($invoice['notes']) || !empty($invoice['terms'])): ?>
			<div class="card-foot">
				<?php if (!empty($invoice['notes'])): ?>
					<div class="t-overline mb-1">Notes</div>
					<p class="t-body mb-2" style="white-space:pre-wrap;"><?= html_escape($invoice['notes']) ?></p>
				<?php endif; ?>
				<?php if (!empty($invoice['terms'])): ?>
					<div class="t-overline mb-1">Terms</div>
					<p class="t-body-sm muted mb-0" style="white-space:pre-wrap;"><?= html_escape($invoice['terms']) ?></p>
				<?php endif; ?>
			</div>
		<?php endif; ?>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Payment</h2></div>
			<div class="card-body">
				<div class="row-between mb-1">
					<span class="t-body-sm muted"><?= round($paid_pct) ?>% paid</span>
					<span class="t-title-sm num"><?= ops_money($invoice['amount_paid']) ?></span>
				</div>
				<div class="progress <?= $paid_pct >= 100 ? 'success' : ($paid_pct > 0 ? 'warning' : '') ?>">
					<span style="width:<?= round($paid_pct, 1) ?>%"></span>
				</div>
				<div class="row-between mt-2">
					<span class="t-body-sm muted"><?= $overpaid ? 'Overpaid by' : 'Still outstanding' ?></span>
					<strong class="num<?= $overpaid ? ' delta-down' : '' ?>"><?= ops_money($balance_amount) ?></strong>
				</div>
				<?php if ($overpaid): ?>
					<div class="alert alert-warning mt-2">
						<?= ops_icon('alert-triangle') ?>
						<div>More has been received than this invoice is for — check whether a payment belongs against another invoice.</div>
					</div>
				<?php endif; ?>
			</div>

			<?php if (!empty($payments)): ?>
				<div class="table-wrap">
					<table class="table">
						<thead><tr><th>Date</th><th>Method</th><th class="right">Amount</th></tr></thead>
						<tbody>
							<?php foreach ($payments as $payment): ?>
								<tr>
									<td><?= ops_date($payment['paid_date']) ?></td>
									<td><?= html_escape(strtoupper($payment['method'])) ?></td>
									<td class="right strong"><?= ops_money($payment['amount']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			<?php else: ?>
				<div class="card-foot"><span class="t-body-sm muted">No payments recorded against this invoice.</span></div>
			<?php endif; ?>
		</div>

		<div class="card">
			<div class="card-head"><h2>Customer</h2></div>
			<div class="card-body">
				<?php if ($customer): ?>
					<div class="cell-primary">
						<span class="avatar"><?= html_escape(ops_initials($customer['name'])) ?></span>
						<div>
							<div class="cell-title"><a href="<?= site_url('customers/'.$customer['id']) ?>"><?= html_escape($customer['name']) ?></a></div>
							<div class="cell-sub"><?= html_escape($customer['phone']) ?></div>
						</div>
					</div>
				<?php else: ?>
					<p class="muted mb-0">Customer record not found.</p>
				<?php endif; ?>
				<?php if ($job): ?>
					<div class="mt-2 kv-inline">
						<span class="k">Job</span>
						<a href="<?= site_url('jobs/'.$job['id']) ?>"><?= html_escape($job['number'] ?: $job['title']) ?></a>
					</div>
				<?php endif; ?>
			</div>
		</div>
	</div>
</div>
