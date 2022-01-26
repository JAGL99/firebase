package com.jagl.firebase

import android.content.Context
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.jagl.firebase.databinding.ActivityHomeBinding

enum class ProviderType{
    BASIC, GOOGLE, FACEBOOK
}

 private lateinit var binding: ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        //Guardado de datos
        val pref = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
        pref.putString("email",email)
        pref.putString("provider",provider)
        pref.apply()

    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"

        binding.emailTextView.text = email
        binding.providerTextView.text = provider

        binding.logOutButton.setOnClickListener {
            //Borrar datos
            val pref = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
            pref.clear()
            pref.apply()

            if (provider == ProviderType.FACEBOOK.name){
                LoginManager.getInstance().logOut()
            }

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        binding.errorButton.setOnClickListener {
            //Envio de informacion adicional
            FirebaseCrashlytics.getInstance().setUserId(email)
            FirebaseCrashlytics.getInstance().setCustomKey("provider",provider)

            //Enviar log de contexto
            FirebaseCrashlytics.getInstance().log("Se ha presionado el boton de error")

            throw RuntimeException("Test Crash")
        }

        binding.saveDataButton.setOnClickListener {

            db.collection("users").document(email).set(
                hashMapOf(
                    "provider" to provider
                )
            )

        }

        binding.getDataButton.setOnClickListener {

            db.collection("users").document(email).get().addOnSuccessListener { data ->
                Log.d("USERS_DATA", data.get("provider") as String)
            }


        }

        binding.deleteDataButton.setOnClickListener {
            db.collection("users").document(email).delete().addOnCompleteListener {
                Log.d("DELETE_USERS_DATA","DATOS_ELIMINADOS")
            }
        }
    }
}