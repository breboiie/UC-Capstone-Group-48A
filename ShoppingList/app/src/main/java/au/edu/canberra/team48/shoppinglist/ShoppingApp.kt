package au.edu.canberra.team48.shoppinglist

import android.app.Application
import au.edu.canberra.team48.shoppinglist.data.Prefs
import au.edu.canberra.team48.shoppinglist.platform.Notifications
import au.edu.canberra.team48.shoppinglist.platform.ReminderWorker

class ShoppingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        // Keeps the daily check alive after an app update, if reminders are switched on.
        if (Prefs(this).remindOn) ReminderWorker.schedule(this)
    }
}
