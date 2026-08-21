from datetime import date, timedelta
from decimal import Decimal

from django.utils import timezone
from rest_framework import status

from finance.models import Expense, Invoice, InvoiceLineItem, Payment
from finance.services import recompute_expense_vat, recompute_invoice_totals

from .helpers import AuthenticatedAPITestCase


class ProfitSummaryTests(AuthenticatedAPITestCase):
    def test_revenue_and_expenses_are_cash_basis_by_month(self):
        today = date.today()
        this_month = today.replace(day=1)
        last_month = (this_month - timedelta(days=1)).replace(day=1)

        Payment.objects.create(
            business=self.business, customer=self.customer, amount=Decimal("1000.00"),
            method=Payment.METHOD_EFT, paid_date=this_month,
        )
        Payment.objects.create(
            business=self.business, customer=self.customer, amount=Decimal("500.00"),
            method=Payment.METHOD_CASH, paid_date=last_month,
        )
        Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_FUEL_TRAVEL, amount=Decimal("200.00"),
            is_vat_applicable=False, date=this_month,
        )

        response = self.client.get("/api/reports/profit-summary/?months=2")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        months = {row["month"]: row for row in response.data["months"]}

        this_key = this_month.strftime("%Y-%m")
        last_key = last_month.strftime("%Y-%m")
        self.assertEqual(months[this_key]["revenue"], "1000.00")
        self.assertEqual(months[this_key]["expenses"], "200.00")
        self.assertEqual(months[this_key]["profit"], "800.00")
        self.assertEqual(months[last_key]["revenue"], "500.00")
        self.assertEqual(months[last_key]["expenses"], "0.00")
        self.assertEqual(months[last_key]["profit"], "500.00")

    def test_months_are_returned_oldest_first(self):
        response = self.client.get("/api/reports/profit-summary/?months=3")
        months = [row["month"] for row in response.data["months"]]
        self.assertEqual(months, sorted(months))
        self.assertEqual(len(months), 3)

    def test_month_with_no_activity_is_zero_not_missing(self):
        response = self.client.get("/api/reports/profit-summary/?months=4")
        self.assertEqual(len(response.data["months"]), 4)
        for row in response.data["months"]:
            self.assertEqual(row["revenue"], "0.00")
            self.assertEqual(row["expenses"], "0.00")
            self.assertEqual(row["profit"], "0.00")

    def test_months_parameter_is_capped(self):
        response = self.client.get("/api/reports/profit-summary/?months=999")
        self.assertEqual(len(response.data["months"]), 24)

    def test_soft_deleted_payment_is_excluded(self):
        today = date.today().replace(day=1)
        payment = Payment.objects.create(
            business=self.business, customer=self.customer, amount=Decimal("1000.00"),
            method=Payment.METHOD_EFT, paid_date=today,
        )
        payment.deleted_at = timezone.now()
        payment.save(update_fields=["deleted_at"])

        response = self.client.get("/api/reports/profit-summary/?months=1")
        self.assertEqual(response.data["months"][0]["revenue"], "0.00")

    def test_scoped_to_own_business_only(self):
        other_client, other_business = self.make_other_business_client()
        today = date.today().replace(day=1)
        from crm.models import Customer

        other_customer = Customer.objects.create(business=other_business, name="Other Customer")
        Payment.objects.create(
            business=other_business, customer=other_customer, amount=Decimal("9999.00"),
            method=Payment.METHOD_EFT, paid_date=today,
        )
        response = self.client.get("/api/reports/profit-summary/?months=1")
        self.assertEqual(response.data["months"][0]["revenue"], "0.00")

    def test_csv_export(self):
        response = self.client.get("/api/reports/profit-summary/?months=1&export=csv")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response["Content-Type"], "text/csv")
        content = response.content.decode()
        self.assertIn("Month,Revenue,Expenses,Profit", content)


class ExpenseCategoriesTests(AuthenticatedAPITestCase):
    def test_biggest_category_first(self):
        today = date.today()
        Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_MATERIALS_STOCK, amount=Decimal("500.00"),
            is_vat_applicable=False, date=today,
        )
        Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_FUEL_TRAVEL, amount=Decimal("1500.00"),
            is_vat_applicable=False, date=today,
        )
        Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_FUEL_TRAVEL, amount=Decimal("300.00"),
            is_vat_applicable=False, date=today,
        )

        response = self.client.get("/api/reports/expense-categories/?period=this_month")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        categories = response.data["categories"]
        self.assertEqual(categories[0]["category"], "fuel_travel")
        self.assertEqual(categories[0]["total"], "1800.00")
        self.assertEqual(categories[0]["label"], "Fuel & travel")
        self.assertEqual(categories[1]["category"], "materials_stock")
        self.assertEqual(categories[1]["total"], "500.00")

    def test_categories_with_no_spend_are_omitted(self):
        response = self.client.get("/api/reports/expense-categories/?period=this_month")
        self.assertEqual(response.data["categories"], [])

    def test_this_month_excludes_older_expenses(self):
        last_month = (date.today().replace(day=1) - timedelta(days=1))
        Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_RENT, amount=Decimal("2000.00"),
            is_vat_applicable=False, date=last_month,
        )
        response = self.client.get("/api/reports/expense-categories/?period=this_month")
        self.assertEqual(response.data["categories"], [])

        all_time = self.client.get("/api/reports/expense-categories/?period=all_time")
        self.assertEqual(len(all_time.data["categories"]), 1)
        self.assertEqual(all_time.data["categories"][0]["total"], "2000.00")

    def test_invalid_period_falls_back_to_this_month(self):
        response = self.client.get("/api/reports/expense-categories/?period=bogus")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["period"], "this_month")

    def test_scoped_to_own_business_only(self):
        other_client, other_business = self.make_other_business_client()
        Expense.objects.create(
            business=other_business, category=Expense.CATEGORY_OTHER, amount=Decimal("9999.00"),
            is_vat_applicable=False, date=date.today(),
        )
        response = self.client.get("/api/reports/expense-categories/?period=this_month")
        self.assertEqual(response.data["categories"], [])


class VatSummaryTests(AuthenticatedAPITestCase):
    def _create_sent_invoice(self, vat_amount, issue_date, status_=Invoice.STATUS_SENT):
        invoice = Invoice.objects.create(
            business=self.business, customer=self.customer, status=status_,
            issue_date=issue_date, is_vat_applicable=True,
        )
        InvoiceLineItem.objects.create(
            business=self.business, invoice=invoice, description="Work", quantity=1,
            unit_price=Decimal("100.00"), line_total=Decimal("100.00"),
        )
        recompute_invoice_totals(invoice, bump_updated_at=False)
        invoice.refresh_from_db()
        # Force a specific vat_amount for a clean, predictable assertion
        # regardless of the 15% computation, since this test is about
        # aggregation, not VAT math (already covered by test_money.py).
        invoice.vat_amount = vat_amount
        invoice.save(update_fields=["vat_amount"])
        return invoice

    def test_vat_collected_and_paid_within_default_period(self):
        today = date.today()
        this_month = today.replace(day=1)
        self._create_sent_invoice(Decimal("150.00"), this_month)
        expense = Expense.objects.create(
            business=self.business, category=Expense.CATEGORY_MATERIALS_STOCK, amount=Decimal("115.00"),
            is_vat_applicable=True, date=this_month,
        )
        recompute_expense_vat(expense, bump_updated_at=False)

        response = self.client.get("/api/reports/vat-summary/")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(response.data["vat_collected"], "150.00")
        self.assertEqual(response.data["vat_paid"], "15.00")
        self.assertEqual(response.data["net_vat_position"], "135.00")

    def test_draft_and_cancelled_invoices_are_excluded(self):
        today = date.today().replace(day=1)
        self._create_sent_invoice(Decimal("100.00"), today, status_=Invoice.STATUS_DRAFT)
        self._create_sent_invoice(Decimal("100.00"), today, status_=Invoice.STATUS_CANCELLED)

        response = self.client.get("/api/reports/vat-summary/")
        self.assertEqual(response.data["vat_collected"], "0.00")

    def test_explicit_date_range(self):
        self._create_sent_invoice(Decimal("50.00"), date(2026, 1, 15))
        response = self.client.get("/api/reports/vat-summary/?since=2026-01-01&until=2026-01-31")
        self.assertEqual(response.data["vat_collected"], "50.00")
        self.assertEqual(response.data["since"], "2026-01-01")
        self.assertEqual(response.data["until"], "2026-01-31")

    def test_scoped_to_own_business_only(self):
        other_client, other_business = self.make_other_business_client()
        from crm.models import Customer

        other_customer = Customer.objects.create(business=other_business, name="Other Customer")
        Invoice.objects.create(
            business=other_business, customer=other_customer, status=Invoice.STATUS_SENT,
            issue_date=date.today(), is_vat_applicable=True, vat_amount=Decimal("9999.00"),
        )
        response = self.client.get("/api/reports/vat-summary/")
        self.assertEqual(response.data["vat_collected"], "0.00")
