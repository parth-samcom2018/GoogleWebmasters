package com.digitalmid.seograph_webmasters_tool

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.anko.*
import org.json.JSONObject
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response


class AuthActivity : AppCompatActivity(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //val googleApiClient: GoogleApiClient?
    val RC_SIGN_IN = 100

    lateinit var dialog: ProgressDialog

    lateinit var googleApiClient: GoogleApiClient

    var unique_id: String? = null
    var mContext: Context

    //set access token to null
    var googleAccessTokenResponse: GoogleTokenResponse?

    init {

        //initialize variables
        mContext = this as Context

        googleAccessTokenResponse = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //hide title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)

        //actionbar
        val actionBar: ActionBar? = supportActionBar;

        //hide it we dont need it
        actionBar!!.hide()

        setContentView(R.layout.activity_auth)

        //signin btn using anko
        signInBtn.setSize(SignInButton.SIZE_WIDE)

        //init google login
        initGoogleLogin()

        //on click
        signInBtn.setOnClickListener { v ->
            signIn()
        }//end on Click


    }//end onCreate

    //init login
    fun initGoogleLogin() {

        val wmsScope = Scope("https://www.googleapis.com/auth/webmasters")

        //lets get profile info
        val plusPorfile = Scope(Scopes.PROFILE)

        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(wmsScope, plusPorfile)
                .requestId()
                .requestEmail()
                .requestServerAuthCode(googleAouth2WebClientId)
                .requestProfile()
                .build()


        //var scopeTwo = ""
        //google api client
        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build()


    }//end fun

    //on conneceted
    override fun onConnected(p0: Bundle?) {
        Log.e("ON_CONNECTED", "Connected")
    }//end on connected


    //on connection failed listterner
    override fun onConnectionFailed(
            connResult: ConnectionResult
    ) {
        //login failed
        longToast(getString(R.string.login_failed))
    }//end connection failed listener

    //connection suspended
    override fun onConnectionSuspended(i: Int) {}//end

    //sign in fun
    fun signIn() {

        //googleApiClient is empty return
        if (googleApiClient == null) {
            return
        }

        val signInIntent: Intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)

        dialog = indeterminateProgressDialog(getString(R.string.please_wait))
        dialog.setCancelable(false)

    }//end

    //sign in results
    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int,
                                  data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        //if our google signin
        if (requestCode == RC_SIGN_IN) {
            val result: GoogleSignInResult = Auth.GoogleSignInApi
                    .getSignInResultFromIntent(data)

            //lets proccess the signin result
            proccessSignInResults(result)
        }//end if

    }//end fun


    //proccess signin result
    fun proccessSignInResults(resultData: GoogleSignInResult): Boolean {

        // Log.e("AUTH DATA",varDump(resultData))

        //lets check if it was success or failure
        if (!resultData.isSuccess()) {
            dialog.dismiss()
            longToast(R.string.login_failed)
            return false
        }//end if

        //lets get details and save it for next time
        val account: GoogleSignInAccount? = resultData.getSignInAccount()

        //server auth code
        val authCode = account?.serverAuthCode

        Log.e("SERVER_AUTH_CODE", resultData.toString())

        //fetch the access token
        fetchAccessTokenResponse(authCode)

        //Log.e("ACCESS TOKEN RESPONSE",googleAccessTokenResponse.toString())

        //if the this.googleAccessTokenResponse is empty
        //then means we cant continue
        if (googleAccessTokenResponse == null) {

            //dismiss dialog
            dialog.dismiss()

            longToast(getString(R.string.access_token_failed))

            //abort proccessing
            return false
        }//end if


        if (googleAccessTokenResponse?.refreshToken == null) {
            //longToast("Refresh Token ${googleAccessTokenResponse?.refreshToken.toString()}")

        }

        //insert data
        var userInfoObj: JSONObject = JSONObject()

        ///get Auth Info
        var accessToken: String = googleAccessTokenResponse?.accessToken.toString()
        var refreshToken: String = googleAccessTokenResponse?.refreshToken.toString()

        var tokenExpiry: Long = googleAccessTokenResponse?.expiresInSeconds!!.toLong()

        //insert data into json object
        userInfoObj.put("display_name", account?.displayName)
        userInfoObj.put("email", account?.email)
        userInfoObj.put("user_id", account?.id)
        userInfoObj.put("server_auth_code", authCode)
        userInfoObj.put("profile_pic_url", account?.photoUrl)
        userInfoObj.put("access_token", accessToken)
        userInfoObj.put("refresh_token", refreshToken)
        userInfoObj.put("token_expiry", tokenExpiry)
        userInfoObj.put("id_token", googleAccessTokenResponse?.idToken)
        userInfoObj.put("id_token", account?.grantedScopes)

        Log.e("jsondata", userInfoObj.toString())

        //lets save the data to shared pref
        saveSharedPref(this, "user_info", userInfoObj.toString())

        unique_id = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        Log.d("info", userInfoObj.toString())


        /*val loginmodel = LoginModel()

        loginmodel.accountID = account?.id
        loginmodel.userName = account?.displayName
        loginmodel.email = account?.email
        loginmodel.userDeviceModel = unique_id
        loginmodel.websiteURL = null
        loginmodel.serverAuthCode = authCode
        loginmodel.idToken = account?.idToken
        loginmodel.profilePicUrl = account?.photoUrl.toString()
        loginmodel.accessToken = accessToken
        loginmodel.refreshToken = refreshToken
        loginmodel.tokenExpiry = tokenExpiry.toString()
        loginmodel.googleAccessTokenResponse = googleAccessTokenResponse?.idToken*/


        /*DM().getApi().postData(DM().getAuthString(),
                loginmodel, object : Callback<Response> {
            override fun success(t: Response?, response: Response?) {
                Toast.makeText(mContext, "Successfully add data",Toast.LENGTH_SHORT).show()
            }

            override fun failure(error: RetrofitError?) {
                Toast.makeText(mContext,"Failed to upload" ,Toast.LENGTH_SHORT).show()
            }
        })*/


        DM().getApi().postData(account?.id!!.toString(),
                account?.displayName!!,
                account?.email!!,
                unique_id!!,
                authCode!!,
                googleAccessTokenResponse?.idToken.toString(),
                "null",
                accessToken,
                refreshToken!!,
                tokenExpiry.toString(),
                userInfoObj!!.toString(),
                googleAccessTokenResponse?.idToken.toString(),
                object : Callback<Response> {
                    override fun success(t: Response?, response: Response?) {
                        //Toast.makeText(mContext, "Successfully add data",Toast.LENGTH_SHORT).show()
                        Log.d("onsuccess", "id :" + account?.id)
                        Log.d("onsuccess", "name :" + account?.displayName)
                        Log.d("onsuccess", "email :" + account?.email)
                        Log.d("onsuccess", "deviceid :" + unique_id)
                        Log.d("onsuccess", "auth :" + authCode)
                        Log.d("onsuccess", "photo :" + account?.photoUrl)
                        Log.d("onsuccess", "accestoken :" + accessToken)
                        Log.d("onsuccess", "refreshtoken :" + refreshToken)
                        Log.d("onsuccess", "info :" + userInfoObj.toString())
                    }

                    override fun failure(error: RetrofitError?) {
                        Toast.makeText(mContext, "Failed to upload", Toast.LENGTH_SHORT).show()
                    }

                })


        /*Log.d("data", "googleIdtoken :" + googleAccessTokenResponse?.idToken.toString())
        Log.d("data", "authcode :" + authCode)
        Log.d("data", "accesstoken :" + accessToken)
        Log.d("data", "refreshtoken :" + refreshToken)
        Log.d("data", "name :" + account?.displayName)
        Log.d("data", "email :" + account?.email)
        Log.d("data", "accountID :" + account?.id)
        Log.d("data", "authcode :" + authCode)
        Log.d("data", "url :" + account?.photoUrl.toString())
        Log.d("data", "tokenexpiry :" + tokenExpiry.toString())
        Log.d("data", "device id :" + unique_id)*/

        //greet user
        longToast("Hello ${account?.displayName}")


        //Toast.makeText(mContext , "" + googleAccessTokenResponse,Toast.LENGTH_SHORT).show()
        //Toast.makeText(mContext , "" + account?.email,Toast.LENGTH_SHORT).show()

        //dismiss dialog
        dialog.dismiss()


        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("googledata", account?.email)

        //lets now open sites list and close this activity
        startActivity(
                intentFor<SitesListActivity>()
                        .clearTop()
                        .newTask()
        )


        //finish current activity
        this.finish()


        return true
    }//end proccess signin data


    /**
     * setAccessToken
     */
    fun setAccessTokenResponse(accessTokenResponse: GoogleTokenResponse) {

        //set access token response
        googleAccessTokenResponse = accessTokenResponse


    }//end set accessTokenResponse

    /**
     *  get AccessToken
     *  @param accountName
     *  @param scopes
     */
    fun fetchAccessTokenResponse(serverAuthCode: String?) {

        val endpoint = "https://www.googleapis.com/oauth2/v4/token"
        val redirectUri = ""

        //create a weak reference
        //val ref: Ref<AuthActivity> = this.asReference()

        //create a deferred job
        val job = {

            //run it async mode
            async(CommonPool) {

                GoogleAuthorizationCodeTokenRequest(
                        NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        endpoint,
                        googleAouth2WebClientId,
                        googleAoauth2ClientSecret,
                        serverAuthCode,
                        redirectUri
                ).execute()
            }//end async
        }//end job

        //await can only run in supennd based function
        runBlocking {

            //token
            val tokenResponse = job.invoke().await()

            //set token response
            setAccessTokenResponse(tokenResponse)
        }//end

    }//end get AccessToken

}//end class

