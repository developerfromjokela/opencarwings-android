package com.developerfromjokela.opencarwings.sendtocar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.ui.main.location.LocationInfoViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Runnable
import org.openapitools.client.models.MapLinkResolvedLocation
import java.util.regex.Matcher
import java.util.regex.Pattern


class SendToCarActivity : AppCompatActivity() {
    private lateinit var viewModel: SendToCarViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider.create(
            this,
            factory = SendToCarViewModel.Factory
        )[SendToCarViewModel::class]

        enableEdgeToEdge()
        setContentView(R.layout.activity_send_to_car)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val title: TextView = findViewById(R.id.message)
        val warningSymbol: ImageView = findViewById(R.id.errorSign)

        viewModel.uiState.observe(this as LifecycleOwner) { state ->
            if (state.mapLinkResolverResponse?.location != null) {
                val resolvedLocation: MapLinkResolvedLocation = state.mapLinkResolverResponse.location
                viewModel.shareLocation(
                    resolvedLocation.name ?: getString(R.string.location_default_name),
                    LatLng(resolvedLocation.lat.toDouble(), resolvedLocation.lon.toDouble())
                )
            } else if (state.mapLinkResolverResponse != null) {
                progressBar.visibility = View.GONE
                warningSymbol.visibility = View.VISIBLE
                title.setText(R.string.link_send_fail)
                Handler().postDelayed(Runnable {
                    finish()
                }, 4000)
            }

            if (state.isSharingComplete) {
                progressBar.visibility = View.GONE
                warningSymbol.setImageResource(android.R.drawable.checkbox_on_background)
                warningSymbol.visibility = View.VISIBLE
                title.setText(R.string.share_complete)
                Handler().postDelayed(Runnable {
                    finish()
                }, 4000)
            }


            state.genericError?.let {
                progressBar.visibility = View.GONE
                warningSymbol.visibility = View.GONE
                title.text = state.error
                Handler().postDelayed(Runnable {
                    finish()
                }, 4000)
            }
        }

        val receiverdIntent = getIntent()
        val receivedAction = receiverdIntent.getAction()
        val receivedType = receiverdIntent.getType()

        if (receivedAction == Intent.ACTION_SEND) {
            if (receivedType!!.startsWith("text/")) {
                val receivedText = receiverdIntent
                    .getStringExtra(Intent.EXTRA_TEXT)
                if (receivedText != null) {
                    val urls = extractUrls(receivedText)
                    if (urls.isNotEmpty()) {
                        viewModel.linkToLocation(urls.first()!!)
                        return
                    }
                }
            }
        }
        progressBar.visibility = View.GONE
        warningSymbol.visibility = View.VISIBLE
        title.setText(R.string.link_send_fail)
        Handler().postDelayed(Runnable {
            finish()
        }, 4000)
    }

    fun extractUrls(text: String): MutableList<String?> {
        val containedUrls: MutableList<String?> = ArrayList()
        val urlRegex =
            "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)"
        val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlMatcher: Matcher = pattern.matcher(text)

        while (urlMatcher.find()) {
            containedUrls.add(
                text.substring(
                    urlMatcher.start(0),
                    urlMatcher.end(0)
                )
            )
        }

        return containedUrls
    }
}