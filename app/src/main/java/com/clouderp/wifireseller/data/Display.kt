package com.clouderp.wifireseller.data

fun money(currency: String?, amount: Double): String =
    "${currency ?: "USD"} ${"%.2f".format(amount)}"

fun moneyLines(rows: List<MoneyTotal>): String {
    if (rows.isEmpty()) return "0.00"
    return rows.joinToString("\n") { money(it.currencyCode, it.total) }
}

fun timeModeLabel(mode: String?): String = when (mode) {
    "connected" -> "Connected time"
    "wall_clock" -> "Wall clock"
    else -> "—"
}

fun timeModeNote(mode: String?): String = when (mode) {
    "connected" -> "Time counts only while the guest is online."
    "wall_clock" -> "Time starts at sale and keeps running offline."
    else -> ""
}

fun channelLabel(channel: String?): String = when (channel) {
    "cash" -> "Cash"
    "paynow" -> "Paynow"
    "pesepay" -> "Pesepay"
    "ecocash" -> "EcoCash"
    "owner" -> "Owner"
    else -> channel?.replaceFirstChar { it.uppercase() } ?: "—"
}

fun statusLabel(status: String?): String = when (status) {
    "pending_payment" -> "Pending payment"
    "active" -> "Active"
    "expired" -> "Expired"
    "revoked" -> "Revoked"
    else -> status?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "—"
}

fun deviceLabel(sharedUsers: Int): String =
    if (sharedUsers <= 1) "1 device" else "$sharedUsers devices"

fun cashIssueLabel(mode: String?): String = when (mode) {
    "auto" -> "Auto print"
    "manual" -> "Manual"
    else -> "Preview slip"
}

fun cashIssueDescription(mode: String?): String = when (mode) {
    "auto" -> "After a cash sale, open the voucher slip and send it to the Android print sheet."
    "manual" -> "After a cash sale, open the voucher slip. Print later from that page when you need a copy."
    else -> "After a cash sale, open the voucher slip so staff can print when ready."
}

fun formatIso(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return value.replace('T', ' ').take(16).replace('Z', ' ').trim()
}

fun VoucherDto.isOwnerAccess(): Boolean = channel == "owner"

fun VoucherDto.isUsable(): Boolean = status == "active" && code.isNotBlank()

fun PlanDto.planMeta(): String = listOfNotNull(
    durationLabel ?: if (isSellable) "$durationMinutes min" else "Does not expire",
    timeModeLabel(timeMode).takeIf { isSellable },
    dataLimitLabel ?: "Unlimited",
    deviceLabel(sharedUsers),
    site?.name?.let { "$it only" },
).joinToString(" · ")
