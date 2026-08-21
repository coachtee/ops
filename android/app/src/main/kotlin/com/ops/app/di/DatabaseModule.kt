package com.ops.app.di

import android.content.Context
import androidx.room.Room
import com.ops.app.data.local.OpsDatabase
import com.ops.app.data.local.dao.BusinessDao
import com.ops.app.data.local.dao.CustomerDao
import com.ops.app.data.local.dao.ExpenseDao
import com.ops.app.data.local.dao.InvoiceDao
import com.ops.app.data.local.dao.InvoiceLineItemDao
import com.ops.app.data.local.dao.JobDao
import com.ops.app.data.local.dao.LeadDao
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.dao.QuoteDao
import com.ops.app.data.local.dao.QuoteLineItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpsDatabase =
        Room.databaseBuilder(context, OpsDatabase::class.java, OpsDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration() // see OpsDatabase's version-bump comment
            .build()

    @Provides
    fun provideBusinessDao(db: OpsDatabase): BusinessDao = db.businessDao()

    @Provides
    fun provideLeadDao(db: OpsDatabase): LeadDao = db.leadDao()

    @Provides
    fun provideCustomerDao(db: OpsDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideQuoteDao(db: OpsDatabase): QuoteDao = db.quoteDao()

    @Provides
    fun provideQuoteLineItemDao(db: OpsDatabase): QuoteLineItemDao = db.quoteLineItemDao()

    @Provides
    fun provideJobDao(db: OpsDatabase): JobDao = db.jobDao()

    @Provides
    fun provideInvoiceDao(db: OpsDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideInvoiceLineItemDao(db: OpsDatabase): InvoiceLineItemDao = db.invoiceLineItemDao()

    @Provides
    fun providePaymentDao(db: OpsDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun provideExpenseDao(db: OpsDatabase): ExpenseDao = db.expenseDao()
}
