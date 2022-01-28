package com.jagl.firebase

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jagl.firebase.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100

    private val callbackManager = CallbackManager.Factory.create()

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //Analytics Event
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message","Integracion con Firebase completa :3")
        analytics.logEvent("InitScreen", bundle)

        //Setup
        notification()
        setup()
        session()
    }

    private fun notification() {
        //Recuperacion del token

        FirebaseMessaging.getInstance().token.addOnCompleteListener {task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
            // Get new FCM registration token
            val token = task.result
            // Log
            Log.d(TAG, token)

            //Temas (Topics)
            FirebaseMessaging.getInstance().subscribeToTopic("tutorial")

            //Get data from a push notification
            val url = intent.getStringExtra("url")
            url?.let {
                Log.d(TAG,url)
            }

        }
    }

    override fun onStart() {
        super.onStart()
        binding.authLayout.visibility = View.VISIBLE
    }

    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email",null)
        val provider = prefs.getString("provider",null)
        if (email != null && provider != null){
            binding.authLayout.visibility = View.INVISIBLE
            showHome(email,ProviderType.valueOf(provider))
        }

    }

    private fun setup() {
        title = "Autenticación"
        binding.signUpButton.setOnClickListener {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                if (email.isNotEmpty() && password.isNotEmpty()){
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email,password)
                        .addOnCompleteListener {
                            if (it.isSuccessful){
                                showHome(it.result.user?.email ?: "",ProviderType.BASIC)
                            } else {
                                showAlert()
                            }
                        }
                }
            }
        binding.logginButton.setOnClickListener {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                if (email.isNotEmpty() && password.isNotEmpty()){
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener {
                            if (it.isSuccessful){
                                showHome(it.result.user?.email ?: "",ProviderType.BASIC)
                            } else {
                                showAlert()
                            }
                        }
                }
            }
        binding.googleButton.setOnClickListener {
                //Configuracion
                val googleConf =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                val googleClient = GoogleSignIn.getClient(this@AuthActivity,googleConf)
                googleClient.signOut()
                startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
            }
        binding.facebookButton.setOnClickListener {

                LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

                //Configuracion
                LoginManager.getInstance().registerCallback(callbackManager,
                    object: FacebookCallback<LoginResult>{
                        override fun onSuccess(result: LoginResult?) {
                            result?.let { loginResult ->

                                val token = loginResult.accessToken
                                val credential = FacebookAuthProvider.getCredential(token.token)
                                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                                    if (it.isSuccessful){
                                        showHome(it.result?.user?.email ?: "",ProviderType.FACEBOOK)
                                    } else {
                                        showAlert()
                                    }
                                }
                            }
                        }

                        override fun onCancel() {
                            /**SIN LOGICA DE NEGOCIO**/
                        }

                        override fun onError(error: FacebookException?) {
                            showAlert()
                        }

                    })
            }
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this,HomeActivity::class.java).apply {
            putExtra("email",email)
            putExtra("provider",provider.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                if (account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken,null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(account.email ?: "",ProviderType.GOOGLE)
                        } else {
                            showAlert()
                        }
                    }
                }
            }catch (e: ApiException){
                showAlert()
            }
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}