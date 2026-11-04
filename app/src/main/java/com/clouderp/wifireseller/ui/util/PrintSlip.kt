package com.clouderp.wifireseller.ui.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.channelLabel
import com.clouderp.wifireseller.data.formatIso
import com.clouderp.wifireseller.data.isOwnerAccess
import com.clouderp.wifireseller.data.money
import com.clouderp.wifireseller.data.timeModeLabel

fun printVoucherSlip(context: Context, voucher: VoucherDto) {
    val html = buildString {
        append("<html><head><meta charset='utf-8'><style>")
        append("body{font-family:sans-serif;padding:24px;color:#12241f}")
        append("h1{font-family:monospace;letter-spacing:4px;font-size:28px}")
        append("p,dt,dd{font-size:14px} dl{display:grid;gap:6px}")
        append("dt{color:#4a5c56} dd{margin:0;font-weight:600}")
        append("</style></head><body>")
        append("<p>${escape(voucher.site?.name ?: "Hotspot")}</p>")
        append("<h1>${escape(voucher.code)}</h1>")
        append("<p>Username = password = this code</p><dl>")
        append("<div><dt>Plan</dt><dd>${escape(voucher.plan?.name ?: "—")}</dd></div>")
        val amount = if (voucher.isOwnerAccess()) "Free" else money(voucher.currencyCode, voucher.amount)
        append("<div><dt>Amount</dt><dd>$amount</dd></div>")
        val time = when {
            voucher.isOwnerAccess() -> "Does not expire"
            voucher.timeMode == "connected" -> "Connected time"
            else -> "Expires ${escape(formatIso(voucher.expiresAt))}"
        }
        append("<div><dt>Time</dt><dd>$time</dd></div>")
        append("<div><dt>Channel</dt><dd>${escape(channelLabel(voucher.channel))}</dd></div>")
        append("<div><dt>Time mode</dt><dd>${escape(if (voucher.isOwnerAccess()) "Does not expire" else timeModeLabel(voucher.timeMode))}</dd></div>")
        append("</dl></body></html>")
    }
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printer = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val job = view.createPrintDocumentAdapter("Voucher ${voucher.code}")
            printer.print("wifi-voucher-${voucher.id}", job, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

private fun escape(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
