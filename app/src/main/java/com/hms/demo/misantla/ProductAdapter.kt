package com.hms.demo.misantla

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hms.demo.misantla.databinding.ItemBinding
import com.huawei.hms.iap.entity.ProductInfo

class ProductAdapter(private val listener:OnProductClickListener):RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    var productList:List<ProductInfo>?=null

    class ProductViewHolder(private val binding : ItemBinding,private var listener: OnProductClickListener):RecyclerView.ViewHolder(binding.root){
        fun bind(item:ProductInfo){
            binding.product=item
            binding.listener=listener
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater=LayoutInflater.from(parent.context)
        val binding=ItemBinding.inflate(inflater,parent, false)
        return ProductViewHolder(binding,listener)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        productList?.let{
            holder.bind(it[position])
        }
    }

    override fun getItemCount(): Int {
        return if(productList!=null){
            productList!!.size
        }else  0
    }

    public interface OnProductClickListener{
        fun onProductClicked(item:ProductInfo)
    }
}