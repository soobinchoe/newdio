package com.traydcorp.newdio.utils.retofitService

import com.google.gson.JsonObject
import com.traydcorp.newdio.dataModel.*
import com.traydcorp.newdio.model.MemberVO
import retrofit2.http.*

interface RetrofitService {
    // 로그인
    @Headers("Content-Type: application/json; charset=UTF-8")
    @POST("app/accounts/social/{socialname}/signin/")
    fun login (
        @Body access_token: SocialToken,
        @Path("socialname") socialname : String?
    ) : retrofit2.Call<JsonObject>

    // 유저 정보 조회
    @Headers("Content-Type: application/json; charset=UTF-8")
    @GET("app/accounts/users/me/")
    fun getUser (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Query("language") language : String
    ) : retrofit2.Call<User>

    // 유저 정보 삭제
    @Headers("Content-Type: application/json; charset=UTF-8")
    @DELETE("app/accounts/users/me/")
    fun deleteUser (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?
    ) : retrofit2.Call<JsonObject>

    // 보관함
    @Headers("Content-Type: application/json; charset=UTF-8")
    @GET("app/accounts/users/me/storage/")
    fun getFavorite (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Query("language") language : String
    ) : retrofit2.Call<Favorites>



    // 회원가입
    @Headers("Content-Type: application/json; charset=UTF-8")
    @POST("app/accounts/social/{socialname}/signup/")
    fun register (
        @Body memberVO: MemberVO,
        @Path("socialname") socialname : String?
    ) : retrofit2.Call<JsonObject>

    // 산업 기업 리스트
    @Headers("Content-Type: application/json; charset=UTF-8")
    @GET("app/groups/total/companies/")
    fun interestedCompanyList (
        @Query("language") language : String
    ) : retrofit2.Call<List<IndustryDetail>>



    // 홈 뉴스 데이터 조회
    @GET("app/crawling/home/")
    fun getHomeNewsData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Query("language") language : String
    ) : retrofit2.Call<List<HomeNewsList>>

    // player데이터 조회
    @GET("app/crawling/processed-datas/{pk}/")
    fun getPlayerData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") crawlingdata : Int,
        @Query("language") language : String
    ) : retrofit2.Call<NewsDetail>

    // player list 데이터 조회
    @GET("app/crawling/processed-datas/{pk}/relation/")
    fun getPlayerListData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") crawlingdata : Int,
        @Query("language") language : String
        ) : retrofit2.Call<NewsDetail>

    // player list 더보기 데이터 조회
    @GET("app/crawling/processed-datas/next-relation/")
    fun getPlayerNextListData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Query("language") language : String,
        @Query("exclude-list", encoded = true) excludeList : String,
        @Query("related-list", encoded = true) relatedList : String
    ) : retrofit2.Call<List<NewsDetail>>

    // player 좋아요 요청
    @POST("app/crawling/processed-datas/{pk}/like/")
    fun playerLikes (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") crawlingdata : Int
    ) : retrofit2.Call<JsonObject>


    // Live 데이터 조회
    @GET("app/live/")
    fun getLiveData (
        //@Header("access_token") access : String,
        //@Header("refresh_token") refresh : String
        @Query("last-id") lastId : Int?,
        @Query("language") language : String
    ) : retrofit2.Call<List<NewsDetail>>

    // live player
    @GET("app/live/{pk}")
    fun getLivePlayerListData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") crawlingdata : Int,
        @Query("language") language : String
    ) : retrofit2.Call<NewsDetail>


    // 둘러보기 추천 기업 조회
    @GET("/app/groups/recommand-company/")
    fun getRecommendedCompanyData (
        @Query("language") language : String
    ) : retrofit2.Call<List<CompanyDetail>>

    // 둘러보기 일간 기업 조회
    @GET("/app/ranking/company/")
    fun getDailyCompanyRankingData (
        @Query("language") language : String
    ) : retrofit2.Call<RankingList>

    // 둘러보기 실시간 기업/산업 조회
    @GET("app/ranking/list/")
    fun getRankingListData (
        @Query("language") language : String
    ) : retrofit2.Call<RankingList>

    // 둘러보기 전체 산업 리스트 조회
    @GET("app/groups/total/industries/")
    fun getTotalIndustryData (
        @Query("language") language : String
    ) : retrofit2.Call<List<IndustryDetail>>

    // 상세보기 관련 company 조회
    @GET("app/groups/companies/{pk}/")
    fun getDiscoverDetailCompanyData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") company_index : String,
        @Query("language") language : String
    ) : retrofit2.Call<CompanyDetail>

    // 상세보기 관련 뉴스 조회
    @GET("app/groups/companies/{pk}/related-news/")
    fun getDiscoverDetailCompanyNewsData (
        @Path("pk") company_index : String,
        @Query("language") language : String,
        @Query("last-id") lastId : Int?
    ) : retrofit2.Call<List<NewsDetail>>

    // 상세보기 관련 industry 조회
    @GET("app/groups/industries/{pk}/")
    fun getDiscoverDetailIndustryData (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") index : String,
        @Query("language") language : String
    ) : retrofit2.Call<IndustryDetail>

    // 상세보기 관련 뉴스 조회
    @GET("app/groups/industries/{pk}/related-news/")
    fun getDiscoverDetailIndustryNewsData (
        @Path("pk") company_index : String,
        @Query("language") language : String,
        @Query("last-id") lastId : Int?
    ) : retrofit2.Call<List<NewsDetail>>

    // 상세보기 좋아요 요청
    @POST("app/groups/{index}/like/")
    fun discoverDetailLikes (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("index") index : String?
    ) : retrofit2.Call<JsonObject>


    // 검색 - 키워드 검색
    @GET("app/searches")
    fun getSearchResult (
        @Query("search-word") searchWord : String,
        @Query("language") language : String,
        @Query("last-id") lastId : Int?
    ) : retrofit2.Call<List<NewsDetail>>

    // 신고하기
    @POST("app/crawling/processed-datas/{pk}/report/")
    fun reportNews (
        @Header("Authorization") access : String?,
        @Header("Refresh-Token") refresh : String?,
        @Path("pk") index : Int,
        @Body reportBody : ReportBody
    ) : retrofit2.Call<JsonObject>

}