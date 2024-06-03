package com.prng.aeedee_android_chat.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop

class UCropContract : ActivityResultContract<UCropInput, UCropResult>() {

    override fun createIntent(context: Context, input: UCropInput): Intent {
        return UCrop.of(input.sourceUri, input.destinationUri)
            .withAspectRatio(input.aspectRatioX, input.aspectRatioY)
            .withMaxResultSize(input.maxSizeX, input.maxSizeY)
            .withOptions(input.options)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): UCropResult {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            UCropResult(resultCode, UCrop.getOutput(intent))
        } else {
            UCropResult(resultCode, null, intent?.let { UCrop.getError(it) })
        }
    }
}

data class UCropInput(
    val sourceUri: Uri,
    val destinationUri: Uri,
    val aspectRatioX: Float = 1f,
    val aspectRatioY: Float = 1f,
    val maxSizeX: Int = 5000,
    val maxSizeY: Int = 5000,
    val options: UCrop.Options = UCrop.Options()
)

data class UCropResult(
    val resultCode: Int,
    val uri: Uri? = null,
    val error: Throwable? = null
)