package com.ibile.features.main.editfolder

import android.view.View
import androidx.databinding.adapters.TextViewBindingAdapter

interface ViewBindingData {
    val iconBtnClickListener: View.OnClickListener
    val colorBtnClickListener: View.OnClickListener
    val folderNameInputChangeListener: TextViewBindingAdapter.AfterTextChanged

    val data: EditFolderViewModel
}