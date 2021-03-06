package com.codingwithrufat.deliveryapplication.fragments.registration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.codingwithrufat.deliveryapplication.R
import com.codingwithrufat.deliveryapplication.activities.ClientActivity
import com.codingwithrufat.deliveryapplication.activities.CourierActivity
import com.codingwithrufat.deliveryapplication.models.users_detail.CourierDetail
import com.codingwithrufat.deliveryapplication.utils.conditions.checkLoginFields
import com.codingwithrufat.deliveryapplication.utils.conditions.isPhoneNumberInDatabaseOrNot
import com.codingwithrufat.deliveryapplication.utils.constants.TAG
import com.codingwithrufat.deliveryapplication.utils.prefence.MyPrefence
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_login.view.*

class LoginFragment : Fragment() {

    // objects
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: MyPrefence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        preferenceManager = MyPrefence(requireContext())

        db = FirebaseFirestore.getInstance()
        clickedTexts(view)
        clickedLoginButton(view)
        clickedbackfromLogin(view)
        return view
    }

    private fun clickedbackfromLogin(view: View) {
        view.ic_backFromLogin.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun clickedTexts(view: View) {

        view.txtSignup.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registerFragment)
        }
        view.bt_forget_password.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }

    }

    private fun clickedLoginButton(view: View) {

        view.button_login.setOnClickListener {

            val type = view.spinnerForLogin.selectedItem.toString()
            val phone_number = view.edit_phone_number_login.text.toString().trim()
            val password = view.edit_password_login.text.toString().trim()

            if (checkLoginFields(view, phone_number, password)) {

                view.rel_progress_login.visibility = View.VISIBLE

                isPhoneNumberInDatabaseOrNot(
                    db,
                    phone_number,
                    "${type}s",
                    isInDB = {

                        db.collection("${type}s")
                            .whereEqualTo("phone_number", phone_number)
                            .whereEqualTo("password", password)
                            .get()
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful && !task.result!!.isEmpty) {

                                    if (type == "Client") {
                                        startActivity(
                                            Intent(
                                                requireContext(),
                                                ClientActivity::class.java
                                            )
                                        )

                                        task.result.forEach {
                                            preferenceManager.setString(
                                                "user_id",
                                                it.getString("id")!!
                                            )
                                        }

                                        update_tokens("Clients")

                                        requireActivity().finish()
                                    }
                                    if (type == "Courier") {

                                        task.result.forEach {
                                            preferenceManager.setString(
                                                "user_id",
                                                it.getString("id")!!
                                            )
                                        }

                                        startActivity(
                                            Intent(
                                                requireContext(),
                                                CourierActivity::class.java
                                            )
                                        )

                                        update_tokens("Couriers")

                                        requireActivity().finish()
                                    }

                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Something went wrong. Try again later.",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }

                                view.rel_progress_login.visibility = View.INVISIBLE

                            }.addOnFailureListener {
                                view.rel_progress_login.visibility = View.INVISIBLE
                                Toast.makeText(
                                    requireContext(),
                                    "Something went wrong. Try again later.",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                    },
                    isNotInDB = {

                        view.rel_progress_login.visibility = View.INVISIBLE // progress is invisible

                        Toast.makeText(
                            requireContext(),
                            "There is no account with this phone number!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

            }

        }

    }

    private fun update_tokens(type: String) {

        // objects
        var userid = preferenceManager.getString("user_id")
        var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
        var data_ref = firebaseDatabase.reference
        val updateFbDb: HashMap<String, Any> = HashMap()

        // add to database login device token
        data_ref.child("$type").child(userid.toString())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                        updateFbDb["token"] = it.result.token
                        data_ref.child("$type").child(userid.toString())
                            .updateChildren(updateFbDb).addOnSuccessListener {
                                Log.d(TAG, "Succesfully updated")
                            }
                    }

                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Error: " + error.message)
                }

            })
    }
}