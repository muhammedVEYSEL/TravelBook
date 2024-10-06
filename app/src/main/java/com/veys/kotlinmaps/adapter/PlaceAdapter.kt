package com.veys.kotlinmaps.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veys.kotlinmaps.databinding.RecycleRowBinding
import com.veys.kotlinmaps.model.Place
import com.veys.kotlinmaps.view.MapsActivity

class PlaceAdapter(val placeList: List<Place>):RecyclerView.Adapter<PlaceAdapter.PlaceHolder>(){

    class PlaceHolder(val binding: RecycleRowBinding):RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceHolder {
        val binding = RecycleRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlaceHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceHolder, position: Int) {
        holder.binding.textViewLocation.text = placeList.get(position).name
        holder.itemView.setOnClickListener(){
            val intent = Intent(holder.itemView.context,MapsActivity::class.java)
            intent.putExtra("selectedPlace",placeList.get(position))
            intent.putExtra("info","old")
            holder.itemView.context.startActivity(intent)
        }

    }


    override fun getItemCount(): Int {
        return placeList.size
    }
}