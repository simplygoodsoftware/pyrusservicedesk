package com.pyrus.servicedesksample;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.pyrus.pyrusservicedesk.MainMenuDelegate;

class ChatMenuDelegate implements MainMenuDelegate {

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu, @NonNull Activity activity) {
       new MenuInflater(activity).inflate(R.menu.custom_chat_menu, menu);

        MenuItem locationItem = menu.findItem(R.id.chat_menu_item_star);
        locationItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        locationItem.setIcon(getDrawable(R.drawable.ic_star, activity.getApplicationContext()));

        MenuItem infoItem = menu.findItem(R.id.chat_menu_item_thumb_up);
        infoItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        infoItem.setIcon(getDrawable(R.drawable.ic_thumb_up, activity.getApplicationContext()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item, @NonNull Activity activity) {

        if (item.getItemId() == R.id.chat_menu_item_star)
            Toast.makeText(activity, "On star clicked", Toast.LENGTH_LONG).show();
        else if (item.getItemId() == R.id.chat_menu_item_thumb_up)
            Toast.makeText(activity, "On thumb up clicked", Toast.LENGTH_LONG).show();
        else
            return false;
        return true;
    }

    private Drawable getDrawable(int iconRes, Context context) {
        return ResourcesCompat.getDrawable(context.getResources(), iconRes, context.getTheme());
    }
}
