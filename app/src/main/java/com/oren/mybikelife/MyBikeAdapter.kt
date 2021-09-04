package com.oren.mybikelife

    import android.content.Context
//    import android.support.v7.widget.RecyclerView
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.*
    import androidx.recyclerview.widget.RecyclerView
    import com.oren.mybikelife.data.Bike
    import com.oren.mybikelife.data.Config
    import com.oren.util.bluetooth.UseGattAttributes
    import com.oren.xml.Element

class MyBikeAdapter(private val context: Context, private val bikeList: ArrayList<Bike>) :  RecyclerView.Adapter<MyBikeAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_bike, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return bikeList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(bikeList[position], context)
    }

    fun reFresh() {
        bikeList.clear()
        for(el in Config.getBikes().gChildren()) {
            val bconn =
                if (el.gAttrValue("connectDevice") == null) true else java.lang.Boolean.parseBoolean(el.gAttrValue("connectDevice"))
//            android.util.Log.d("xy", "????==> "+el.gAttrValue("connectDevice"))
            val bike = Bike(el.gAttrValue("id"), el.gAttrValue("name")
                , el.gAttrValue("wheelType"), el.gAttrValue("wheelDes"), el.gAttrValue("wheelSize").toInt()
                ,el.gAttrValue("photo"), bconn)
            bikeList.add(bike)
        }
        this.notifyDataSetChanged()
    }
    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconType = itemView.findViewById<ImageView>(R.id.iconType)
        private val bikeId = itemView.findViewById<TextView>(R.id.bikeId)
        private val bikeName = itemView.findViewById<TextView>(R.id.bikeName)
        private val wheelType = itemView.findViewById<Spinner>(R.id.wheelType)
//        private val wheelSize = itemView.findViewById<TextView>(R.id.wheelSize)
        private val deviceName = itemView.findViewById<TextView>(R.id.deviceName)
        private val deviceAddress = itemView.findViewById<TextView>(R.id.deviceAddress)
        private val tbConnectDevice = itemView.findViewById<ToggleButton>(R.id.tbConnectDevice)
        private val tbInitDevice = itemView.findViewById<Button>(R.id.tbInitDevice)
        private val deviceName1 = itemView.findViewById<TextView>(R.id.deviceName1)
        private val deviceAddress1 = itemView.findViewById<TextView>(R.id.deviceAddress1)
        fun bind (bike: Bike, context: Context) {
            if (bike.photo != null) {
                val resourceId = context.resources.getIdentifier(bike.photo, "drawable", context.packageName)
                iconType.setImageResource(resourceId)
            } else {
                iconType.setImageResource(R.mipmap.ic_launcher)
            }
            bikeId.text = bike.id
            bikeName.text = bike.name
            wheelType.adapter = ArrayAdapter.createFromResource(context, R.array.wheeltypelist, android.R.layout.simple_spinner_item)
//            android.util.Log.d("xy", "??? = > "+bike.connectDevice)
            tbConnectDevice.isChecked = bike.connectDevice
            val wheelarr = context.resources.getStringArray(R.array.wheeltypelist)
            var pos = 0
            for(str in wheelarr) {
                if(str.startsWith(bike.wheelType)) {
                    wheelType.setSelection(pos)
                    break
                }
                pos++
            }
            wheelType.onItemSelectedListener = object:AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val wheelInfo = context.resources.getStringArray(R.array.wheeltypelist)[position].split(":")
                    Config.selectedElBike.sAttr("wheelType",wheelInfo[0])
                    Config.selectedElBike.sAttr("wheelDes", wheelInfo[1])
                    Config.selectedElBike.sAttr("wheelSize", wheelInfo[2].substring(0,3))
                    Config.selectWeelSize = wheelInfo[2].substring(0,3).toFloat()
                    Config.selectedBike.wheelType = wheelInfo[0]
                    Config.selectedBike.wheelDes = wheelInfo[1]
                    Config.selectedBike.wheelSize = wheelInfo[2].substring(0,3).toInt()
                    Config.saveConfig()
                }
            }
//            for(elDevice in Config.selectedElBike.gChildren("device")) {
//                tbConnectDevice.isChecked = elDevice.gAttrValue("connectDevice") == "true"
//                break
//            }
            //
            tbConnectDevice.setOnClickListener {
                android.util.Log.d("xy", "11111????==> "+bike.connectDevice+"::::"+ tbConnectDevice.isChecked);
                bike.connectDevice = tbConnectDevice.isChecked
                android.util.Log.d("xy", "22222????==> "+bike.connectDevice+"::::"+ tbConnectDevice.isChecked);
                Config.modifySave(bike)
                if(!tbConnectDevice.isChecked) {
                    UseGattAttributes.disconnect()
                } else {
                    if(deviceAddress.text != null && deviceAddress.text != "")
                        UseGattAttributes.connect(deviceAddress.text.toString(),  deviceAddress1.text.toString())
                }
            }
            // 등록된 디바이스 제거
            tbInitDevice.setOnClickListener {
                deviceName.text = ""
                deviceAddress.text = ""
                deviceName1.text = ""
                deviceAddress1.text = ""
                tbConnectDevice.isChecked = false
                bike.connectDevice = false
                Config.selectedElBike.rChildren("device")
                Config.modifySave(Config.selectedBike)
                UseGattAttributes.disconnect()
            }
//            wheelType.text = bike.wheelType + ":"+bike.wheelDes
//            wheelSize.text = bike.wheelSize.toString()
            for(elDevice in Config.selectedElBike.gChildren("device")) {
                if(deviceName.text == "") {
                    deviceName.text = elDevice.gAttrValue("deviceName")
                    deviceAddress.text = elDevice.gAttrValue("deviceAddress")
                } else {
                    deviceName1.text = elDevice.gAttrValue("deviceName")
                    deviceAddress1.text = elDevice.gAttrValue("deviceAddress")
                }
            }

//            itemView.setOnClickListener {
//                if(lyItemBody.visibility == View.VISIBLE) {
//                    lyItemBody.visibility = View.GONE
//                } else {
//                    lyItemBody.visibility = View.VISIBLE
//                }
//                itemClick(history)
//            }
//            remove.setOnClickListener {
//                val yesNo = AlertDialog.Builder(context)
//                yesNo.setTitle(context.getString(R.string.ok))
//                yesNo.setMessage(context.getString(R.string.his_del_msg) + "\n"+history.name)
//                yesNo.setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
//                    val ff = File(util.dataPath + history.name)
//                    if(ff.exists()) {
//                        ff.delete()
//                        reFresh()
//                    }
//                    dialog.dismiss()
//                }
//                yesNo.setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
//                    dialog.dismiss()
//                }
//                yesNo.create()
//                yesNo.show()
//            }
        }
    }
}


