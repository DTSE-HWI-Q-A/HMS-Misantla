package com.hms.demo.misantla

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hms.demo.misantla.databinding.IapBinding
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.Task
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapApiException
import com.huawei.hms.iap.entity.*
import com.huawei.hms.iap.util.IapClientHelper
import com.huawei.hms.support.api.client.Status
import org.json.JSONException


class IapActivity : AppCompatActivity(), ProductAdapter.OnProductClickListener {
    companion object {
        const val ENV_CHECK = 200
        const val REQUEST_PURCHASE = 300
    }

    lateinit var binding: IapBinding
    lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = IapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ProductAdapter(this)
        binding.recyclerView.adapter = adapter
        startIap()
    }

    private fun startIap() {
        val task = Iap.getIapClient(this).isEnvReady

        task.addOnSuccessListener {
            val accountFlag = it.accountFlag
            obtainProductInfo()
        }.addOnFailureListener {
            Log.e("IAP", it.toString())
            if (it is IapApiException) {
                val apiException = it as IapApiException
                val status: Status = apiException.status
                if (status.statusCode == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                    // HUAWEI ID is not signed in.
                    if (status.hasResolution()) {
                        try {
                            // Open the sign-in screen returned.
                            status.startResolutionForResult(this@IapActivity, ENV_CHECK)
                        } catch (exp: IntentSender.SendIntentException) {
                        }
                    }
                } else if (status.statusCode == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                    // The current location does not support IAP.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENV_CHECK -> processAccountResolution(data)
            REQUEST_PURCHASE -> processPurchaseResult(data)
        }
    }

    private fun processPurchaseResult(data: Intent?) {
        data?.let{
            // Call the parsePurchaseResultInfoFromIntent method to parse the payment result.
            val purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data)
            when (purchaseResultInfo.returnCode) {
                OrderStatusCode.ORDER_STATE_CANCEL -> {
                }
                OrderStatusCode.ORDER_STATE_FAILED ,
                OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                    redeliveryProcess()
                }
                OrderStatusCode.ORDER_STATE_SUCCESS -> {
                    // The payment is successful.
                    val inAppPurchaseData = purchaseResultInfo.inAppPurchaseData
                    val inAppPurchaseDataSignature = purchaseResultInfo.inAppDataSignature
                    // Construct a ConsumeOwnedPurchaseReq object.
                    val req = ConsumeOwnedPurchaseReq()
                    var purchaseToken = ""
                    try {
                        // Obtain purchaseToken from InAppPurchaseData.
                        val inAppPurchaseDataBean = InAppPurchaseData(inAppPurchaseData)
                        provideProduct(inAppPurchaseDataBean.productId)
                        purchaseToken = inAppPurchaseDataBean.purchaseToken
                    } catch (e: JSONException) {
                    }
                    req.purchaseToken = purchaseToken
// Call the consumeOwnedPurchase API to consume the product after delivery if the product is a consumable.
                    val task = Iap.getIapClient(this).consumeOwnedPurchase(req)
                    task.addOnSuccessListener {
                        // Obtain the execution result.
                    }.addOnFailureListener { e ->
                        if (e is IapApiException) {
                            val apiException = e as IapApiException
                            val status: Status = apiException.status
                            val returnCode = apiException.statusCode
                        } else {
                            // Other external errors.
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun redeliveryProcess() {
        // Construct an OwnedPurchasesReq object.
        val ownedPurchasesReq = OwnedPurchasesReq().apply {
            // priceType: 0: consumable; 1: non-consumable; 2: subscription
            priceType = 0
        }
// Call the obtainOwnedPurchases API to obtain the order information about all consumable products that have been purchased but not delivered.
        val task = Iap.getIapClient(this).obtainOwnedPurchases(ownedPurchasesReq)
        task.addOnSuccessListener { result ->
            // Obtain the execution result if the request is successful.
            if (result?.inAppPurchaseDataList != null) {
                for (i in result.inAppPurchaseDataList.indices) {
                    val inAppPurchaseData = result.inAppPurchaseDataList[i]
                    val inAppSignature = result.inAppSignature[i]
                    // Use the IAP public key to verify the signature of inAppPurchaseData.
                    // Check the purchase status of each product if the verification is successful. When the payment has been made, deliver the required product. After the delivery is performed, consume the product.
                    try {
                        val inAppPurchaseDataBean = InAppPurchaseData(inAppPurchaseData)
                        val purchaseState = inAppPurchaseDataBean.purchaseState
                        if(purchaseState==0){
                            provideProduct(inAppPurchaseDataBean.productId)
                        }
                    } catch (e: JSONException) {
                    }
                }
            }
        }.addOnFailureListener { e ->
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val status: Status = apiException.status
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        }
    }

    private fun provideProduct(productId: String) {
        //TODO aumentar vidas, gemas, monedas o el producto que el usuario haya adquirido
        Toast.makeText(this,"El producto: $productId se adquirió exitosamente",Toast.LENGTH_LONG).show()
    }

    private fun processAccountResolution(data: Intent?) {
        data?.let {
            // Call the parseRespCodeFromIntent method to obtain the result of the API request.
            val returnCode = IapClientHelper.parseRespCodeFromIntent(it)
            if (returnCode == 0) {
                obtainProductInfo()
            }
        }
    }

    private fun obtainProductInfo() {
        val productIdList: MutableList<String> = ArrayList()
// Only those products already configured in AppGallery Connect can be queried.
        productIdList.add("CONSUMABLE1")
        productIdList.add("CONSUMABLE2")
        val req = ProductInfoReq().apply {
            // priceType: 0: consumable; 1: non-consumable; 2: subscription
            priceType = 0
            productIds = productIdList
        }
// Call the obtainProductInfo API to obtain the details of the product configured in AppGallery Connect.
        val task = Iap.getIapClient(this).obtainProductInfo(req)
        task.addOnSuccessListener { result ->
            // Obtain the product details returned upon a successful API call.
            Log.e("Tag", "ProductInfo Success")
            val productList = result.productInfoList
            adapter.productList = productList
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.e("IAP", e.toString())
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        }
    }

    override fun onProductClicked(item: ProductInfo) {
        Toast.makeText(this, "Producto: ${item.productName}", Toast.LENGTH_SHORT).show()
        //Create purchase intent
        val req = PurchaseIntentReq().apply {
            // Only those products already configured in AppGallery Connect can be purchased through the createPurchaseIntent API.
            productId = item.productId
// priceType: 0: consumable; 1: non-consumable; 2: subscription
            priceType = 0
            developerPayload = "test"
        }

// Obtain the Activity object that calls the API.
// Call the createPurchaseIntent API to create a managed product order.
        val task: Task<PurchaseIntentResult> = Iap.getIapClient(this).createPurchaseIntent(req)
        task.addOnSuccessListener { result ->
            // Obtain the order creation result.
            val status = result.status
            if (status.hasResolution()) {
                try {
                    // Open the checkout screen returned.
                    status.startResolutionForResult(this@IapActivity, REQUEST_PURCHASE)
                } catch (exp: SendIntentException) {
                }
            }
        }.addOnFailureListener(OnFailureListener { e ->
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val status = apiException.status
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        })
    }

    override fun onBackPressed() {
        finish()
    }
}