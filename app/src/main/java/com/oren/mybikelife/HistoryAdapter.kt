package com.oren.mybikelife

    import android.content.Context
    import android.graphics.Color
//    import android.support.v4.app.FragmentActivity
//    import android.support.v7.app.AlertDialog
//    import android.support.v7.widget.RecyclerView
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.RelativeLayout
    import android.widget.TextView
    import androidx.appcompat.app.AlertDialog
    import androidx.fragment.app.FragmentActivity
    import androidx.recyclerview.widget.RecyclerView
    import com.google.android.gms.maps.CameraUpdateFactory
    import com.google.android.gms.maps.GoogleMap
    import com.google.android.gms.maps.OnMapReadyCallback
    import com.google.android.gms.maps.SupportMapFragment
    import com.google.android.gms.maps.model.*
    import com.oren.mybikelife.data.History
    import com.oren.xml.Element
    import com.oren.xml.XmlParser
    import java.io.File
    import kotlin.collections.ArrayList
    import kotlin.Comparator as Comparator1

class HistoryAdapter(private val context: Context, private val historyList: ArrayList<History>, private val itemClick: (History) -> Unit) :
    RecyclerView.Adapter<HistoryAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(historyList[position], context)
    }

    fun reFresh() {
        if(util.dataPath == null) util.dataPath = context.filesDir.absolutePath+ File.separator
        val file = File(util.dataPath)
        historyList.clear()
        if (file.exists()) {
            if (file.isDirectory) {
                var list = file.listFiles()
                list = list.sortedArray().sortedArrayDescending()

                for (f in list) {
                    if (!f.isDirectory && f.path.endsWith(".xml") && !f.path.endsWith("config.xml")) {
                        val name = f.path.substring(f.path.lastIndexOf("/")+1)
                        val dt = name.split("_")[1];
                        val dateTime = dt.substring(0,4)+"."+dt.substring(4,6)+"."+dt.substring(6,8)+" "+dt.substring(8,10)+":"+dt.substring(10,12)
                        val ele = XmlParser().parse(f.path).gRootElem()
                        val loadHistory = PolylineOptions()
                        for(el: Element in ele.gChildren()) {
                            val strXY = el.gText().split(" ")
                            loadHistory.add(LatLng(strXY[0].toDouble(), strXY[1].toDouble()))
                        }
                        val history = History(
                            name,
                            dateTime,
                            0,
                            "riding_history"
                            ,
                            if (ele.gAttrValue("maxSpeed") == null) context.getString(R.string.max_speed) + " " + "Empty" else context.getString(
                                R.string.max_speed
                            ) + " " + ele.gAttrValue("maxSpeed")
                            ,
                            if (ele.gAttrValue("aveSpeed") == null) context.getString(R.string.ave_speed) + " " + "Empty" else context.getString(
                                R.string.ave_speed
                            ) + " " + ele.gAttrValue("aveSpeed")
                            ,
                            if (ele.gAttrValue("moveLen") == null) context.getString(R.string.move_len) + " " + "Empty" else context.getString(
                                R.string.move_len
                            ) + " " + ele.gAttrValue("moveLen")
                            ,
                            if (ele.gAttrValue("moveTime") == null) context.getString(R.string.move_time) + " " + "Empty" else context.getString(
                                R.string.move_time
                            ) + " " + ele.gAttrValue("moveTime")
                            ,
                            loadHistory
                        )
                        historyList.add(history)
                    }
                }
                notifyDataSetChanged()
            }
        }
    }
    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView), OnMapReadyCallback {

        private val iconType = itemView.findViewById<ImageView>(R.id.iconType)
        private val dateTime = itemView.findViewById<TextView>(R.id.dateTime)
        private val maxSpeed = itemView.findViewById<TextView>(R.id.maxSpeed)
        private val aveSpeed = itemView.findViewById<TextView>(R.id.aveSpeed)
        private val moveLen = itemView.findViewById<TextView>(R.id.moveLen)
        private val moveTime = itemView.findViewById<TextView>(R.id.moveTime)
        private val remove = itemView.findViewById<ImageView>(R.id.remove)
        private val lyItemBody = itemView.findViewById<RelativeLayout>(R.id.lyItemBody)
        private var historyData : PolylineOptions? = null
        private var sMapItem: GoogleMap? = null
        private var mapFragment: SupportMapFragment? = null
        override fun onMapReady(googleMap: GoogleMap) {
            sMapItem = googleMap
            sMapItem?.addPolyline(historyData)
            var latMin = Double.MAX_VALUE
            var latMax = Double.MIN_VALUE
            var lngMin = Double.MAX_VALUE
            var lngMax = Double.MIN_VALUE
            for (latLng : LatLng  in historyData?.points!!) {
                if(latMin > latLng.latitude && latLng.latitude != 0.0) latMin = latLng.latitude
                if(latMax < latLng.latitude && latLng.latitude != 0.0) latMax = latLng.latitude
                if(lngMin > latLng.longitude && latLng.longitude != 0.0) lngMin = latLng.longitude
                if(lngMax < latLng.longitude && latLng.longitude != 0.0) lngMax = latLng.longitude
            }
            val latLngBounds = LatLngBounds(LatLng(latMin, lngMin),LatLng(latMax, lngMax))
//            sMapItem?.setLatLngBoundsForCameraTarget(latLngBounds)
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(CameraPosition(historyData?.points!![0], 18f,0f,0f))
            sMapItem?.moveCamera(cameraUpdate)
            if(historyData?.points!!.size > 2) {
                sMapItem?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 300, 200, 0))
//           } else {
//                val cameraUpdate = CameraUpdateFactory.newCameraPosition(CameraPosition(historyData?.points!![0], 18f,0f,0f))
//                sMapItem?.moveCamera(cameraUpdate)
            }

        }
        fun bind (history: History, context: Context) {
            try {
                val fragmentManager = (context as FragmentActivity).supportFragmentManager;
                mapFragment = fragmentManager.findFragmentById(lyItemBody.getChildAt(0) .id) as SupportMapFragment //context.activi.findViewById(R.id.mapItem) as SupportMapFragment
                mapFragment?.getMapAsync(this@Holder)
            } catch (e: Exception) {
                android.util.Log.d("xy", "Can't get fragment manager");
            }

            if (history.photo != "" && history.type == 0) {
                val resourceId = context.resources.getIdentifier(history.photo, "drawable", context.packageName)
                iconType.setImageResource(resourceId)
            } else {
                iconType.setImageResource(R.mipmap.ic_launcher)
            }
            historyData = history.loadHistory
            historyData?.color(Color.BLUE)
            historyData?.width(7f)
            historyData?.startCap(RoundCap()) //ButtCap, CustomCap, RoundCap, SquareCap
            historyData?.endCap(RoundCap())

            dateTime.text = history.dateTime
            maxSpeed.text = history.maxSpeed
            aveSpeed.text = history.aveSpeed
            moveLen.text = history.moveLen
            moveTime.text = history.moveTime
            itemView.setOnClickListener {
                if(lyItemBody.visibility == View.VISIBLE) {
                    lyItemBody.visibility = View.GONE
                } else {
                    lyItemBody.visibility = View.VISIBLE
                }
                itemClick(history)
            }
            remove.setOnClickListener {
                val yesNo = AlertDialog.Builder(context)
                yesNo.setTitle(context.getString(R.string.ok))
                yesNo.setMessage(context.getString(R.string.his_del_msg) + "\n"+history.name)
                yesNo.setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                    val ff = File(util.dataPath + history.name)
                    if(ff.exists()) {
                        ff.delete()
                        reFresh()
                    }
                    dialog.dismiss()
                }
                yesNo.setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                yesNo.create()
                yesNo.show()
            }
        }
    }
}


