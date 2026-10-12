package com.gov.employeedb.di

import javax.inject.Qualifier

/** Injected I/O-bound dispatcher (never a hardcoded `Dispatchers.IO` — PLAN.md ADR-8). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Injected CPU-bound dispatcher for pure computations (analytics, bucket maths). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Process-wide [kotlinx.coroutines.CoroutineScope]; replaces `GlobalScope` everywhere. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope