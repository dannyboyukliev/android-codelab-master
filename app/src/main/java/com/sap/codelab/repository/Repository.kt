package com.sap.codelab.repository

import androidx.annotation.WorkerThread
import com.sap.codelab.model.Memo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Repository @Inject constructor(private val dao: MemoDao) : IMemoRepository {

    @WorkerThread
    override fun saveMemo(memo: Memo): Long = dao.insert(memo)

    @WorkerThread
    override fun getOpen(): List<Memo> = dao.getOpen()

    @WorkerThread
    override fun getAll(): List<Memo> = dao.getAll()

    @WorkerThread
    override fun getMemoById(id: Long): Memo = dao.getMemoById(id)
}
