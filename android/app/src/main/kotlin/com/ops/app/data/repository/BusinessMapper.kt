package com.ops.app.data.repository

import com.ops.app.data.local.SyncState
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.remote.dto.BusinessDto
import com.ops.coredomain.IsoTimestamp
import java.time.Instant

/** Shared by [AuthRepository] (register/login) and [BusinessRepository]
 * (GET/PATCH `/api/business/me/`) — every path that gets a [BusinessDto]
 * back from the server funnels through here. */
fun BusinessDto.toLocalEntity(): BusinessEntity = BusinessEntity(
    id = id,
    name = name,
    tradingName = tradingName,
    registrationNumber = registrationNumber,
    taxNumber = taxNumber,
    vatNumber = vatNumber,
    isVatRegistered = isVatRegistered,
    industry = industry,
    phone = phone,
    email = email,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    suburb = suburb,
    city = city,
    province = province,
    postalCode = postalCode,
    logoUrl = logo,
    updatedAt = updatedAt.ifBlank { IsoTimestamp.format(Instant.now()) },
    deletedAt = null,
    syncState = SyncState.SYNCED,
    syncError = null,
    conflictServerJson = null,
)
