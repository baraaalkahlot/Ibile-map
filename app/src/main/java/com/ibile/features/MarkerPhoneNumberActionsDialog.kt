package com.ibile.features

import android.Manifest.permission.CALL_PHONE
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ibile.R
import com.ibile.core.currentContext
import com.ibile.databinding.DialogViewMarkerPhoneNumberActionsBinding
import com.ibile.utils.extensions.runWithPermissions
import com.ibile.utils.extensions.startResolvableActivityForResult

class MarkerPhoneNumberActionsDialog : DialogFragment() {

    private val args by navArgs<MarkerPhoneNumberActionsDialogArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding =
            DialogViewMarkerPhoneNumberActionsBinding.inflate(LayoutInflater.from(currentContext))
        binding.btnCallPhoneNumber.setOnClickListener {
            runWithPermissions(::callPhone, CALL_PHONE, RC_CALL_PHONE_PERMISSION)
        }
        binding.btnDialPhoneNumber.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${args.phoneNumber}"))
            startResolvableActivityForResult(intent, RC_DIAL_PHONE)
        }
        binding.btnTextPhoneNumber.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${args.phoneNumber}"))
            startResolvableActivityForResult(intent, RC_TEXT_PHONE)
        }
        return AlertDialog.Builder(currentContext, R.style.AlertDialog)
            .setTitle("Phone number options")
            .setView(binding.root)
            .setNegativeButton(getString(R.string.text_cancel)) { _, _ -> }
            .create()
    }

    private fun callPhone() {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${args.phoneNumber}"))
        startResolvableActivityForResult(intent, RC_CALL_PHONE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) return
        when (requestCode) {
            RC_CALL_PHONE_PERMISSION -> callPhone()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        findNavController().popBackStack()
    }

    companion object {
        const val RC_CALL_PHONE_PERMISSION = 1003
        const val RC_CALL_PHONE = 1004
        const val RC_TEXT_PHONE = 1005
        const val RC_DIAL_PHONE = 1006
    }

}