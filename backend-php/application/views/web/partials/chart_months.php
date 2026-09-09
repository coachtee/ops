<?php
/**
 * Grouped bar chart: revenue vs expenses per calendar month.
 *
 * Both series are the same unit (rand) on ONE shared axis — never a second
 * y-scale. Series colours come from the validated --chart-revenue /
 * --chart-expense tokens (see app.css), assigned in fixed order, and each
 * bar carries an SVG <title> so hovering gives the exact figure. The
 * caller renders the same numbers as a table underneath, which doubles as
 * the accessible alternative to the plot.
 *
 * Expects: $months — list of ['month' => 'YYYY-MM', 'revenue', 'expenses'].
 */
$w = 720;
$h = 210;
$pad_l = 52;
$pad_r = 10;
$pad_t = 18;
$pad_b = 28;
$plot_w = $w - $pad_l - $pad_r;
$plot_h = $h - $pad_t - $pad_b;

$peak = 0.0;
foreach ($months as $m)
{
	$peak = max($peak, (float) $m['revenue'], (float) $m['expenses']);
}
$max = ops_nice_max($peak);

$count = max(1, count($months));
$group_w = $plot_w / $count;
$bar_gap = 2;                                  // surface gap between adjacent fills
$bar_w = min(26, ($group_w * 0.62 - $bar_gap) / 2);
$last = $count - 1;

$y_for = function ($value) use ($pad_t, $plot_h, $max) {
	return $pad_t + $plot_h - ($plot_h * (min((float) $value, $max) / $max));
};
?>
<svg class="chart" viewBox="0 0 <?= $w ?> <?= $h ?>" role="img"
     aria-label="Revenue and expenses per month for the last <?= $count ?> months">

	<?php foreach (array(0, 0.5, 1) as $tick): ?>
		<?php $ty = $pad_t + $plot_h - ($plot_h * $tick); ?>
		<line class="<?= $tick === 0 ? 'baseline' : 'grid-line' ?>"
		      x1="<?= $pad_l ?>" y1="<?= round($ty, 1) ?>" x2="<?= $w - $pad_r ?>" y2="<?= round($ty, 1) ?>"/>
		<text class="axis-label" x="<?= $pad_l - 8 ?>" y="<?= round($ty + 3.5, 1) ?>" text-anchor="end">
			<?= $tick === 0 ? '0' : number_format($max * $tick) ?>
		</text>
	<?php endforeach; ?>

	<?php foreach ($months as $i => $m): ?>
		<?php
		$group_x = $pad_l + ($i * $group_w);
		$centre = $group_x + ($group_w / 2);
		$rev_x = $centre - $bar_w - ($bar_gap / 2);
		$exp_x = $centre + ($bar_gap / 2);
		$rev_y = $y_for($m['revenue']);
		$exp_y = $y_for($m['expenses']);
		$base_y = $pad_t + $plot_h;
		?>

		<?php $d = ops_bar_path($rev_x, $rev_y, $bar_w, $base_y - $rev_y); ?>
		<?php if ($d !== ''): ?>
			<path class="bar bar-revenue" d="<?= $d ?>"><title><?= html_escape(ops_month_label($m['month'])) ?> revenue: R <?= number_format((float) $m['revenue'], 2) ?></title></path>
		<?php endif; ?>

		<?php $d = ops_bar_path($exp_x, $exp_y, $bar_w, $base_y - $exp_y); ?>
		<?php if ($d !== ''): ?>
			<path class="bar bar-expense" d="<?= $d ?>"><title><?= html_escape(ops_month_label($m['month'])) ?> expenses: R <?= number_format((float) $m['expenses'], 2) ?></title></path>
		<?php endif; ?>

		<?php /* Selective direct labels: only the latest month, the one the
		         reader is actually deciding on. A number over all 12 bars is
		         noise, and the table below covers the rest. */ ?>
		<?php if ($i === $last && $peak > 0): ?>
			<text class="value-label" x="<?= round($rev_x + $bar_w / 2, 1) ?>" y="<?= round($rev_y - 6, 1) ?>" text-anchor="middle"><?= number_format((float) $m['revenue']) ?></text>
			<text class="value-label" x="<?= round($exp_x + $bar_w / 2, 1) ?>" y="<?= round($exp_y - 6, 1) ?>" text-anchor="middle"><?= number_format((float) $m['expenses']) ?></text>
		<?php endif; ?>

		<text class="axis-label" x="<?= round($centre, 1) ?>" y="<?= $h - 9 ?>" text-anchor="middle"><?= html_escape(ops_month_label($m['month'])) ?></text>
	<?php endforeach; ?>
</svg>

<div class="chart-legend mt-2">
	<span class="key"><span class="swatch swatch-revenue"></span> Revenue (payments received)</span>
	<span class="key"><span class="swatch swatch-expense"></span> Expenses</span>
</div>
