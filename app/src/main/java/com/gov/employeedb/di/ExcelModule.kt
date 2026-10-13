package com.gov.employeedb.di

import com.gov.employeedb.data.excel.ExcelReader
import com.gov.employeedb.data.excel.ExcelWriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Binds the Excel engine singletons. Both services are stateless; [ExcelReader] needs the injected
 * I/O dispatcher so no hardcoded `Dispatchers.IO` appears in the data layer (PLAN.md ADR-8).
 */
@Module
@InstallIn(SingletonComponent::class)
object ExcelModule {

    @Provides
    @Singleton
    fun provideExcelReader(@IoDispatcher dispatcher: CoroutineDispatcher): ExcelReader =
        ExcelReader(dispatcher)

    @Provides
    @Singleton
    fun provideExcelWriter(): ExcelWriter = ExcelWriter
}