<?php $this->load->view('web/partials/page_head', array(
	'title' => $lead['name'],
	'subtitle' => 'Lead &middot; captured '.ops_date($lead['created_at']),
	'actions' => '<a class="btn btn-outline" href="'.site_url('leads').'">'.ops_icon('arrow-left').' All leads</a>',
)); ?>

<div class="grid split-5-7">
	<div>
		<div class="card">
			<div class="card-head">
				<h2>Lead details</h2>
				<div class="card-actions"><?= ops_status_badge($lead['status']) ?></div>
			</div>
			<div class="card-body">
				<dl class="dl">
					<dt>Phone</dt><dd><?= $lead['phone'] ? '<a href="tel:'.html_escape($lead['phone']).'">'.html_escape($lead['phone']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<dt>Email</dt><dd><?= $lead['email'] ? '<a href="mailto:'.html_escape($lead['email']).'">'.html_escape($lead['email']).'</a>' : '<span class="subtle">&mdash;</span>' ?></dd>
					<dt>Source</dt><dd><?= html_escape(ucfirst(str_replace('_', ' ', $lead['source']))) ?></dd>
					<dt>Follow-up date</dt>
					<dd>
						<?= ops_date($lead['follow_up_date']) ?>
						<?php if (!empty($lead['follow_up_date'])): ?>
							<span class="subtle">(<?= html_escape(ops_relative_day($lead['follow_up_date'])) ?>)</span>
						<?php endif; ?>
					</dd>
					<?php if ($converted_customer): ?>
						<dt>Converted to</dt>
						<dd><a href="<?= site_url('customers/'.$converted_customer['id']) ?>"><?= html_escape($converted_customer['name']) ?></a></dd>
					<?php endif; ?>
				</dl>
			</div>
		</div>

		<?php if (!empty($lead['enquiry']) || !empty($lead['notes'])): ?>
			<div class="card">
				<div class="card-head"><h2>What they asked for</h2></div>
				<div class="card-body">
					<?php if (!empty($lead['enquiry'])): ?>
						<p class="t-body-lg mt-0" style="white-space:pre-wrap;"><?= html_escape($lead['enquiry']) ?></p>
					<?php endif; ?>
					<?php if (!empty($lead['notes'])): ?>
						<div class="t-overline mt-3 mb-1">Notes</div>
						<p class="muted mb-0" style="white-space:pre-wrap;"><?= html_escape($lead['notes']) ?></p>
					<?php endif; ?>
				</div>
			</div>
		<?php endif; ?>
	</div>

	<div class="card">
		<div class="card-head">
			<h2>Quotes from this lead</h2>
			<div class="card-actions"><span class="result-count"><?= count($quotes) ?></span></div>
		</div>
		<?php if (empty($quotes)): ?>
			<div class="card-body"><p class="muted t-body-sm mb-0">No quote has been raised against this lead yet.</p></div>
		<?php else: ?>
			<div class="table-wrap">
				<table class="table">
					<thead><tr><th>Number</th><th>Issued</th><th class="right">Total</th><th>Status</th></tr></thead>
					<tbody>
						<?php foreach ($quotes as $quote): ?>
							<tr class="clickable" data-href="<?= site_url('quotes/'.$quote['id']) ?>">
								<td class="strong"><?= ops_or_dash($quote['number']) ?></td>
								<td><?= ops_date($quote['issue_date']) ?></td>
								<td class="right"><?= ops_money($quote['total']) ?></td>
								<td><?= ops_status_badge($quote['status']) ?></td>
							</tr>
						<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		<?php endif; ?>
	</div>
</div>
