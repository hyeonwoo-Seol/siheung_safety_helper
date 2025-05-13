package com.example.safetyhelper.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.safetyhelper.Complaint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ComplaintRepository {
    private const val PREFS_NAME      = "complaints_prefs"
    private const val KEY_COMPLAINTS  = "complaints"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSample(ctx: Context) {
        val list = listOf(
            Complaint(1, "도로 파손 신고", "인근 도로에 큰 웅덩이가 생겼습니다."),
            Complaint(2, "가로등 고장 신고", "123번 가로등이 어제부터 꺼져 있습니다."),
            Complaint(3, "불법 주차 단속 요청", "우리 집 앞에 매일 불법 주차 차량이 있습니다.")
        )
        val json = Gson().toJson(list)
        prefs(ctx).edit().putString(KEY_COMPLAINTS, json).apply()
    }

    fun loadAll(ctx: Context): List<Complaint> {
        val json = prefs(ctx).getString(KEY_COMPLAINTS, "[]")
        val type = object : TypeToken<List<Complaint>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun findById(ctx: Context, id: Int): Complaint? =
        loadAll(ctx).firstOrNull { it.id == id }
}
