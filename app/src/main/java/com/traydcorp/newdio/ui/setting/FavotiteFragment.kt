package com.traydcorp.newdio.ui.setting

import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.JsonObject
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.Favorites
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.FragmentFavotiteBinding
import com.traydcorp.newdio.ui.discover.DiscoverDetailFragment
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit


class FavotiteFragment : Fragment() {

    private var viewBinding : FragmentFavotiteBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private lateinit var adapter: FavoriteAdapter

    private var bundle = Bundle()

    private lateinit var callback: OnBackPressedCallback

    private lateinit var result: Favorites
    private var isEmpty = false

    private var loadingDialog = LoadingFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        language = (activity as HomeActivity).getLanguagePreference()

        // access token, refresh token
        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

        val bottomNavi = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavi?.visibility = View.GONE

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentFavotiteBinding.inflate(inflater, container, false)

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")


        if (access != null && refresh != null) {
            getFavorites(supplementService.getFavorite(access, refresh, language))
        }

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            }
        }



        return bind.root
    }

    override fun onDetach() {
        super.onDetach()
        val bottomNavi = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavi?.visibility = View.VISIBLE
    }

    // 보관함 api
    private fun getFavorites(service: Call<Favorites>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<Favorites> {
                override fun onResponse(call: Call<Favorites>, response: Response<Favorites>) {
                    if (response.code() == 200){
                        result = response.body()!!

                        isEmpty = emptyCheck() // 3개 리스트 모두 비었는지 체크
                        if(!isEmpty){
                            favoriteRecyclerView(result.interested_companies, null)
                            favoriteRecyclerView(null, result.interested_industries)
                            favoriteNewsRecyclerView(result.liked_news)
                            // 없는 리스트 view 숨기기
                            if (result.interested_companies.isEmpty()){
                                bind.companyTitle.visibility = View.GONE
                            }
                            if (result.interested_industries.isEmpty()){
                                bind.industryTitle.visibility = View.GONE
                            }
                            if (result.liked_news.isEmpty()){
                                bind.NewsTitle.visibility = View.GONE
                            }
                        }

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }


                    }
                    if (response.code() == 401) {
                        // 잘못된 토큰 팝업 메세지
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setMessage(getString(R.string.error_401_content))
                            .setTitle(getString(R.string.error_401_title))
                            .setPositiveButton(getString(R.string.popup_confirm),
                                DialogInterface.OnClickListener { dialog, id ->
                                })
                        val alertDialog = builder.create()
                        alertDialog.show()
                    }
                    if (response.code() == 404) {
                        // 회원 없음
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setMessage(getString(R.string.error_user_content))
                            .setTitle(getString(R.string.error_user_title))
                            .setPositiveButton(getString(R.string.popup_confirm),
                                DialogInterface.OnClickListener { dialog, id ->
                                })
                        val alertDialog = builder.create()
                        alertDialog.show()
                    }
                }

                override fun onFailure(call: Call<Favorites>, t: Throwable) {
                    Log.d("onFailure", t.message.toString())
                    errorFragment(null)
                }

            })
        }, 1000)
    }

    // 기업 산업 recyclerView
    private fun favoriteRecyclerView(companies: ArrayList<CompanyDetail>?, industries: ArrayList<IndustryDetail>?) {

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        if (companies != null) {
            adapter = FavoriteAdapter(companies, null, null)
            bind.favoriteCompaniesRcy.adapter = adapter
            bind.favoriteCompaniesRcy.layoutManager = layoutManager
        }
        if (industries != null) {
            adapter = FavoriteAdapter(null, industries, null)
            bind.favoriteIndustryRcy.adapter = adapter
            bind.favoriteIndustryRcy.layoutManager = layoutManager
        }

        adapter.setItemClickListener(object : FavoriteAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String?, heart: String?, industry: String?) {
                if (heart == null) {
                    if (industry == "industry") {
                        bundle.putString("key", "industry")
                    }
                    bundle.putString("id", id)
                    bundle.putString("from", "favorite")
                    (activity as HomeActivity).getDiscoverDetail(bundle)
                }
                // 좋아요 취소
                if (heart != null) {
                    favoriteLikes(supplementService.discoverDetailLikes(access, refresh, id))

                    if (companies != null) {
                        companies.removeAt(position)
                        if (companies.isEmpty()){
                            bind.companyTitle.visibility = View.GONE
                            getEmptyMessage(emptyCheck())
                        }
                    }

                    if (industries != null) {
                        industries.removeAt(position)
                        if (industries.isEmpty()){
                            bind.industryTitle.visibility = View.GONE
                            getEmptyMessage(emptyCheck())
                        }
                    }

                }
            }
        })



    }

    // 뉴스 recyclerView
    private fun favoriteNewsRecyclerView(likedNews: ArrayList<NewsDetail>) {

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = FavoriteAdapter(null, null, likedNews)
        bind.favoriteNewsRcy.adapter = adapter
        bind.favoriteNewsRcy.layoutManager = layoutManager

        getEmptyMessage(isEmpty)
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }

        adapter.setItemClickListener(object : FavoriteAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String?, heart: String?, industry: String?) {
                val crawlingdata = id!!.toInt()
                bundle.putInt("crawlingdata", crawlingdata)
                bundle.putString("from", "favorite")

                if (heart == null) { // 기사 선택
                    (activity as HomeActivity).getPlayer(bundle)
                }
                if (heart != null) { // 하트 선택
                    favoriteLikes(supplementService.playerLikes(access, refresh, crawlingdata))
                    likedNews.removeAt(position)
                    if (likedNews.isEmpty()){
                        bind.NewsTitle.visibility = View.GONE
                        getEmptyMessage(emptyCheck())
                    }

                }

            }
        })



    }

    // 기업 산업 likes api 호출
    private fun favoriteLikes(service: Call<JsonObject>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.code() == 200){
                        Log.d("response body code", response.body()?.get("code").toString())
                    }

                    // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                    val newAccess = response.headers()["Authorization"]
                    if (newAccess != null) {
                        sharedPreferences.setShared("access_token", newAccess, requireContext())
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.d("onFailure", t.message.toString())
                    errorFragment(null)
                }

            })
        }, 1000)
    }

    // 에러 페이지
    private fun errorFragment(serverError : String?) {
        bind.errorFragment.visibility = View.VISIBLE
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }

        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)

        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            if (access != null && refresh != null) {
                getFavorites(supplementService.getFavorite(access, refresh, language))
            }
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
            refreshView()
        }
    }

    private fun refreshView() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()

        bind.errorFragment.visibility = View.GONE
    }

    // 리스트 3개 모두 비었는지 체크
    private fun emptyCheck() : Boolean {
        return result.interested_companies.isEmpty() && result.interested_companies.isEmpty() &&
                result.liked_news.isEmpty()
    }

    // 저장 내역 없을 때 메세지
    private fun getEmptyMessage(isEmpty: Boolean) {
        if (isEmpty) {
            bind.emptyMessage.visibility = View.VISIBLE
            bind.scrollView.visibility = View.GONE
        } else {
            bind.scrollView.visibility = View.VISIBLE
        }
    }

}