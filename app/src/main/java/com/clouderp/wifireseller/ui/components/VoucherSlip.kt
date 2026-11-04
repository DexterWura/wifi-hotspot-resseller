package com.clouderp.wifireseller.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.channelLabel
import com.clouderp.wifireseller.data.formatIso
import com.clouderp.wifireseller.data.isOwnerAccess
import com.clouderp.wifireseller.data.isUsable
import com.clouderp.wifireseller.data.money
import com.clouderp.wifireseller.data.statusLabel
import com.clouderp.wifireseller.data.timeModeLabel
import com.clouderp.wifireseller.data.timeModeNote
import com.clouderp.wifireseller.ui.theme.VoucherCodeStyle
import com.clouderp.wifireseller.ui.util.copyText
import com.clouderp.wifireseller.ui.util.printVoucherSlip
import com.clouderp.wifireseller.ui.util.qrBitmap
import com.clouderp.wifireseller.ui.util.shareText

@Composable
fun VoucherSlip(
    voucher: VoucherDto,
    showQr: Boolean,
    onRevoke: (() -> Unit)? = null,
    extraActions: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val usable = voucher.isUsable()
    val owner = voucher.isOwnerAccess()
    val amount = if (owner) "Free" else money(voucher.currencyCode, voucher.amount)
    val timeValue = when {
        owner -> "This owner code does not expire."
        voucher.timeMode == "connected" -> timeModeNote(voucher.timeMode)
        else -> "Expires ${formatIso(voucher.expiresAt)}"
    }
    val shareBody = buildString {
        appendLine(voucher.site?.name ?: "Wi-Fi hotspot")
        appendLine("Code: ${voucher.code}")
        appendLine("Username = password = this code")
        appendLine("Plan: ${voucher.plan?.name ?: "—"}")
        append("Amount: $amount")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SoftCard {
            SectionLabel(voucher.site?.name ?: "Hotspot code")
            Text(
                if (usable) voucher.code else "Pending payment",
                style = VoucherCodeStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            MetaText(
                if (owner) {
                    "Staff hotspot code. It does not expire. Username and password are the same value."
                } else {
                    "Username and password are this code on the hotspot login."
                },
            )
            if (showQr && usable) {
                val qr = remember(voucher.code) { qrBitmap(voucher.code) }
                Image(
                    bitmap = qr,
                    contentDescription = "QR for ${voucher.code}",
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(160.dp)
                        .align(Alignment.Start),
                )
            }
            Column(Modifier.padding(top = 16.dp)) {
                DetailRow("Plan", voucher.plan?.name ?: "—")
                DetailRow("Amount", amount)
                DetailRow("Time", timeValue)
            }
        }

        if (usable) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxButton("Copy") { copyText(context, "Voucher", voucher.code) }
                BoxButton("Share") { shareText(context, "Wi-Fi code", shareBody) }
                BoxButton("Print") { printVoucherSlip(context, voucher) }
            }
        }
        extraActions?.invoke()

        SoftCard {
            DetailRow("Status", statusLabel(voucher.status))
            DetailRow("Channel", channelLabel(voucher.channel))
            DetailRow("Time mode", if (owner) "Does not expire" else timeModeLabel(voucher.timeMode))
            DetailRow("Guest phone", voucher.guestPhone ?: "—")
            DetailRow("Sold by", voucher.issuer?.name ?: "—")
            DetailRow("Activated", formatIso(voucher.activatedAt))
            DetailRow("Expires", if (owner) "Does not expire" else formatIso(voucher.expiresAt))
            val sync = buildString {
                append(voucher.syncStatus ?: "—")
                if (voucher.removalPending) append(" (removal pending)")
            }
            DetailRow("Router sync", sync)
            if (voucher.bytesIn != null || voucher.bytesOut != null) {
                DetailRow("Bytes in", (voucher.bytesIn ?: 0).toString())
                DetailRow("Bytes out", (voucher.bytesOut ?: 0).toString())
            }
        }

        if (onRevoke != null && voucher.status == "active") {
            DangerButton("Revoke voucher", onClick = onRevoke)
        }
    }
}

@Composable
private fun RowScope.BoxButton(text: String, onClick: () -> Unit) {
    Box(Modifier.weight(1f)) {
        SecondaryButton(text, onClick = onClick)
    }
}
