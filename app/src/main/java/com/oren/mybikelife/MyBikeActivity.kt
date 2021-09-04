package com.oren.mybikelife

//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
//import android.support.v7.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.oren.mybikelife.data.Bike
import com.oren.xml.ElemList
import kotlinx.android.synthetic.main.activity_bike.*

class MyBikeActivity : AppCompatActivity() {
    private var bikeList = arrayListOf<Bike>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike)

        lvBikeList.adapter = MyBikeAdapter(this, bikeList)

        val lm = LinearLayoutManager(this)
        lvBikeList.layoutManager = lm
        lvBikeList.setHasFixedSize(true)
        (lvBikeList.adapter as MyBikeAdapter).reFresh()
    }
}
