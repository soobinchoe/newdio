package com.traydcorp.newdio.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_DELETE
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_NEXT
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_PLAY
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_PREV

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionIntent = Intent(context, PlayerService::class.java)
        if (intent?.action != null){
            when (intent.action) {
                ACTION_PLAY -> {
                    actionIntent.putExtra("actionName", intent.action)
                    context?.startService(actionIntent)
                }

                ACTION_PREV -> {
                    actionIntent.putExtra("actionName", intent.action)
                    context?.startService(actionIntent)
                }

                ACTION_NEXT -> {
                    actionIntent.putExtra("actionName", intent.action)
                    context?.startService(actionIntent)
                }

                ACTION_DELETE -> {
                    actionIntent.putExtra("actionName", intent.action)
                    context?.startService(actionIntent)
                }
            }
        }
    }
}