package com.oren.mybikelife

//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
//import android.support.v7.widget.LinearLayoutManager
import com.oren.mybikelife.data.History
import kotlinx.android.synthetic.main.activity_history.*



class HistoryActivity : AppCompatActivity() {
    private var historyList = arrayListOf<History>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        lvHistoryList.adapter = HistoryAdapter(this, historyList) { history ->
//            android.widget.Toast.makeText(this, "개의 품종은 ${history.dateTime} 이며, 나이는 ${history.type}세이다.", android.widget.Toast.LENGTH_SHORT).show()
        }

        val lm = LinearLayoutManager(this)
        lvHistoryList.layoutManager = lm
        lvHistoryList.setHasFixedSize(true)
        (lvHistoryList.adapter as HistoryAdapter).reFresh()
    }


}
