<div class="filter-bar">
	<form class="search" method="get" action="<?= site_url($base_path) ?>">
		<?= ops_icon('search') ?>
		<input type="search" name="q" value="<?= html_escape($q ?? '') ?>"
		       placeholder="<?= html_escape($search_placeholder ?? 'Search…') ?>" aria-label="Search this list">
		<?php foreach (($carry ?? array()) as $key => $value): ?>
			<input type="hidden" name="<?= html_escape($key) ?>" value="<?= html_escape($value) ?>">
		<?php endforeach; ?>
	</form>

	<?php if (!empty($status_options)): ?>
		<div class="chip-row">
			<a class="chip<?= ($status ?? '') === '' ? ' active' : '' ?>"
			   href="<?= ops_query_url($base_path, array('status' => NULL, 'page' => NULL)) ?>">All</a>
			<?php foreach ($status_options as $value => $label): ?>
				<a class="chip<?= ($status ?? '') === $value ? ' active' : '' ?>"
				   href="<?= ops_query_url($base_path, array('status' => $value, 'page' => NULL)) ?>"><?= html_escape($label) ?></a>
			<?php endforeach; ?>
		</div>
	<?php endif; ?>

	<div class="spacer"></div>
	<div class="result-count"><?= (int) $total ?> record<?= (int) $total === 1 ? '' : 's' ?></div>
</div>
