package com.traydcorp.newdio.dataModel

import java.io.Serializable
import kotlin.collections.ArrayList

data class NewsDetail(
    var crawlingdata : Int = 0,
    var image_url : String? = null,
    var title : String? = null,
    var post_date : String? = null,
    var eng_title : String? = null,
    var eng_content : String? = null,
    var language_type : String? = null,
    var summarized_content : String? = null,
    var related_category : String? = null,
    var text_sentiment : Float? = null,
    var long_summarized_content : String? = null,
    var audio_file : String? = null,
    var news_url : String? = null,
    var news_site : String? = null,
    var likes : Int = 0,
    var user_likes : Boolean = false,
    var related_news_list : ArrayList<NewsDetail>? = null,
    var isPlaying : Boolean = false,
    var related_company_list : ArrayList<CompanyDetail>? = null
) : Serializable

data class CompanyDetail(
    var index : String? = null,
    var company : String? = null,
    var description : String? = null,
    var logo_url : String? = null,
    var user_likes : Boolean = false,
    var abbreviation_company_name : String? = null,
    var related_industry : String? = null,
    var rankNo : Int = 0,
    var rank : Int = 0,
    var rank_change : String? = null,
    var likes : Int = 0,
    var isSelected : Boolean = false,
    var isCreated : Boolean = false
)

data class IndustryDetail(
    var index : String? = null,
    var industry : String? = null,
    var user_likes : Boolean = false,
    var logo_url : String? = null,
    var rankNo : Int = 0,
    var rank : Int = 0,
    var rank_change : String? = null,
    var likes : Int = 0,
    var isSelected : Boolean = false,
    var company_list: ArrayList<CompanyDetail>? = null
)

data class RankingList(
    var created : String? = null,
    var company_list : ArrayList<CompanyDetail>? = null,
    var industry_list : ArrayList<IndustryDetail>? = null,
    var company_ranking : ArrayList<CompanyDetail>? = null
)

data class HomeNewsList (
    var index : String? = null,
    var category_name : String? = null,
    var category : String? = null,
    var newsList : ArrayList<NewsDetail>? = null
)

data class User(
    var id : String,
    var username : String,
    var email : String,
    var gender : String,
    var provider : String,
    var birthday : String,
    var last_login : String,
    var date_joined : String
)

data class Favorites(
    var interested_companies : ArrayList<CompanyDetail>,
    var interested_industries : ArrayList<IndustryDetail>,
    var liked_news : ArrayList<NewsDetail>

)

data class ReportBody(
    var report_type : String,
    var description : String
)

data class ResponseBody(
    var response : String?,
    var message : String?,
    var is_user : String?,
    var detail : String?
)

data class SocialToken (
    var access_token: String? = null
)