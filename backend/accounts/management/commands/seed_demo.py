"""
Seeds a realistic South African demo business — Thabo's Plumbing &
Maintenance — walking every stage of the vertical slice: business setup,
leads, customers, quotes, jobs, invoices, payments, expenses and
suppliers. Used to populate the backend the Android app demo build
points at.

Usage: python manage.py seed_demo
"""

from datetime import date, timedelta
from decimal import Decimal

from django.core.management.base import BaseCommand
from django.db import transaction
from django.utils import timezone

from accounts.models import Business, Membership, User
from crm.models import Customer, Lead
from finance.models import Expense, Invoice, InvoiceLineItem, Payment, Supplier
from finance.services import (
    assign_invoice_number_if_needed,
    recompute_expense_vat,
    recompute_invoice_payment_state,
    recompute_invoice_totals,
)
from people.models import Employee, Payslip
from people.services import recompute_payslip_net_pay
from sales.models import Quote, QuoteLineItem
from sales.services import assign_quote_number_if_needed, recompute_quote_totals
from work.models import Job
from work.services import assign_job_number_if_needed

DEMO_EMAIL = "thabo@thabosplumbing.co.za"


class Command(BaseCommand):
    help = "Seed a realistic South African demo business (Thabo's Plumbing & Maintenance)."

    @transaction.atomic
    def handle(self, *args, **options):
        if User.objects.filter(email=DEMO_EMAIL).exists():
            self.stdout.write(self.style.WARNING(f"{DEMO_EMAIL} already exists — skipping."))
            return

        business = Business.objects.create(
            name="Thabo's Plumbing & Maintenance",
            trading_name="Thabo's Plumbing",
            registration_number="2019/123456/07",
            tax_number="9012345678",
            vat_number="",
            is_vat_registered=False,
            industry="plumbing",
            phone="+27 82 123 4567",
            email="info@thabosplumbing.co.za",
            address_line1="12 Vygie Street",
            suburb="Delft",
            city="Cape Town",
            province="WC",
            postal_code="7100",
        )
        user = User.objects.create_user(
            email=DEMO_EMAIL,
            password="Demo12345",
            first_name="Thabo",
            last_name="Nkosi",
            phone="+27 82 123 4567",
        )
        Membership.objects.create(user=user, business=business, role=Membership.ROLE_OWNER)

        today = timezone.now()

        # --- Leads -----------------------------------------------------
        lead_pending = Lead.objects.create(
            business=business,
            name="Nomsa Dlamini",
            phone="+27 83 555 1122",
            email="nomsa.dlamini@gmail.com",
            source="whatsapp",
            enquiry="Geyser burst in the roof, water coming through the ceiling in Bellville.",
            status="new",
            follow_up_date=(today + timedelta(days=1)).date(),
        )
        Lead.objects.create(
            business=business,
            name="Riaan Botha",
            phone="+27 84 221 9087",
            email="",
            source="facebook",
            enquiry="Wants a quote to re-pipe an old house in Parow before renting it out.",
            status="contacted",
            notes="Spoke to him Tuesday, sending a quote once I've seen photos of the pipework.",
            follow_up_date=(today + timedelta(days=3)).date(),
        )
        Lead.objects.create(
            business=business,
            name="Fatima Adams",
            phone="+27 21 555 7890",
            email="fatima@adamsattorneys.co.za",
            source="referral",
            enquiry="Blocked drain at their office in Goodwood, referred by a past customer.",
            status="lost",
            notes="Went with another plumber who could come out same day.",
        )

        # --- Customer (converted from a lead) ---------------------------
        customer_lead = Lead.objects.create(
            business=business,
            name="Sipho Khumalo",
            phone="+27 82 340 5566",
            email="sipho.khumalo@outlook.com",
            source="call",
            enquiry="New bathroom installation, semi-detached house in Kuils River.",
            status="converted",
        )
        customer = Customer.objects.create(
            business=business,
            name="Sipho Khumalo",
            customer_type="individual",
            phone="+27 82 340 5566",
            email="sipho.khumalo@outlook.com",
            address_line1="45 Protea Avenue",
            suburb="Kuils River",
            city="Cape Town",
            province="Western Cape",
            postal_code="7580",
            source_lead=customer_lead,
        )
        customer_lead.converted_customer = customer
        customer_lead.save(update_fields=["converted_customer"])

        second_customer = Customer.objects.create(
            business=business,
            name="Greenline Property Management",
            customer_type="company",
            phone="+27 21 555 3344",
            email="maintenance@greenlineprops.co.za",
            address_line1="8 Voortrekker Road",
            suburb="Bellville",
            city="Cape Town",
            province="Western Cape",
            postal_code="7530",
            notes="Manages 6 rental units — recurring maintenance work.",
        )

        # --- Quote -> accepted -> job -----------------------------------
        quote = Quote.objects.create(
            business=business,
            customer=customer,
            lead=customer_lead,
            status=Quote.STATUS_ACCEPTED,
            issue_date=(today - timedelta(days=10)).date(),
            valid_until=(today + timedelta(days=20)).date(),
            notes="New bathroom plumbing installation — supply and fit.",
            terms="50% deposit on acceptance, balance on completion. Valid for 30 days.",
            is_vat_applicable=False,
            sent_at=today - timedelta(days=10),
            accepted_at=today - timedelta(days=8),
        )
        QuoteLineItem.objects.create(
            business=business, quote=quote, description="Toilet supply and installation",
            quantity=1, unit_price=Decimal("2200.00"), sort_order=1,
        )
        QuoteLineItem.objects.create(
            business=business, quote=quote, description="Basin, mixer tap and waste installation",
            quantity=1, unit_price=Decimal("1850.00"), sort_order=2,
        )
        QuoteLineItem.objects.create(
            business=business, quote=quote, description="Shower installation incl. copper piping",
            quantity=1, unit_price=Decimal("3400.00"), sort_order=3,
        )
        QuoteLineItem.objects.create(
            business=business, quote=quote, description="Labour (2 days)",
            quantity=2, unit_price=Decimal("950.00"), sort_order=4,
        )
        for line_item in quote.line_items.all():
            line_item.line_total = line_item.quantity * line_item.unit_price
            line_item.save(update_fields=["line_total"])
        recompute_quote_totals(quote, bump_updated_at=False)
        assign_quote_number_if_needed(quote)

        job = Job.objects.create(
            business=business,
            customer=customer,
            quote=quote,
            title="Bathroom installation — Khumalo residence",
            description="Full bathroom plumbing installation: toilet, basin and shower.",
            status=Job.STATUS_COMPLETED,
            start_date=(today - timedelta(days=7)).date(),
            due_date=(today - timedelta(days=5)).date(),
            completed_date=(today - timedelta(days=5)).date(),
        )
        assign_job_number_if_needed(job)

        # --- Invoice, partially paid ------------------------------------
        invoice = Invoice.objects.create(
            business=business,
            customer=customer,
            job=job,
            quote=quote,
            status=Invoice.STATUS_SENT,
            issue_date=(today - timedelta(days=5)).date(),
            due_date=(today + timedelta(days=9)).date(),
            notes="Thank you for your business!",
            terms="Payment due within 14 days.",
            is_vat_applicable=False,
            sent_at=today - timedelta(days=5),
        )
        for line_item in quote.line_items.all():
            InvoiceLineItem.objects.create(
                business=business,
                invoice=invoice,
                description=line_item.description,
                quantity=line_item.quantity,
                unit_price=line_item.unit_price,
                line_total=line_item.line_total,
                sort_order=line_item.sort_order,
            )
        recompute_invoice_totals(invoice, bump_updated_at=False)
        assign_invoice_number_if_needed(invoice)

        payment = Payment.objects.create(
            business=business,
            customer=customer,
            invoice=invoice,
            amount=Decimal("5100.00"),
            method=Payment.METHOD_EFT,
            reference="KHUMALO DEPOSIT",
            paid_date=(today - timedelta(days=8)).date(),
            notes="50% deposit paid on acceptance.",
        )
        recompute_invoice_payment_state(invoice, bump_updated_at=False)

        # --- A second, unpaid invoice for the recurring commercial customer
        quote2 = Quote.objects.create(
            business=business,
            customer=second_customer,
            status=Quote.STATUS_SENT,
            issue_date=(today - timedelta(days=2)).date(),
            valid_until=(today + timedelta(days=28)).date(),
            notes="Quarterly maintenance — geyser inspection across 6 units.",
            is_vat_applicable=False,
            sent_at=today - timedelta(days=2),
        )
        QuoteLineItem.objects.create(
            business=business, quote=quote2, description="Geyser inspection and service (6 units)",
            quantity=6, unit_price=Decimal("450.00"), sort_order=1,
        )
        for line_item in quote2.line_items.all():
            line_item.line_total = line_item.quantity * line_item.unit_price
            line_item.save(update_fields=["line_total"])
        recompute_quote_totals(quote2, bump_updated_at=False)
        assign_quote_number_if_needed(quote2)

        overdue_invoice = Invoice.objects.create(
            business=business,
            customer=second_customer,
            status=Invoice.STATUS_OVERDUE,
            issue_date=(today - timedelta(days=30)).date(),
            due_date=(today - timedelta(days=16)).date(),
            is_vat_applicable=False,
        )
        InvoiceLineItem.objects.create(
            business=business, invoice=overdue_invoice,
            description="Blocked drain clearing — Unit 4", quantity=1,
            unit_price=Decimal("980.00"), sort_order=1,
        )
        recompute_invoice_totals(overdue_invoice, bump_updated_at=False)
        assign_invoice_number_if_needed(overdue_invoice)

        # --- Suppliers ----------------------------------------------------
        builders_warehouse = Supplier.objects.create(
            business=business,
            name="Builders Warehouse Kuils River",
            contact_person="Annelie Botha",
            phone="+27 21 903 4455",
            email="kuilsriver@builderswarehouse.co.za",
            notes="Main materials supplier — good account terms, 30 days.",
        )
        Supplier.objects.create(
            business=business,
            name="Cashbuild Delft",
            phone="+27 21 954 2210",
            notes="Backup supplier when Builders Warehouse is out of stock.",
        )

        # --- Expenses — money out, against the Khumalo job and general overheads
        materials = Expense.objects.create(
            business=business,
            job=job,
            supplier=builders_warehouse,
            category=Expense.CATEGORY_MATERIALS_STOCK,
            description="Copper piping, fittings and sealant — Builders",
            amount=Decimal("1840.50"),
            is_vat_applicable=True,
            date=(today - timedelta(days=7)).date(),
        )
        recompute_expense_vat(materials, bump_updated_at=False)

        fuel = Expense.objects.create(
            business=business,
            job=job,
            category=Expense.CATEGORY_FUEL_TRAVEL,
            description="Diesel — bakkie, Kuils River round trips",
            amount=Decimal("450.00"),
            is_vat_applicable=True,
            date=(today - timedelta(days=6)).date(),
        )
        recompute_expense_vat(fuel, bump_updated_at=False)

        bank_charges = Expense.objects.create(
            business=business,
            category=Expense.CATEGORY_BANK_CHARGES,
            description="FNB business account monthly fee",
            amount=Decimal("189.00"),
            is_vat_applicable=False,
            date=(today - timedelta(days=3)).date(),
        )
        recompute_expense_vat(bank_charges, bump_updated_at=False)

        # --- Employees & payslips -----------------------------------------
        helper = Employee.objects.create(
            business=business,
            name="Bongani Sithole",
            role="Plumber's assistant",
            phone="+27 71 442 8890",
            pay_rate_type=Employee.PAY_RATE_HOURLY,
            pay_rate=Decimal("85.00"),
            start_date=date(2024, 3, 4),
            notes="Started as an apprentice, now works most jobs unsupervised.",
        )

        paid_payslip = Payslip.objects.create(
            business=business,
            employee=helper,
            period_start=(today - timedelta(days=14)).date(),
            period_end=(today - timedelta(days=8)).date(),
            gross_pay=Decimal("3400.00"),
            deductions=Decimal("150.00"),
            deductions_note="UIF",
            paid_date=(today - timedelta(days=7)).date(),
        )
        recompute_payslip_net_pay(paid_payslip, bump_updated_at=False)

        draft_payslip = Payslip.objects.create(
            business=business,
            employee=helper,
            period_start=(today - timedelta(days=7)).date(),
            period_end=today.date(),
            gross_pay=Decimal("3400.00"),
            deductions=Decimal("150.00"),
            deductions_note="UIF",
        )
        recompute_payslip_net_pay(draft_payslip, bump_updated_at=False)

        self.stdout.write(self.style.SUCCESS("Seeded Thabo's Plumbing & Maintenance."))
        self.stdout.write(f"  login: {DEMO_EMAIL} / Demo12345")
        self.stdout.write(f"  leads: {Lead.objects.filter(business=business).count()}")
        self.stdout.write(f"  customers: {Customer.objects.filter(business=business).count()}")
        self.stdout.write(f"  quotes: {Quote.objects.filter(business=business).count()}")
        self.stdout.write(f"  jobs: {Job.objects.filter(business=business).count()}")
        self.stdout.write(f"  invoices: {Invoice.objects.filter(business=business).count()}")
        self.stdout.write(f"  payments: {Payment.objects.filter(business=business).count()}")
        self.stdout.write(f"  expenses: {Expense.objects.filter(business=business).count()}")
        self.stdout.write(f"  suppliers: {Supplier.objects.filter(business=business).count()}")
        self.stdout.write(f"  employees: {Employee.objects.filter(business=business).count()}")
        self.stdout.write(f"  payslips: {Payslip.objects.filter(business=business).count()}")
