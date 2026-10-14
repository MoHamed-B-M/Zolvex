package com.gov.employeedb.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.gov.employeedb.R
import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.ServiceBucket

/**
 * Bucket → localized label (PLAN.md Phase-6 checklist). Keeping this mapping in the presentation
 * layer is what stops the domain enums from ever holding a resource id.
 */
@StringRes
fun AgeBucket.labelRes(): Int = when (this) {
    AgeBucket.UNDER_25 -> R.string.age_under_25
    AgeBucket.B25_34 -> R.string.age_25_34
    AgeBucket.B35_44 -> R.string.age_35_44
    AgeBucket.B45_54 -> R.string.age_45_54
    AgeBucket.B55_PLUS -> R.string.age_55_plus
}

/** Service bucket → localized label. */
@StringRes
fun ServiceBucket.labelRes(): Int = when (this) {
    ServiceBucket.LESS_5 -> R.string.service_less_5
    ServiceBucket.B5_10 -> R.string.service_5_10
    ServiceBucket.B10_15 -> R.string.service_10_15
    ServiceBucket.B15_20 -> R.string.service_15_20
    ServiceBucket.B20_PLUS -> R.string.service_20_plus
}

/**
 * The resource id for a raw bucket label ([com.gov.employeedb.domain.model.BucketCount.label]), or
 * `null` when the label is free text with no translation. Every chart and legend receives whichever
 * distribution it is given, so it has to resolve an enum name and a department name the same way.
 *
 * Two spellings are not data at all and are translated wherever they appear: [WorkforceAnalytics.OTHER_LABEL],
 * which the engine appends when a distribution is rolled up, and the empty label, which stands for a
 * row that simply has no department or grade. A real department or grade name is returned as `null`
 * and so passes through untouched.
 */
@StringRes
fun bucketLabelRes(label: String): Int? {
    AgeBucket.entries.firstOrNull { it.name == label }?.let { return it.labelRes() }
    ServiceBucket.entries.firstOrNull { it.name == label }?.let { return it.labelRes() }
    return when (label) {
        WorkforceAnalytics.OTHER_LABEL -> R.string.other
        "" -> R.string.unspecified
        else -> null
    }
}

/** The localized display label for a raw bucket [label], falling back to the label as stored. */
fun bucketLabel(context: Context, label: String): String =
    bucketLabelRes(label)?.let { context.getString(it) } ?: label

/**
 * Employee field → localized label (presentation layer). Keeps the domain enum free of resource IDs.
 */
@StringRes
fun EmployeeField.labelRes(): Int = when (this) {
    EmployeeField.FULL_NAME -> R.string.full_name
    EmployeeField.DATE_OF_BIRTH -> R.string.date_of_birth
    EmployeeField.HIRE_DATE -> R.string.hire_date
    EmployeeField.DEPARTMENT -> R.string.department
    EmployeeField.POSITION -> R.string.position
    EmployeeField.GRADE -> R.string.grade
    EmployeeField.SALARY -> R.string.salary
    EmployeeField.EMAIL -> R.string.email
    EmployeeField.PHONE -> R.string.phone
    EmployeeField.ADDRESS -> R.string.address
    EmployeeField.NOTES -> R.string.notes
}
