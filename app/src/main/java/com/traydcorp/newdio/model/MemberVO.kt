package com.traydcorp.newdio.model

import java.io.Serializable

class MemberVO : Serializable
{
    lateinit var access_token : String // 소셜 로그인 시도시 콜백으로 받은 access token
    lateinit var interested_companies : ArrayList<String> // 관심 회사 리스트
    lateinit var interested_industries : ArrayList<String> // 관심 산업 리스트
    lateinit var birthday : String // datetime 1111-11-11 형식
    lateinit var gender : String // 성별 0:남자 1:여자 2:선택안함

}