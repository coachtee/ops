<?php $this->load->view('web/partials/page_head', array(
	'title' => 'Search',
	'subtitle' => $q !== ''
		? (int) $total.' result'.((int) $total === 1 ? '' : 's').' for &ldquo;'.html_escape($q).'&rdquo;'
		: 'Find a customer, quote, invoice, job, supplier or employee.',
)); ?>

<?php if ($q === ''): ?>
	<div class="card">
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'search',
			'title' => 'What are you looking for?',
			'text' => 'Search by name, phone number, email, or a document number like INV-0004.',
		)); ?>
	</div>
<?php elseif (empty($groups)): ?>
	<div class="card">
		<?php $this->load->view('web/partials/empty_state', array(
			'icon' => 'search',
			'title' => 'Nothing found',
			'text' => 'No customer, lead, quote, invoice, job, supplier or employee matches &ldquo;'.html_escape($q).'&rdquo;.',
			'action' => '<a class="btn btn-outline" href="'.site_url('dashboard').'">Back to dashboard</a>',
		)); ?>
	</div>
<?php else: ?>
	<div class="grid grid-2">
		<?php foreach ($groups as $group): ?>
			<div class="card">
				<div class="card-head">
					<h2><?= ops_icon($group['icon']) ?> <?= html_escape($group['label']) ?></h2>
					<div class="card-actions result-count"><?= count($group['rows']) ?></div>
				</div>
				<div class="table-wrap">
					<table class="table">
						<tbody>
							<?php foreach ($group['rows'] as $row): ?>
								<tr class="clickable" data-href="<?= site_url($group['path'].'/'.$row['id']) ?>">
									<td>
										<div class="cell-title truncate"><?= html_escape($row['title']) ?></div>
										<?php if ($row['meta'] !== ''): ?>
											<div class="cell-sub"><?= html_escape(ucfirst(str_replace('_', ' ', $row['meta']))) ?></div>
										<?php endif; ?>
									</td>
									<td class="right subtle" style="width:24px;"><?= ops_icon('chevron-right') ?></td>
								</tr>
							<?php endforeach; ?>
						</tbody>
					</table>
				</div>
			</div>
		<?php endforeach; ?>
	</div>
<?php endif; ?>
