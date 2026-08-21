"""
Reports: the question-shaped answers the brief asks for ("what did I make
this month", "what are my biggest expenses", VAT collected vs paid) —
computed on demand from data that already exists (Payment, Expense,
Invoice), never a new stored model of its own. No general ledger, trial
balance, or chart of accounts — see DISCOVERY.md's "Explicitly NOT in V1"
list. "Revenue" here is cash-basis (actual payments received), matching
Home dashboard's "money in" definition exactly, not invoiced/accrued
amounts — one financial vocabulary throughout the app, not two.
"""

import csv
from datetime import date
from decimal import Decimal

from django.db.models import Sum
from django.db.models.functions import TruncMonth
from django.http import HttpResponse
from django.utils.dateparse import parse_date
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.services import get_current_business
from common.money import quantize
from finance.models import Expense, Invoice, Payment

MAX_MONTHS = 24
DEFAULT_MONTHS = 6


def _month_start(d: date) -> date:
    return d.replace(day=1)


def _add_months(d: date, months: int) -> date:
    month_index = d.month - 1 + months
    year = d.year + month_index // 12
    month = month_index % 12 + 1
    return date(year, month, 1)


def _last_n_month_starts(n: int, today: date) -> list[date]:
    current = _month_start(today)
    return [_add_months(current, -offset) for offset in range(n - 1, -1, -1)]


class ProfitSummaryView(APIView):
    """`GET /api/reports/profit-summary/?months=6[&export=csv]` — per-month
    revenue (payments received), expenses, and profit for the last `months`
    calendar months (default 6, capped at 24), oldest first.

    `export=csv` rather than DRF's usual `?format=csv` deliberately —
    `format` is a reserved query param DRF's own content-negotiation
    intercepts before a view's `get()` even runs, and 404s on an
    unregistered format (there is no CSV renderer configured), which
    would silently swallow this branch entirely.
    """

    def get(self, request):
        business = get_current_business(request.user)
        months = min(max(int(request.query_params.get("months", DEFAULT_MONTHS)), 1), MAX_MONTHS)
        today = date.today()
        month_starts = _last_n_month_starts(months, today)
        range_start = month_starts[0]

        revenue_by_month = {
            row["month"]: row["total"]
            for row in Payment.objects.filter(
                business=business, deleted_at__isnull=True, paid_date__gte=range_start
            )
            .annotate(month=TruncMonth("paid_date"))
            .values("month")
            .annotate(total=Sum("amount"))
        }
        expenses_by_month = {
            row["month"]: row["total"]
            for row in Expense.objects.filter(
                business=business, deleted_at__isnull=True, date__gte=range_start
            )
            .annotate(month=TruncMonth("date"))
            .values("month")
            .annotate(total=Sum("amount"))
        }

        rows = []
        for month_start in month_starts:
            revenue = quantize(revenue_by_month.get(month_start) or Decimal("0.00"))
            expenses = quantize(expenses_by_month.get(month_start) or Decimal("0.00"))
            rows.append(
                {
                    "month": month_start.strftime("%Y-%m"),
                    "revenue": str(revenue),
                    "expenses": str(expenses),
                    "profit": str(quantize(revenue - expenses)),
                }
            )

        if request.query_params.get("export") == "csv":
            response = HttpResponse(content_type="text/csv")
            response["Content-Disposition"] = 'attachment; filename="profit-summary.csv"'
            writer = csv.writer(response)
            writer.writerow(["Month", "Revenue", "Expenses", "Profit"])
            for row in rows:
                writer.writerow([row["month"], row["revenue"], row["expenses"], row["profit"]])
            return response

        return Response({"months": rows})


class ExpenseCategoriesView(APIView):
    """`GET /api/reports/expense-categories/?period=this_month|all_time` —
    total spent per category, biggest first. Categories with nothing spent
    are omitted rather than listed at zero."""

    def get(self, request):
        business = get_current_business(request.user)
        period = request.query_params.get("period", "this_month")
        if period not in ("this_month", "all_time"):
            period = "this_month"

        queryset = Expense.objects.filter(business=business, deleted_at__isnull=True)
        if period == "this_month":
            queryset = queryset.filter(date__gte=_month_start(date.today()))

        totals = (
            queryset.values("category")
            .annotate(total=Sum("amount"))
            .order_by("-total")
        )
        category_labels = dict(Expense.CATEGORY_CHOICES)

        categories = [
            {
                "category": row["category"],
                "label": category_labels.get(row["category"], row["category"]),
                "total": str(quantize(row["total"])),
            }
            for row in totals
            if row["total"]
        ]

        return Response({"period": period, "categories": categories})


class VatSummaryView(APIView):
    """`GET /api/reports/vat-summary/?since=YYYY-MM-DD&until=YYYY-MM-DD` —
    VAT collected (output VAT on sent/paid invoices) vs VAT paid (input VAT
    on expenses) for the period, defaulting to the current calendar month.
    Informational only — for the owner's own SARS VAT201 prep, not a
    computation this app files anywhere."""

    def get(self, request):
        business = get_current_business(request.user)
        today = date.today()
        since = parse_date(request.query_params.get("since", "")) or _month_start(today)
        until = parse_date(request.query_params.get("until", "")) or today

        vat_collected = Invoice.objects.filter(
            business=business,
            deleted_at__isnull=True,
            issue_date__gte=since,
            issue_date__lte=until,
        ).exclude(status__in=[Invoice.STATUS_DRAFT, Invoice.STATUS_CANCELLED]).aggregate(
            total=Sum("vat_amount")
        )["total"] or Decimal("0.00")

        vat_paid = Expense.objects.filter(
            business=business,
            deleted_at__isnull=True,
            date__gte=since,
            date__lte=until,
        ).aggregate(total=Sum("vat_amount"))["total"] or Decimal("0.00")

        vat_collected = quantize(vat_collected)
        vat_paid = quantize(vat_paid)

        return Response(
            {
                "since": since.isoformat(),
                "until": until.isoformat(),
                "vat_collected": str(vat_collected),
                "vat_paid": str(vat_paid),
                "net_vat_position": str(quantize(vat_collected - vat_paid)),
            }
        )
