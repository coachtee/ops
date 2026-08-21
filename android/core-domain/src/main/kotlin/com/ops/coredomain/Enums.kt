package com.ops.coredomain

/**
 * Wire-format enums. Every `wire` value here is copy-checked against the
 * Django `choices` lists on the model that owns it — these strings travel
 * over the network (in CRUD bodies and sync `fields` payloads) and into
 * Room, so they must match byte-for-byte:
 *   - LeadSource / LeadStatus     -> backend/crm/models.py (Lead)
 *   - CustomerType                -> backend/crm/models.py (Customer)
 *   - QuoteStatus                 -> backend/sales/models.py (Quote)
 *   - JobStatus                   -> backend/work/models.py (Job)
 *   - InvoiceStatus / PaymentMethod / ExpenseCategory -> backend/finance/models.py
 *   - PayRateType                 -> backend/people/models.py (Employee)
 *
 * Each enum carries its own [wire] value rather than relying on `name`, so a
 * future Kotlin-side rename never silently breaks the network contract, and
 * exposes [fromWire] for parsing values coming back from the server.
 */

interface WireEnum {
    val wire: String
}

private fun <T> requireWire(values: Array<T>, wire: String, label: String): T where T : Enum<T>, T : WireEnum =
    values.firstOrNull { it.wire == wire }
        ?: throw IllegalArgumentException("Unknown $label wire value: '$wire'")

enum class LeadSource(override val wire: String) : WireEnum {
    WHATSAPP("whatsapp"),
    CALL("call"),
    FACEBOOK("facebook"),
    WEBSITE("website"),
    EMAIL("email"),
    REFERRAL("referral"),
    WALKIN("walkin"),
    TENDER("tender"),
    OTHER("other"),
    ;

    companion object {
        fun fromWire(wire: String): LeadSource = requireWire(entries.toTypedArray(), wire, "LeadSource")
    }
}

enum class LeadStatus(override val wire: String) : WireEnum {
    NEW("new"),
    CONTACTED("contacted"),
    QUOTED("quoted"),
    CONVERTED("converted"),
    LOST("lost"),
    ;

    companion object {
        fun fromWire(wire: String): LeadStatus = requireWire(entries.toTypedArray(), wire, "LeadStatus")
    }
}

enum class CustomerType(override val wire: String) : WireEnum {
    INDIVIDUAL("individual"),
    COMPANY("company"),
    ;

    companion object {
        fun fromWire(wire: String): CustomerType = requireWire(entries.toTypedArray(), wire, "CustomerType")
    }
}

enum class QuoteStatus(override val wire: String) : WireEnum {
    DRAFT("draft"),
    SENT("sent"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    EXPIRED("expired"),
    ;

    companion object {
        fun fromWire(wire: String): QuoteStatus = requireWire(entries.toTypedArray(), wire, "QuoteStatus")
    }
}

enum class JobStatus(override val wire: String) : WireEnum {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    ;

    companion object {
        fun fromWire(wire: String): JobStatus = requireWire(entries.toTypedArray(), wire, "JobStatus")
    }
}

enum class InvoiceStatus(override val wire: String) : WireEnum {
    DRAFT("draft"),
    SENT("sent"),
    PARTIALLY_PAID("partially_paid"),
    PAID("paid"),
    OVERDUE("overdue"),
    CANCELLED("cancelled"),
    ;

    companion object {
        fun fromWire(wire: String): InvoiceStatus = requireWire(entries.toTypedArray(), wire, "InvoiceStatus")
    }
}

enum class PaymentMethod(override val wire: String) : WireEnum {
    CASH("cash"),
    EFT("eft"),
    CARD("card"),
    SNAPSCAN("snapscan"),
    OTHER("other"),
    ;

    companion object {
        fun fromWire(wire: String): PaymentMethod = requireWire(entries.toTypedArray(), wire, "PaymentMethod")
    }
}

enum class ExpenseCategory(override val wire: String) : WireEnum {
    MATERIALS_STOCK("materials_stock"),
    FUEL_TRAVEL("fuel_travel"),
    TOOLS_EQUIPMENT("tools_equipment"),
    RENT("rent"),
    UTILITIES("utilities"),
    INSURANCE("insurance"),
    BANK_CHARGES("bank_charges"),
    PROFESSIONAL_FEES("professional_fees"),
    MARKETING("marketing"),
    TELEPHONE_INTERNET("telephone_internet"),
    VEHICLE("vehicle"),
    REPAIRS_MAINTENANCE("repairs_maintenance"),
    WAGES_SUBCONTRACTORS("wages_subcontractors"),
    OTHER("other"),
    ;

    companion object {
        fun fromWire(wire: String): ExpenseCategory = requireWire(entries.toTypedArray(), wire, "ExpenseCategory")
    }
}

enum class PayRateType(override val wire: String) : WireEnum {
    HOURLY("hourly"),
    DAILY("daily"),
    MONTHLY("monthly"),
    ;

    companion object {
        fun fromWire(wire: String): PayRateType = requireWire(entries.toTypedArray(), wire, "PayRateType")
    }
}
