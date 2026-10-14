package com.gov.employeedb.presentation.employees

/**
 * The shared-content keys of the list -> detail transition (PLAN.md Phase-8 checklist).
 *
 * Both ends derive the key from the same employee id through these helpers, so the two screens can
 * never disagree about the `"employee-avatar-$id"` pattern and silently lose the transition.
 */
object EmployeeSharedElements {

    /** The key of the initials avatar travelling between the list card and the detail header. */
    fun avatar(id: Long): String = "employee-avatar-$id"

    /** The key of the full name, which cross-fades and resizes through `sharedBounds`. */
    fun name(id: Long): String = "employee-name-$id"
}
