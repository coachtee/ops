<?php $this->load->view('web/partials/page_head', array(
	'title' => $supplier['name'],
	'subtitle' => 'Supplier',
	'actions' => '<a class="btn btn-outline" href="'.site_url('suppliers').'">'.ops_icon('arrow-left').' All suppliers</a>',
)); ?>

<div class="grid split-4-8">
	<div>
		<div class="stat mb-3 accent-warning">
			<div class="stat-label"><?= ops_icon('wallet') ?> Total bought from them</div>
			<div class="stat-value"><?= ops_money_compact($spent) ?></div>
			<div class="stat-meta">All expenses linked to this supplier</div>
		</div>

		<div class="card">
			<div class="card-head"><h2>Contact</h2></div>
			<div class="card-body">
				<dl class="dl">
					<dt>Contact person</dt><dd><?= ops_or_dash($supplier['contact_person']) ?></dd>
					<dt>Phone</dt><dd><?= $supplier['phone'] ? '<a href="tel:'.html_escape($supplier['phone']).'">'.html_escape($supplier['phone']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<dt>Email</dt><dd><?= $supplier['email'] ? '<a href="mailto:'.html_escape($supplier['email']).'">'.html_escape($supplier['email']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<?php if (!empty($supplier['notes'])): ?>
						<dt>Notes</dt><dd class="muted" style="font-weight:400;"><?= nl2br(html_escape($supplier['notes'])) ?></dd>
					<?php endif; ?>
				</dl>
			</div>
		</div>
	</div>

	<div class="card">
		<div class="card-head">
			<h2>Expenses from this supplier</h2>
			<div class="card-actions"><a class="btn btn-ghost btn-sm" href="<?= site_url('expenses') ?>">All expenses</a></div>
		</div>
		<?php if (empty($expenses)): ?>
			<div class="card-body"><p class="muted t-body-sm mb-0">Nothing has been bought from this supplier yet.</p></div>
		<?php else: ?>
			<div class="table-wrap">
				<table class="table">
					<thead><tr><th>Date</th><th>What</th><th class="right">VAT</th><th class="right">Amount</th></tr></thead>
					<tbody>
						<?php foreach ($expenses as $expense): ?>
							<tr>
								<td><?= ops_date($expense['date']) ?></td>
								<td class="truncate"><?= ops_or_dash($expense['description']) ?></td>
								<td class="right"><?= (float) $expense['vat_amount'] > 0 ? ops_money($expense['vat_amount']) : '<span class="subtle">&mdash;</span>' ?></td>
								<td class="right strong"><?= ops_money($expense['amount']) ?></td>
							</tr>
						<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		<?php endif; ?>
	</div>
</div>
