from django.urls import path

from .views import ExpenseCategoriesView, ProfitSummaryView, VatSummaryView

urlpatterns = [
    path("reports/profit-summary/", ProfitSummaryView.as_view(), name="report-profit-summary"),
    path("reports/expense-categories/", ExpenseCategoriesView.as_view(), name="report-expense-categories"),
    path("reports/vat-summary/", VatSummaryView.as_view(), name="report-vat-summary"),
]
