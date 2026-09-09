<div class="page-head">
	<div>
		<h1 class="page-title"><?= html_escape($title) ?></h1>
		<?php if (!empty($subtitle)): ?>
			<div class="page-sub"><?= $subtitle ?></div>
		<?php endif; ?>
	</div>
	<?php if (!empty($actions)): ?>
		<div class="actions"><?= $actions ?></div>
	<?php endif; ?>
</div>
