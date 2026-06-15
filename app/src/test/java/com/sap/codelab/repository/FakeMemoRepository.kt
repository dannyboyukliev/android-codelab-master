package com.sap.codelab.repository

import com.sap.codelab.model.Memo

internal class FakeMemoRepository : IMemoRepository {

    val memos = mutableListOf<Memo>()

    override fun saveMemo(memo: Memo) {
        memos.removeAll { it.id == memo.id }
        memos.add(memo)
    }

    override fun getAll(): List<Memo> = memos.toList()

    override fun getOpen(): List<Memo> = memos.filter { !it.isDone }

    override fun getMemoById(id: Long): Memo = memos.first { it.id == id }
}
