package com.fvbox.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.fvbox.BuildConfig
import com.fvbox.R
import com.fvbox.app.base.BaseActivity
import com.fvbox.app.config.BoxConfig
import com.fvbox.app.contract.OpenZipDocument
import com.fvbox.app.ui.info.AppInfoActivity
import com.fvbox.app.ui.local.AppInstallReceiver
import com.fvbox.app.ui.local.LocalAppActivity
import com.fvbox.app.ui.setting.PreferenceActivity
import com.fvbox.app.ui.tab.UserSelectActivity
import com.fvbox.databinding.ActivityDashboardBinding
import com.fvbox.databinding.ActivityMainBinding
import com.fvbox.databinding.ViewMainHeaderBinding
import com.fvbox.util.Log
import com.fvbox.util.property.viewBinding
import com.fvbox.util.showSnackBar
import com.gyf.immersionbar.ktx.immersionBar
import com.permissionx.guolindev.PermissionX
import org.json.JSONObject

class DashboardActivity : BaseActivity() {

    private val binding by viewBinding(ActivityDashboardBinding::bind)

    private val viewModel by viewModels<DashboardViewModel>()

    private var currentUserID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setUpToolbar(R.string.app_name)
        initDrawerLayout()
        initOtherView()
        observeData()
        AppInstallReceiver.register(this)
    }

    override fun initImmersionBar() {
        immersionBar {
            transparentNavigationBar()
            transparentStatusBar()
            navigationBarDarkIcon(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initDrawerLayout() {
        val actionBarDrawerToggle =
            ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
            )
        actionBarDrawerToggle.syncState()
        binding.navigationView.setNavigationItemSelectedListener {
            onMenuClick(it)
            return@setNavigationItemSelectedListener true
        }

        val headerBinding = ViewMainHeaderBinding.bind(binding.navigationView.getHeaderView(0))
        headerBinding.title.text = getString(R.string.app_name) + " V" + BuildConfig.VERSION_NAME
    }

    private fun initOtherView() {
        binding.fab.setOnClickListener {
            LocalAppActivity.start(this, -1)
        }
    }

    private fun observeData() {
        viewModel.boxActionState.observe(this, boxActionStateObserver)

        viewModel.appChangeLiveData.observe(this) {
            toolbar.title = "Dashboard"
            toolbar.subtitle = getString(R.string.app_name)
            BoxConfig.currentUserID = 0
            currentUserID = 0
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Tìm menu item bằng ID và ẩn nó
        val userMenuItem = menu?.findItem(R.id.main_name)
        userMenuItem?.isEnabled = false // Ẩn menu
        // Để đổi màu tiêu đề, bạn có thể sử dụng Spannable cho title
        val title = SpannableString(userMenuItem?.title)
        title.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorSecondary)), 0, title.length, 0)
        userMenuItem?.title = title
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onMenuClick(item)
        return super.onOptionsItemSelected(item)
    }

    private fun onMenuClick(item: MenuItem) {

        when (item.itemId) {
            R.id.main_user -> {
                UserSelectActivity.start(this, currentUserID)
            }

            R.id.main_name -> {
                changeUserName()
            }

            R.id.main_import_data -> {
                importData()
            }

            R.id.main_release_memory -> {
                viewModel.stopAll()
                showSnackBar(getString(R.string.release_memory_finish))
            }

            R.id.main_fake_device->{
                PreferenceActivity.start(this,PreferenceActivity.DeviceSetting,currentUserID)
            }


            R.id.main_xposed -> {
                showSnackBar(getString(R.string.developering))
            }

            R.id.main_exit_app -> {
                finish()
            }
            R.id.main_clear_cache -> {
                clearAppCache()
                viewModel.getAppsList()
            }

//            R.id.main_telegram -> {
//                startBrowser("https://t.me/dualmeta")
//            }
            R.id.space -> {
                MainActivity.start(this)
            }
        }

    }

    private fun startBrowser(url: String?) {
        if (url.isNullOrEmpty()) {
            showSnackBar(getString(R.string.config_load_fail))
            return
        }
        runCatching {
            val uri = Uri.parse(url)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri
            startActivity(intent)
        }.onFailure {
            showSnackBar(getString(R.string.open_fail))
        }
    }

    @SuppressLint("CheckResult")
    private fun changeUserName() {
        MaterialDialog(this).show {
            title(R.string.change_name)

            positiveButton(R.string.done)
            negativeButton(R.string.cancel)
            input(prefill = toolbar.title, waitForPositiveButton = true) { _, text ->
                BoxConfig.setUserRemark(currentUserID, text.toString())
                toolbar.title = text
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
         return viewModel.getAppsList();
//        android.util.Log.e(TAG, "=== "+requestCode)
        when (requestCode) {
            UserSelectActivity.REQUEST_CODE -> {
                if (resultCode == UserSelectActivity.RESULT_EMPTY) {
//                    viewModel.getUserList()
                } else if (data != null) {
                    if (currentUserID != currentUserID(data)) {
//                        viewModel.changeUser(currentUserID(data))
                    }
                }
            }

            LocalAppActivity.REQUEST_CODE -> {
                if (data != null) {
//                    android.util.Log.e(TAG, "=== "+data.toString())
//                    viewModel.changeUser(currentUserID(data))
                    viewModel.getAppsList()
                }
            }

            AppInfoActivity.REQUEST_CODE -> {
                if (resultCode == AppInfoActivity.RESULT_UNINSTALL) {
//                    viewModel.changeUser(currentUserID)
                }
            }
        }
    }

    private fun importData() {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_to_setting),
                    getString(R.string.done),
                    getString(R.string.cancel)
                )

            }.request { allGranted, _, _ ->
                if (allGranted) {
                    runCatching {
                        openDocument.launch(Unit)
                    }.onFailure {
                        showSnackBar(getString(R.string.open_fail))
                    }
                } else {
                    showSnackBar(getString(R.string.permission_request_denied))
                }
            }
    }

    private val openDocument = registerForActivityResult(OpenZipDocument()) {
        if (it != null) {
            viewModel.importData(currentUserID, it)
        }
    }

    companion object {
        private const val TAG = "DashBoardActivity"

        fun start(activity: BaseActivity) {
            val intent = Intent(activity, DashboardActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
