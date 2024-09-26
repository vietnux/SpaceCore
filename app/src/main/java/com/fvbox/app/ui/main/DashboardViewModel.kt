package com.fvbox.app.ui.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fvbox.R
import com.fvbox.app.base.BaseViewModel
import com.fvbox.app.config.BoxConfig
import com.fvbox.data.BackRepository
import com.fvbox.data.BoxRepository
import com.fvbox.data.bean.box.BoxAppBean
import com.fvbox.data.bean.box.BoxUserInfo
import com.fvbox.data.state.BoxActionState
import com.fvbox.data.state.BoxAppState
import com.fvbox.util.extension.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DashboardViewModel : BaseViewModel() {

    /**
     * 用户切换
     */
    private val mAppChangeLiveData = MutableLiveData<BoxAppState>()
    val appChangeLiveData: LiveData<BoxAppState> = mAppChangeLiveData


    private val userList: MutableList<BoxUserInfo?> = mutableListOf()
    private var appList: MutableList<BoxAppBean> = mutableListOf()

    init {
        getAppsList()
    }
    /**
     * 没用户就创建
     * 有用户就post通知fragment
     */
    fun getAppsList() {
        appList.clear()
        userList.clear()
        userList.addAll(BoxRepository.getUserList())
        userList.forEach {
            if (it != null) {
                mAppChangeLiveData.postValue(BoxAppState.Loading)
//                freshBoxAppList(it.userID)
                val list = BoxRepository.getBoxAppList(it.userID)
                appList += list;
            }
        }
        launchIO {
            if (appList.isEmpty()) {
                mAppChangeLiveData.postValue(BoxAppState.Empty)
            } else {
                mAppChangeLiveData.postValue(BoxAppState.Success(appList))
            }
        }

    }

    /**
     * 清除所有后台，释放内存
     */
    fun stopAll() {
        BoxRepository.stopAllPackage()
    }


    /**
     * 导入数据
     * @param userID Int
     * @param uri Uri
     */
    fun importData(userID: Int, uri: Uri) {
        launch {
            mBoxActionState.postValue(BoxActionState.Loading)
            val msg = withContext(Dispatchers.IO) {
                BackRepository.importData(userID, uri)
            }
            if (msg.isNullOrEmpty()) {
                mBoxActionState.postValue(BoxActionState.Success(getString(R.string.import_success)))
            } else {
                mBoxActionState.postValue(BoxActionState.Fail(msg))
            }
        }
    }

}
