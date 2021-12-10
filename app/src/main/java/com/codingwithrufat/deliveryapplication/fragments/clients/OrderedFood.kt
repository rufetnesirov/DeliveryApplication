package com.codingwithrufat.deliveryapplication.fragments.clients

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.codingwithrufat.deliveryapplication.R
import com.codingwithrufat.deliveryapplication.models.food_properties.FoodProperty
import com.codingwithrufat.deliveryapplication.notifications.FcmNotificationsSender
import com.codingwithrufat.deliveryapplication.utils.constants.TAG
import com.codingwithrufat.deliveryapplication.utils.prefence.MyPrefence
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_ordered_food.view.*
import kotlin.collections.HashMap
import com.codingwithrufat.deliveryapplication.activities.MainActivity
import com.google.firebase.database.DatabaseError
import com.codingwithrufat.deliveryapplication.models.users_detail.CourierDetail
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener








class OrderedFood :Fragment() {

    //variables
    private var source_latitude:Double?=null
    private var source_longitude:Double?=null
    private var food_name:String?=null
    private var food_heigth:String?=null
    private var food_length:String?=null
    private var food_width:String?=null
    private var food_weight:String?=null
    private var order_time: String? = ""
    private var food_id:String?=null
    private var destination_latitude:Double?=null
    private var destination_longitude:Double?=null

    lateinit var firebaseAuth: FirebaseAuth
    var firebaseDatabase: FirebaseDatabase= FirebaseDatabase.getInstance()
    var data_ref=firebaseDatabase.reference
    lateinit var firebaseFirestore: FirebaseFirestore
    lateinit var prefence: MyPrefence
    lateinit var deviceToken:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view=inflater.inflate(R.layout.fragment_ordered_food,container,false)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        prefence= MyPrefence(context)

        view.submit.setOnClickListener {
            food_name=view.product_name.text.toString()
            data_setfoods(view,food_name!!)
            update_client_data(view)
            sendnotification_tocouriers(food_name!!)
        }
        view.ic_backFromOrderedFood.setOnClickListener {
            startActivity(Intent(context,MainActivity::class.java))
            firebaseAuth.signOut()
        }
        return view
    }

    private fun sendnotification_tocouriers(food_name: String) {
        //find courier user ids
        firebaseFirestore.collection("Couriers").addSnapshotListener { query,error ->
            for (i in 0 until query?.documents?.size!! ){
                var userId:String=query.documents[i].getString("id").toString()
                //find courier tokens
                data_ref.child("Couriers").child(userId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.getValue(CourierDetail::class.java)
                        for (j in 0 until value?.token?.size!!) {
                            deviceToken = value.token!![i]
                            var busy = value?.busy
                            //notification sending only not busy couriers
                            if (busy == false) {
                                val notificationsSender = FcmNotificationsSender(
                                    deviceToken,
                                    "New Order",
                                    "Someone wants " + food_name,
                                    requireContext(),
                                    requireActivity()
                                )
                                notificationsSender.SendNotifications()

                            }
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d(TAG,"Error:"+error.message)
                    }

                })

            }
        }


    }

    private fun update_client_data(view: View) {
        val longitude=prefence.getString("longitude")?.toDouble()
        val latitude=prefence.getString("latitude")?.toDouble()
        val updateFbDb: HashMap<String, Any> = HashMap()
        val userID=firebaseAuth.currentUser?.uid

        if (latitude != null && longitude != null) {
            updateFbDb.put("client_latitude", latitude)
            updateFbDb.put("client_longitude",longitude)
                    data_ref.child("Clients").child(userID.toString()).updateChildren(updateFbDb).addOnSuccessListener {
                        Log.d(TAG,"Succesfully updated")
                        Navigation.findNavController(view)
                            .navigate(R.id.action_orderedFood_to_clientMapFragment)
                    }

            }


    }

    private fun data_setfoods(view: View, food_name: String) {
            food_heigth=view.product_heigth.text.toString()
            food_weight=view.product_length.text.toString()
            food_width=view.product_width.text.toString()
            food_length=view.product_length.text.toString()
            source_latitude=0.0
            source_longitude=0.0
            destination_latitude=0.0
            destination_longitude=0.0
            order_time=""
            food_id= data_ref.push().key
            if (TextUtils.isEmpty(food_name) || TextUtils.isEmpty(food_heigth) || TextUtils.isEmpty(food_weight) ||
                TextUtils.isEmpty(food_width) || TextUtils.isEmpty(food_length)){
                Toast.makeText(context,"Please fill all boxes",Toast.LENGTH_LONG).show()
            }
            else {
                val foods_property = FoodProperty(
                    food_id,
                    this.food_name,
                    food_weight,
                    food_heigth,
                    food_width,
                    food_length,
                    order_time,
                    source_latitude,
                    source_longitude,
                    destination_longitude,
                    destination_latitude
                )
                data_ref.child("Foods").child(food_id.toString()).setValue(foods_property)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Successfully set")
                        } else {
                            Log.d(TAG, task.exception.message.toString())
                        }
                    }
            }
        }
}