<?php
/**
 * The subtotal/VAT/total block for a quote or an invoice, as a list rather
 * than a table foot.
 *
 * A tfoot's labels carry colspan="3" so they sit under the desktop columns,
 * and colspan can't be changed from CSS — on a phone that either pushes the
 * amounts off the right edge or, if the rows are made flex, traps them inside
 * the first column. So the narrow layout gets its own markup and the table
 * foot is hidden; both are printed from the same variables, so they can't
 * disagree.
 *
 * Expects: $rows — list of ['label' => string, 'amount' => string (already
 * formatted), 'strong' => bool].
 */
?>
<dl class="doc-totals">
	<?php foreach ($rows as $row): ?>
		<div<?= !empty($row['strong']) ? ' class="is-total"' : '' ?>>
			<dt><?= $row['label'] ?></dt>
			<dd><?= $row['amount'] ?></dd>
		</div>
	<?php endforeach; ?>
</dl>
