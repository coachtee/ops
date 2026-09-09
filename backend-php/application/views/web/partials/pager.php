<?php if (($pages ?? 1) > 1): ?>
<div class="card-foot row-between">
	<div class="result-count">
		Page <?= (int) $page ?> of <?= (int) $pages ?>
		&middot; <?= (int) $total ?> record<?= $total === 1 ? '' : 's' ?>
	</div>
	<div class="pager">
		<?php
		$window = 2;
		$from = max(1, $page - $window);
		$to = min($pages, $page + $window);
		?>
		<a class="<?= $page <= 1 ? 'disabled' : '' ?>" href="<?= ops_query_url($base_path, array('page' => $page - 1 > 1 ? $page - 1 : NULL)) ?>" aria-label="Previous page"><?= ops_icon('chevron-left') ?></a>

		<?php if ($from > 1): ?>
			<a href="<?= ops_query_url($base_path, array('page' => NULL)) ?>">1</a>
			<?php if ($from > 2): ?><span class="gap">…</span><?php endif; ?>
		<?php endif; ?>

		<?php for ($p = $from; $p <= $to; $p++): ?>
			<?php if ($p === (int) $page): ?>
				<span class="current" aria-current="page"><?= $p ?></span>
			<?php else: ?>
				<a href="<?= ops_query_url($base_path, array('page' => $p > 1 ? $p : NULL)) ?>"><?= $p ?></a>
			<?php endif; ?>
		<?php endfor; ?>

		<?php if ($to < $pages): ?>
			<?php if ($to < $pages - 1): ?><span class="gap">…</span><?php endif; ?>
			<a href="<?= ops_query_url($base_path, array('page' => $pages)) ?>"><?= (int) $pages ?></a>
		<?php endif; ?>

		<a class="<?= $page >= $pages ? 'disabled' : '' ?>" href="<?= ops_query_url($base_path, array('page' => $page + 1)) ?>" aria-label="Next page"><?= ops_icon('chevron-right') ?></a>
	</div>
</div>
<?php endif; ?>
