<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Ports backend/common/money.py exactly (see that file's own docstring for
 * the business rules — this is the single source of truth being mirrored,
 * not reinterpreted). No bcmath in this environment (php8.4-bcmath ships
 * from a blocked PPA — see backend-php/README.md), so this uses PHP's
 * native float + round() instead of arbitrary-precision decimal strings.
 * This is safe here specifically because every function below does AT
 * MOST one multiply/divide followed immediately by one round() to cents —
 * never a chain of unrounded float operations — so no error can
 * accumulate. PHP_ROUND_HALF_UP (the default mode) rounds ties away from
 * zero, matching Python's Decimal ROUND_HALF_UP exactly for the
 * all-non-negative amounts this app ever deals in.
 */

const OPS_VAT_RATE = 0.15;

/** Rounds to 2 decimal places, half-up, and formats as a fixed "0.00"
 * string — the wire format every money field uses (never a bare float). */
function money_quantize($value)
{
	return number_format(round((float) $value, 2, PHP_ROUND_HALF_UP), 2, '.', '');
}

function money_compute_line_total($quantity, $unit_price)
{
	return money_quantize((float) $quantity * (float) $unit_price);
}

/**
 * @param string[] $line_totals
 * @return array{0: string, 1: string, 2: string} [subtotal, vat_amount, total]
 */
function money_compute_document_totals(array $line_totals, $discount_amount, $is_vat_applicable)
{
	$subtotal_raw = 0.0;
	foreach ($line_totals as $t)
	{
		$subtotal_raw += (float) $t;
	}
	$subtotal = money_quantize($subtotal_raw);

	$discount = money_quantize($discount_amount ?: 0);
	$taxable = (float) $subtotal - (float) $discount;
	if ($taxable < 0)
	{
		$taxable = 0.0;
	}

	$vat_amount = $is_vat_applicable ? money_quantize($taxable * OPS_VAT_RATE) : '0.00';
	$total = money_quantize($taxable + (float) $vat_amount);

	return array($subtotal, $vat_amount, $total);
}

/**
 * Expenses run the opposite direction from quotes/invoices: the owner
 * already knows the VAT-inclusive total they paid, and wants to know how
 * much of it was VAT — not have VAT added on top. Standard SA VAT-
 * inclusive extraction: vat = total * rate / (1 + rate), i.e. total *
 * 15/115 at the current flat rate. Returns "0.00" when the expense wasn't
 * VAT-charged at all (e.g. a non-VAT-registered supplier, or bank charges).
 */
function money_extract_vat_from_inclusive($inclusive_amount, $is_vat_applicable)
{
	if (!$is_vat_applicable)
	{
		return '0.00';
	}
	$amount = (float) $inclusive_amount;
	return money_quantize($amount * OPS_VAT_RATE / (1 + OPS_VAT_RATE));
}

/** net_pay = gross_pay - deductions — see backend/people/models.py's
 * Payslip.save(); both inputs are already cent-precision so this is exact. */
function money_compute_net_pay($gross_pay, $deductions)
{
	return money_quantize((float) $gross_pay - (float) $deductions);
}
