<div class="mb-3">
	<a href="<?= site_url('quotes') ?>" class="text-decoration-none small"><i class="fa-solid fa-arrow-left"></i> Back to quotes</a>
</div>

<div class="row g-3">
	<div class="col-lg-4">
		<div class="ops-card">
			<div class="ops-card-header">
				<h2><?= ops_or_dash($quote['number']) ?></h2>
				<?= ops_status_badge($quote['status']) ?>
			</div>
			<div class="ops-card-body">
				<div class="ops-detail-row"><div class="ops-detail-label">Customer</div><div class="ops-detail-value"><a href="<?= site_url('customers/'.$quote['customer_id']) ?>"><?= html_escape($quote['customer_name']) ?></a></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Issue date</div><div class="ops-detail-value"><?= ops_date($quote['issue_date']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Valid until</div><div class="ops-detail-value"><?= ops_date($quote['valid_until']) ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">VAT applicable</div><div class="ops-detail-value"><?= $quote['is_vat_applicable'] ? 'Yes' : 'No' ?></div></div>
				<div class="ops-detail-row"><div class="ops-detail-label">Notes</div><div class="ops-detail-value"><?= ops_or_dash($quote['notes']) ?></div></div>
			</div>
		</div>
	</div>

	<div class="col-lg-8">
		<div class="ops-card">
			<div class="ops-card-header"><h2>Line items</h2></div>
			<div class="table-responsive">
				<table class="table ops-table mb-0">
					<thead><tr><th>Description</th><th>Qty</th><th>Unit price</th><th>Line total</th></tr></thead>
					<tbody>
					<?php if (empty($line_items)): ?><tr><td colspan="4" class="text-muted text-center py-3">No line items.</td></tr><?php endif; ?>
					<?php foreach ($line_items as $item): ?>
						<tr>
							<td><?= html_escape($item['description']) ?></td>
							<td><?= $item['quantity'] ?></td>
							<td><?= ops_money($item['unit_price']) ?></td>
							<td><?= ops_money($item['line_total']) ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
					<tfoot>
						<tr><td colspan="3" class="text-end text-muted">Subtotal</td><td><?= ops_money($quote['subtotal']) ?></td></tr>
						<tr><td colspan="3" class="text-end text-muted">Discount</td><td><?= ops_money($quote['discount_amount']) ?></td></tr>
						<tr><td colspan="3" class="text-end text-muted">VAT</td><td><?= ops_money($quote['vat_amount']) ?></td></tr>
						<tr class="fw-bold"><td colspan="3" class="text-end">Total</td><td><?= ops_money($quote['total']) ?></td></tr>
					</tfoot>
				</table>
			</div>
		</div>
	</div>
</div>
