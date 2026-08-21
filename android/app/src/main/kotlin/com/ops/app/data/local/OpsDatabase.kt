package com.ops.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ops.app.data.local.dao.BusinessDao
import com.ops.app.data.local.dao.CustomerDao
import com.ops.app.data.local.dao.InvoiceDao
import com.ops.app.data.local.dao.InvoiceLineItemDao
import com.ops.app.data.local.dao.JobDao
import com.ops.app.data.local.dao.LeadDao
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.dao.QuoteDao
import com.ops.app.data.local.dao.QuoteLineItemDao
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity

/**
 * Room is the source of truth for every screen — no screen ever waits on the
 * network (see DISCOVERY.md section 6). Every money field on every entity
 * here (quantity, unit_price, line_total, subtotal, vat_amount, total,
 * discount_amount, amount_paid, amount) is stored as TEXT holding the
 * canonical decimal string, e.g. "1250.00" — never REAL/float — converted
 * via `BigDecimal(string)` at the domain boundary (see
 * com.ops.coredomain.Money). This is deliberate: a REAL column would let
 * SQLite silently round-trip money through an IEEE-754 double, reintroducing
 * exactly the float-rounding bug class DecimalField avoids on the Django side.
 *
 * No Room [androidx.room.TypeConverter]s are needed: every column here is
 * already a Room-native type (String, Boolean, Int) — that's also why money
 * is TEXT rather than a custom BigDecimal type converter, which would hide a
 * REAL/float column behind the scenes on some Room versions.
 */
@Database(
    entities = [
        BusinessEntity::class,
        LeadEntity::class,
        CustomerEntity::class,
        QuoteEntity::class,
        QuoteLineItemEntity::class,
        JobEntity::class,
        InvoiceEntity::class,
        InvoiceLineItemEntity::class,
        PaymentEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class OpsDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun leadDao(): LeadDao
    abstract fun customerDao(): CustomerDao
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteLineItemDao(): QuoteLineItemDao
    abstract fun jobDao(): JobDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceLineItemDao(): InvoiceLineItemDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        const val DATABASE_NAME = "ops.db"
    }
}
