package ai.doma.miniappdemo.data

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface RetrofitApi {
    @GET
    @Headers(
        "cache-control: no-cache",
        "accept: */*",
        "accept-encoding: gzip, deflate, br",
        "user-agent: CordovaDemoApp/1 CFNetwork/1331.0.7 Darwin/21.2.0"
    )
    suspend fun miniappRequestAuthCode(
        @Url url: String,
        @Header("Cookie") cookie: String
    ): Response<ResponseBody>

    @POST
    @Headers(
        "Content-Type: application/x-www-form-urlencoded",
        "Response-Type: application/json",
        "Accept: application/json",
    )
    suspend fun miniappRequestAuth(
        @Url url: String,
        //@Header("Cookie") cookie: String,
        @Header("Authorization") authorizationHeader: String,
        @Body body: RequestBody
    ): ResponseBody


    @GET
    @Headers(
        "cache-control: no-cache",
        "accept: */*",
        "accept-encoding: gzip, deflate, br",
        "user-agent: CordovaDemoApp/1 CFNetwork/1331.0.7 Darwin/21.2.0"
    )
    suspend fun miniappRequestFullAuth(
        @Url url: String,
        @Header("Cookie") cookie: String
    ): Response<ResponseBody>

    companion object {

    }
}