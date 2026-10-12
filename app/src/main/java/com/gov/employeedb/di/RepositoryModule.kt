package com.gov.employeedb.di

import com.gov.employeedb.data.repository.EmployeeRepositoryImpl
import com.gov.employeedb.domain.repository.EmployeeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Contract bindings: use cases depend on the interfaces, never on the Room implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEmployeeRepository(impl: EmployeeRepositoryImpl): EmployeeRepository
}