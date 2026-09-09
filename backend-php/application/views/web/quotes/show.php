<?php $this->load->view('web/partials/page_head', array(
	'title' => $quote['number'] ?: 'Draft quote',
	'subtitle' => 'Quote for '.html_escape($quote['customer_name']),
	'actions' =>
		'<button type="button" class="btn btn-outline no-print" onclick="window.print()">'.ops_icon('printer').' Print</button>'
		.'<a class="btn btn-outline no-print" href="'.site_url('quotes').'">'.ops_icon('arrow-left').' All quotes</a>',
)); ?>

<div class="print-head mb-3">
	<div class="t-title-lg"><?= html_escape($business['name']) ?></div>
	<div class="muted"><?= html_escape(trim($business['phone'].' '.$business['email'])) ?></div>
</div>

<div class="grid split-8-4">
	<div class="card">
		<div class="record-hero">
			<div>
				<div class="record-title"><?= html_escape($quote['number'] ?: 'Draft quote') ?></div>
				<div class="record-sub">
					Issued <?= ops_date($quote['issue_date']) ?>
					<?php if (!empty($quote['valid_until'])): ?>
						&middot; valid until <?= ops_date($quote['valid_until']) ?>
					<?php endif; ?>
				</div>
			</div>
			<div class="record-side">
				<div class="record-amount"><?= ops_money($quote['total']) ?></div>
				<div class="mt-1"><?= ops_status_badge($quote['status']) ?></div>
			</div>
		</div>

		<div class="table-wrap">
			<table class="table doc-table">
				<thead><tr><th>Description</th><th class="right">Qty</th><th class="right">Unit price</th><th class="right">Line total</th></tr></thead>
				<tbody>
					<?php if (empty($line_items)): ?>
						<tr><td colspan="4" class="muted" style="text-align:center;padding:26px;">No line items on this quote.</td></tr>
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
					<tr><td colspan="3" class="right muted">Subtotal</td><td class="right"><?= ops_money($quote['subtotal']) ?></td></tr>
					<?php if ((float) $quote['discount_amount'] > 0): ?>
						<tr><td colspan="3" class="right muted">Discount</td><td class="right">&minus; <?= ops_money($quote['discount_amount']) ?></td></tr>
					<?php endif; ?>
					<tr><td colspan="3" class="right muted">VAT <?= $quote['is_vat_applicable'] ? '(15%)' : '(not applicable)' ?></td><td class="right"><?= ops_money($quote['vat_amount']) ?></td></tr>
					<tr class="total"><td colspan="3" class="right">Total</td><td class="right"><?= ops_money($quote['total']) ?></td></tr>
				</tfoot>
			</table>
		</div>
		<?php $this->load->view('web/partials/doc_totals', array('rows' => array_values(array_filter(array(
			array('label' => 'Subtotal', 'amount' => ops_money($quote['subtotal'])),
			(float) $quote['discount_amount'] > 0
				? array('label' => 'Discount', 'amount' => '&minus; '.ops_money($quote['discount_amount'])) : NULL,
			array('label' => 'VAT '.($quote['is_vat_applicable'] ? '(15%)' : '(not applicable)'), 'amount' => ops_money($quote['vat_amount'])),
			array('label' => 'Total', 'amount' => ops_money($quote['total']), 'strong' => TRUE),
		))))); ?>

		<?php if (!empty($quote['notes']) || !empty($quote['terms'])): ?>
			<div class="card-foot">
				<?php if (!empty($quote['notes'])): ?>
					<div class="t-overline mb-1">Notes</div>
					<p class="t-body mb-2" style="white-space:pre-wrap;"><?= html_escape($quote['notes']) ?></p>
				<?php endif; ?>
				<?php if (!empty($quote['terms'])): ?>
					<div class="t-overline mb-1">Terms</div>
					<p class="t-body-sm muted mb-0" style="white-space:pre-wrap;"><?= html_escape($quote['terms']) ?></p>
				<?php endif; ?>
			</div>
		<?php endif; ?>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Customer</h2></div>
			<div class="card-body">
				<?php if ($customer): ?>
					<div class="cell-primary mb-2">
						<span class="avatar"><?= html_escape(ops_initials($customer['name'])) ?></span>
						<div>
							<div class="cell-title"><a href="<?= site_url('customers/'.$customer['id']) ?>"><?= html_escape($customer['name']) ?></a></div>
							<div class="cell-sub"><?= html_escape($customer['phone']) ?></div>
						</div>
					</div>
					<?php if (!empty($customer['email'])): ?>
						<div class="kv-inline"><span class="k">Email</span> <?= html_escape($customer['email']) ?></div>
					<?php endif; ?>
				<?php else: ?>
					<p class="muted mb-0">Customer record not found.</p>
				<?php endif; ?>
			</div>
		</div>

		<div class="card no-print">
			<div class="card-head"><h2>Progress</h2></div>
			<div class="card-body">
				<div class="timeline">
					<div class="timeline-item done">
						<div class="timeline-title">Quote created</div>
						<div class="timeline-meta"><?= ops_date($quote['created_at']) ?></div>
					</div>
					<div class="timeline-item <?= $quote['sent_at'] ? 'done' : '' ?>">
						<div class="timeline-title">Sent to customer</div>
						<div class="timeline-meta"><?= $quote['sent_at'] ? ops_datetime($quote['sent_at']) : 'Not sent yet' ?></div>
					</div>
					<div class="timeline-item <?= $quote['accepted_at'] ? 'done' : ($quote['declined_at'] ? 'active' : '') ?>">
						<div class="timeline-title"><?= $quote['declined_at'] ? 'Declined' : 'Accepted' ?></div>
						<div class="timeline-meta">
							<?php if ($quote['accepted_at']): ?><?= ops_datetime($quote['accepted_at']) ?>
							<?php elseif ($quote['declined_at']): ?><?= ops_datetime($quote['declined_at']) ?>
							<?php else: ?>Awaiting a decision<?php endif; ?>
						</div>
					</div>
					<div class="timeline-item <?= !empty($invoices) ? 'done' : '' ?>">
						<div class="timeline-title">Invoiced</div>
						<div class="timeline-meta"><?= !empty($invoices) ? count($invoices).' invoice(s) raised' : 'Not invoiced yet' ?></div>
					</div>
				</div>
			</div>
		</div>

		<?php if (!empty($jobs) || !empty($invoices)): ?>
			<div class="card no-print">
				<div class="card-head"><h2>Turned into</h2></div>
				<div class="table-wrap">
					<table class="table">
						<tbody>
							<?php foreach ($jobs as $job): ?>
								<tr class="clickable" data-href="<?= site_url('jobs/'.$job['id']) ?>">
									<td><?= ops_icon('briefcase') ?> <?= html_escape($job['number'] ?: $job['title']) ?></td>
									<td class="right"><?= ops_status_badge($job['status']) ?></td>
								</tr>
							<?php endforeach; ?>
							<?php foreach ($invoices as $invoice): ?>
								<tr class="clickable" data-href="<?= site_url('invoices/'.$invoice['id']) ?>">
									<td><?= ops_icon('receipt') ?> <?= html_escape($invoice['number'] ?: 'Draft invoice') ?></td>
									<td class="right"><?= ops_status_badge($invoice['status']) ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			</div>
		<?php endif; ?>
	</div>
</div>
