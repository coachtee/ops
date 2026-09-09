<div class="empty">
	<div class="empty-icon"><?= ops_icon($icon ?? 'inbox') ?></div>
	<h3><?= html_escape($title) ?></h3>
	<p><?= $text ?></p>
	<?php if (!empty($action)): ?><?= $action ?><?php endif; ?>
</div>
