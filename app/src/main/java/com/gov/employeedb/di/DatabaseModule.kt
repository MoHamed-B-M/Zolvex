package com.gov.employeedb.di

import android.content.Context
import androidx.room.Room
import com.gov.employeedb.data.local.AppDatabase
import com.gov.employeedb.data.local.Migrations
import com.gov.employeedb.data.local.dao.EmployeeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Builds the singleton [AppDatabase] and exposes its DAO. Created lazily on first request. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "zolvex.db")
            // Migrations.ALL is empty at version 1; a destructive fallback stays banned because the
            // tables hold personal government data (PLAN.md §5.6).
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideEmployeeDao(database: AppDatabase): EmployeeDao = database.employeeDao()
}