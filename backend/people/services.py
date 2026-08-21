from django.utils import timezone

from common.money import quantize

from .models import Payslip


def recompute_payslip_net_pay(payslip: Payslip, bump_updated_at: bool = True) -> Payslip:
    """gross_pay - deductions, always derived — see Payslip's doc comment."""
    payslip.net_pay = quantize(payslip.gross_pay - payslip.deductions)
    update_fields = ["net_pay"]
    if bump_updated_at:
        payslip.updated_at = timezone.now()
        update_fields.append("updated_at")
    payslip.save(update_fields=update_fields)
    return payslip
