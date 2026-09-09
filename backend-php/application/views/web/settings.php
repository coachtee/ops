<?php
$address = array_filter(array($business['address_line1'], $business['address_line2'], $business['suburb'], $business['city'], $business['province'], $business['postal_code']));
?>

<?php $this->load->view('web/partials/page_head', array(
	'title' => 'Settings',
	'subtitle' => 'Your business profile, and how the phone app connects to this server.',
)); ?>

<div class="grid split-7-5">
	<div>
		<div class="card">
			<div class="card-head">
				<h2>Business profile</h2>
				<div class="card-actions"><span class="badge badge-neutral">Edited in the app</span></div>
			</div>
			<div class="card-body">
				<div class="cell-primary mb-3">
					<?php if (!empty($business['logo_url'])): ?>
						<img src="<?= html_escape($business['logo_url']) ?>" alt="" width="48" height="48" style="border-radius:12px;object-fit:cover;">
					<?php else: ?>
						<span class="avatar avatar-lg"><?= html_escape(ops_initials($business['name'])) ?></span>
					<?php endif; ?>
					<div>
						<div class="t-title-md"><?= html_escape($business['name']) ?></div>
						<div class="t-body-sm muted"><?= ops_or_dash($business['trading_name'] ?: $business['industry']) ?></div>
					</div>
				</div>

				<dl class="dl">
					<dt>Trading name</dt><dd><?= ops_or_dash($business['trading_name']) ?></dd>
					<dt>Industry</dt><dd><?= ops_or_dash(ucfirst(str_replace('_', ' ', $business['industry']))) ?></dd>
					<dt>Registration no.</dt><dd><?= ops_or_dash($business['registration_number']) ?></dd>
					<dt>Tax no.</dt><dd><?= ops_or_dash($business['tax_number']) ?></dd>
					<dt>VAT no.</dt>
					<dd>
						<?= ops_or_dash($business['vat_number']) ?>
						<?php if ($business['is_vat_registered']): ?>
							<span class="badge badge-info">VAT registered</span>
						<?php endif; ?>
					</dd>
					<dt>Phone</dt><dd><?= ops_or_dash($business['phone']) ?></dd>
					<dt>Email</dt><dd><?= ops_or_dash($business['email']) ?></dd>
					<dt>Address</dt><dd><?= $address ? nl2br(html_escape(implode("\n", $address))) : '<span class="subtle">&mdash;</span>' ?></dd>
				</dl>
			</div>
		</div>

		<div class="card">
			<div class="card-head"><h2>What has synced</h2></div>
			<div class="table-wrap">
				<table class="table">
					<tbody>
						<?php foreach ($counts as $label => $count): ?>
							<tr>
								<td><?= html_escape($label) ?></td>
								<td class="right strong"><?= (int) $count ?></td>
							</tr>
						<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		</div>
	</div>

	<div>
		<div class="card">
			<div class="card-head"><h2>Connect the app</h2></div>
			<div class="card-body">
				<p class="t-body muted mt-0">
					On the phone, open <strong>Business Profile &rarr; Developer options &rarr; Connection
					Diagnostics</strong> and set the server URL to:
				</p>
				<div class="input" style="display:flex;align-items:center;font-family:var(--font-mono);font-size:13px;overflow-x:auto;">
					<?= html_escape($api_base) ?>
				</div>
				<p class="t-body-sm muted mt-2 mb-0">
					Then run "Test connection" — it calls <code>/api/health/</code> on this server. Once that
					passes, log in on the phone with the same email and password you use here.
				</p>
			</div>
		</div>

		<div class="card">
			<div class="card-head"><h2>Your account</h2></div>
			<div class="card-body">
				<dl class="dl">
					<dt>Name</dt><dd><?= ops_or_dash(trim($user['first_name'].' '.$user['last_name'])) ?></dd>
					<dt>Email</dt><dd><?= ops_or_dash($user['email']) ?></dd>
					<dt>Phone</dt><dd><?= ops_or_dash($user['phone']) ?></dd>
				</dl>
				<a class="btn btn-outline btn-block mt-3" href="<?= site_url('logout') ?>"><?= ops_icon('log-out') ?> Log out</a>
			</div>
		</div>

		<div class="card">
			<div class="card-head"><h2>How this panel works</h2></div>
			<div class="card-body">
				<ul class="stack" style="list-style:none;margin:0;padding:0;gap:12px;">
					<li class="row" style="align-items:flex-start;gap:10px;">
						<span class="subtle" style="flex-shrink:0;"><?= ops_icon('smartphone') ?></span>
						<span class="t-body-sm">The app on your phone is where records are created and edited — it works offline and syncs when it can.</span>
					</li>
					<li class="row" style="align-items:flex-start;gap:10px;">
						<span class="subtle" style="flex-shrink:0;"><?= ops_icon('eye') ?></span>
						<span class="t-body-sm">This panel is the read side: the big screen for looking at your money, your jobs and your deadlines.</span>
					</li>
					<li class="row" style="align-items:flex-start;gap:10px;">
						<span class="subtle" style="flex-shrink:0;"><?= ops_icon('lock') ?></span>
						<span class="t-body-sm">Only your business's records are ever visible here, scoped by the account you logged in with.</span>
					</li>
				</ul>
			</div>
		</div>
	</div>
</div>
