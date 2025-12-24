package com.polstat.simcat.api

import com.polstat.simcat.auth.AuthRequest
import com.polstat.simcat.auth.AuthResponse
import com.polstat.simcat.auth.ChangePasswordRequest
import com.polstat.simcat.model.User
import com.polstat.simcat.model.Equipment
import com.polstat.simcat.model.Borrow
import com.polstat.simcat.model.Schedule
import com.polstat.simcat.model.Participation
import com.polstat.simcat.model.MessageResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== AUTH ====================

    @POST("register")
    suspend fun register(@Body request: User): Response<User>

    @POST("login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // ==================== USER ====================

    @GET("api/users/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @PUT("api/users/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body user: User
    ): Response<User>

    @PUT("api/users/change-password")
    @Headers("Content-Type: application/json")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    @DELETE("api/users/delete-account")
    suspend fun deleteAccount(@Header("Authorization") token: String): Response<MessageResponse>

    // ==================== EQUIPMENT ====================

    @GET("api/equipment/getall")
    suspend fun getAllEquipment(): Response<List<Equipment>>

    @POST("api/equipment/create")
    suspend fun createEquipment(
        @Header("Authorization") token: String,
        @Body equipment: Equipment
    ): Response<MessageResponse>

    @PUT("api/equipment/{id}")
    suspend fun updateEquipment(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body equipment: Equipment
    ): Response<MessageResponse>

    @DELETE("api/equipment/{id}")
    suspend fun deleteEquipment(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>

    // ==================== BORROW ====================

    @POST("api/borrows/borrow")
    suspend fun borrowEquipment(
        @Header("Authorization") token: String,
        @Body borrow: Borrow
    ): Response<Borrow>  // ✅ Ubah dari String ke Borrow

    @POST("api/borrows/return/{id}")
    suspend fun returnEquipment(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Borrow>  // ✅ Ubah dari String ke Borrow

    @GET("api/borrows/getall")
    suspend fun getAllBorrows(@Header("Authorization") token: String): Response<List<Borrow>>

    @GET("api/borrows/user/{userId}")
    suspend fun getBorrowsByUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<List<Borrow>>

    // ==================== SCHEDULE ====================

    @GET("api/schedules/getall")
    suspend fun getAllSchedules(): Response<List<Schedule>>

    @POST("api/schedules/create")
    suspend fun createSchedule(
        @Header("Authorization") token: String,
        @Body schedule: Schedule
    ): Response<Schedule>

    @PUT("api/schedules/{id}")
    suspend fun updateSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body schedule: Schedule
    ): Response<MessageResponse>

    @DELETE("api/schedules/delete/{id}")
    suspend fun deleteSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>
    // ==================== PARTICIPATION ====================

    @POST("api/participations/register")
    suspend fun registerParticipation(
        @Header("Authorization") token: String,
        @Body participation: Participation
    ): Response<Participation>

    @PUT("api/participations/cancel/{id}")
    suspend fun cancelParticipation(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body participation: Participation
    ): Response<String>

    @GET("api/participations/schedule/{scheduleId}")
    suspend fun getParticipationsBySchedule(
        @Header("Authorization") token: String,
        @Path("scheduleId") scheduleId: Long
    ): Response<List<Participation>>

    @GET("api/participations/user/{userId}")
    suspend fun getParticipationsByUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<List<Participation>>
}