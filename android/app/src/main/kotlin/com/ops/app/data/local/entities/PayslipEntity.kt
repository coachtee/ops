package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/**
 * Mirrors the `payslip` sync model's `fields` payload in API_CONTRACT.md.
 * `grossPay` and `deductions` are entered by the owner (or copied from
 * whatever their bookkeeper tells them) — this app deliberately does not
 * compute PAYE/UIF tax tables. `netPay` is derived (`grossPay - deductions`)
 * computed locally for instant UI and overwritten by the server's value
 * once synced — same pattern as `vatAmount` on [ExpenseEntity].
 */
@Entity(tableName = "payslips")
data class PayslipEntity(
    @PrimaryKey override val id: String,
    val employeeId: String,
    /** `YYYY-MM-DD`. */
    val periodStart: String,
    /** `YYYY-MM-DD`. */
    val periodEnd: String,
    /** Decimal string — never a float. */
    val grossPay: String,
    /** Decimal string — never a float. */
    val deductions: String,
    val deductionsNote: String,
    /** Decimal string, derived from grossPay/deductions — never hand-entered. */
    val netPay: String,
    /** `YYYY-MM-DD`, or null if not yet paid. */
    val paidDate: String?,
    val notes: String,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
