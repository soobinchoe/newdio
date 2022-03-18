package com.traydcorp.newdio.ui.search

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.widget.SearchView
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.traydcorp.newdio.databinding.FragmentSearchBinding
import com.traydcorp.newdio.ui.setting.DialogFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Retrofit
import com.traydcorp.newdio.R
import com.traydcorp.newdio.ui.home.HomeActivity


class SearchFragment : Fragment() {

    private var viewBinding : FragmentSearchBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private lateinit var language : String

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var fadeInAnimation : Animation
    private lateinit var fadeOutAnimation : Animation

    private lateinit var searchWord : String
    private var recentSearchList : ArrayList<String>? = null
    private lateinit var adapter : SearchAdapter

    var isSearchOn = false

    private lateinit var bottomNavi : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        language = (activity as HomeActivity).getLanguagePreference()

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSearchBinding.inflate(inflater, container, false)


        bottomNavi = activity?.findViewById(R.id.bottomNavigation)!!


        // view change 애니메이션
        fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 200
        fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
        fadeOutAnimation.duration = 200

        // 검색 버튼 누르면 view 전환
        bind.searchBtn.setOnClickListener {
            // 최근 검색어가 있으면
            if (sharedPreferences.getRecentSearch(requireContext(), "recentSearch")?.isNullOrEmpty() == false){
                recentSearchList = sharedPreferences.getRecentSearch(requireContext(), "recentSearch")
                Log.d("recent", recentSearchList.toString())
                recentSearchList?.let { recentSearchRecyclerView(it) }
                bind.recentSearch.visibility = View.VISIBLE
            } else {
                bind.recentSearch.visibility = View.GONE
                bind.noSearchHistory.visibility = View.VISIBLE
            }
            isSearchOn = true
            bind.searchBefore.visibility = View.GONE

            bind.searchHistory.visibility = View.VISIBLE
            bind.searchHistory.animation = fadeInAnimation
            bind.searchHistory.animation.start()

            bind.searchBar.isFocusable = true
            bind.searchBar.requestFocus()
            bind.searchBar.isIconified = false
            bind.searchBar.requestFocusFromTouch()

            bottomNavi.visibility = View.GONE


            // 검색 취소 버튼
            bind.cancelBtn.setOnClickListener {
                searchBarOff()
                bottomNavi.visibility = View.VISIBLE
            }
        }
        

        // 텍스트 입력 후 검색
        bind.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 검색 api call
                if (query != null) {
                    searchWord = query

                    bind.noSearchHistory.visibility = View.GONE
                    bind.searchHistory.visibility = View.GONE
                    bind.searchBefore.visibility = View.VISIBLE
                    isSearchOn = false

                    val bundle = Bundle()
                    bundle.putString("searchWord", searchWord)
                    val searchResultFragment = SearchResultFragment()
                    searchResultFragment.arguments = bundle
                    requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                        .add(R.id.homeView, searchResultFragment, "searchResult").commit()

                    // 검색어 초기화
                    bind.searchBar.setQuery("", false)
                    bind.searchBar.clearFocus()
                    bottomNavi.visibility = View.VISIBLE
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })


        // 전체 삭제
        bind.deleteAll.setOnClickListener {
            val dialog = DialogFragment()
            val bundle = Bundle()
            bundle.putString("key", "DeleteAll")
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "dialog")
        }

        return bind.root
    }


    // 전체 삭제 activity에서 call
    fun deleteAll() {
        recentSearchList?.clear()
        sharedPreferences.sharedClear(requireContext(), "recentSearch")
        bind.recentSearch.visibility = View.GONE
        bind.noSearchHistory.visibility = View.VISIBLE
    }

    // 최근 검색어 recyclerView
    private fun recentSearchRecyclerView(it: ArrayList<String>) {
        adapter = SearchAdapter(it)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        bind.recentSearchRcy.adapter = adapter
        bind.recentSearchRcy.layoutManager = layoutManager

        adapter.setItemClickListener(object : SearchAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, search: String?) {
                if (search != null) { // 삭제 선택
                    recentSearchList!!.remove(search)
                    adapter.notifyItemRemoved(position)
                    sharedPreferences.setRecentSearch("recentSearch", recentSearchList!!, requireContext())

                    if (recentSearchList!!.isEmpty()) {
                        bind.recentSearch.visibility = View.GONE
                        bind.noSearchHistory.visibility = View.VISIBLE
                    }
                } else { // 검색어 선택
                    searchWord = it[position]
                    sharedPreferences.setRecentSearch("recentSearch", recentSearchList!!, requireContext())
                    bind.searchHistory.visibility = View.GONE
                    bind.searchBefore.visibility = View.VISIBLE
                    isSearchOn = false

                    val bundle = Bundle()
                    bundle.putString("searchWord", searchWord)
                    val searchResultFragment = SearchResultFragment()
                    searchResultFragment.arguments = bundle
                    requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                        .add(R.id.homeView, searchResultFragment, "searchResult").commit()
                }

            }
        })
    }


    // 검색 화면 종료
    fun searchBarOff() {
        if (bind.searchHistory.isShown){
            bind.searchHistory.visibility = View.GONE
            bind.searchBefore.visibility = View.VISIBLE
            bind.searchBefore.animation = fadeInAnimation
            bind.searchBefore.animation.start()
            isSearchOn = false
        }
    }

    // 검색어 response code 200일때 검색 리스트 업데이트
    fun refreshRecyclerView(newList : ArrayList<String>) {
        recentSearchList = newList
        bind.recentSearchRcy.removeAllViewsInLayout()
        recentSearchRecyclerView(recentSearchList!!)
    }



}