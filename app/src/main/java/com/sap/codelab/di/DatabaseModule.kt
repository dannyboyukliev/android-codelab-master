package com.sap.codelab.di

import android.content.Context
import androidx.room.Room
import com.sap.codelab.repository.Database
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.repository.MemoDao
import com.sap.codelab.repository.Repository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindMemoRepository(repository: Repository): IMemoRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): Database {
            return Room.databaseBuilder(context, Database::class.java, "codelab").build()
        }

        @Provides
        fun provideMemoDao(database: Database): MemoDao {
            return database.getMemoDao()
        }
    }
}
