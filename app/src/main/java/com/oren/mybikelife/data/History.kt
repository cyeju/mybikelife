package com.oren.mybikelife.data

import com.google.android.gms.maps.model.PolylineOptions

class History (
    val name : String
    , val dateTime: String  // 날짜 및 시간
    , val type: Int = 0   // 형싱 0 : 읿반 주행 기록
    , val photo: String // 아이콘 이미지
    , val maxSpeed : String
    , val aveSpeed : String
    , val moveLen : String
    , val moveTime : String
    , var loadHistory : PolylineOptions
)