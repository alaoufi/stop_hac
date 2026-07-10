package com.privacyshield.monitor.core.model

/**
 * Severity classification applied to every security event.
 *
 * The colour is only a summary — every event also carries a human-readable
 * [reason][SecurityEvent.reason] explaining *why* it landed in this bucket, as
 * required by the product spec ("توضيح سبب التصنيف وليس مجرد لون").
 */
enum class RiskLevel(val weight: Int) {
    /** 🟢 Expected, benign usage. */
    NORMAL(0),

    /** 🟡 Worth a glance — unusual but not necessarily harmful. */
    ATTENTION(1),

    /** 🟠 Suspicious — behaviour that commonly precedes abuse. */
    SUSPICIOUS(2),

    /** 🔴 Dangerous — strong signal of covert surveillance. */
    CRITICAL(3);

    val isElevated: Boolean get() = this == SUSPICIOUS || this == CRITICAL
}
