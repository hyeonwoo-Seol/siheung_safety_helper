package com.example.safetyhelper

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://220.120.3.8:9000/") // 에뮬레이터라면 10.0.2.2, 실기기라면 서버 IP
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

interface ApiService {
    @POST("/generate")  // FastAPI 에 정의한 엔드포인트
    suspend fun getLLMResponse(
        @Body request: ApiRequest
    ): Response<ApiResponse>
}