package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `employee` sync model's `fields` payload in API_CONTRACT.md —
 * a staff contact plus the agreed pay rate, not a workforce-management
 * module (no shift/hours tracking). `payRate`/`payRateType` are shown back
 * as a reminder on the payslip form, never used to auto-compute a
 * payslip's grossPay. */
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    /** [com.ops.coredomain.PayRateType] wire value. */
    val payRateType: String,
    /** Decimal string — never a float. */
    val payRate: String,
    /** `YYYY-MM-DD`, or null if not recorded. */
    val startDate: String?,
    val notes: String,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
