package com.digitalmid.seograph_webmasters_tool

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.Toast

/**
 * Created by dr_success on 8/12/2017.
 */
open class DrawerActivity : AppCompatActivity() {

    val MYPref = "Pref"
    lateinit var sPref: SharedPreferences
    //lazy init vals
    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }

    private val drawerLayout: DrawerLayout by lazy {
        findViewById<DrawerLayout>(R.id.drawer_layout)
    }

    private val navView: NavigationView by lazy {
        findViewById<NavigationView>(R.id.navView)
    }

    val context: Context? by lazy {
        this
    }

    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set toolbar as actionbar
        setSupportActionBar(toolbar)

        //lets get the headerView of the drawer and set the neccessary vars
        val headerView: View = navView.getHeaderView(0)

        //set Header Info
        setDrawerHeaderInfo(this, headerView)

        drawerToggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        )

        //lets set drawer listener
        drawerLayout.addDrawerListener(drawerToggle)

        //lets set drawer item click listener
        navView.setNavigationItemSelectedListener({ item: MenuItem ->
            onNavItemSelected(item)
        })

        sPref = getSharedPreferences(MYPref, MODE_PRIVATE)

    }//end

    //on NavItemSelected
    fun onNavItemSelected(item: MenuItem): Boolean {

        val itemId = item.itemId

        val itemActivity = when (itemId) {
            R.id.sites -> SitesListActivity::class.java
            //R.id.comparison -> ComparisonActivity::class.java
            R.id.about -> AboutActivity::class.java
            R.id.contact_us -> ContactActivity::class.java
            else -> null
        }

        val curActivityName = this.javaClass.name

        //is Same activity
        val isSameActivity = curActivityName.equals(itemActivity?.name)

        //if itemActivity is not null or
        //not the same activity, then open activity
        if (!(itemActivity == null || isSameActivity)) {

            //Open activity
            startActivity(Intent(applicationContext, itemActivity))
        } else if (itemId == R.id.revoke_access) {
            removeUserInfo(this)

            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = preferences.edit()
            editor.remove("autoSave")
            editor.apply()
        }

        //close drawer
        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }//end

    //onOptionItemSelected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }

        //if item
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }//end


    /**
     * onPostCreate
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        //sync state of toggle
        drawerToggle.syncState()
    }//end


    /**
     * onconfiguration change tool.. we should
     * change the toggle
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        //onConfigurationChanged
        drawerToggle.onConfigurationChanged(newConfig)
    }//end


}//end class