/*
This file is part of AbianReader.

AbianReader is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AbianReader is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AbianReader.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.abiansoftware.lib.reader;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.abiansoftware.lib.reader.R;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AbianReaderActivity extends SherlockFragmentActivity
{
    // private static final String TAG = "AbianReaderActivity";

    public static final int REFRESH_ITEM_ID = 22610;

    private AbianReaderListView m_rssFeedListView;

    private Handler m_activityHandler;

    private Dialog m_splashScreenDialog;

    /** Called when the activity is first created. */
    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // set the view
        setContentView(R.layout.abian_reader_activity);

        m_splashScreenDialog = null;

        m_activityHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what == AbianReaderApplication.MSG_DATA_UPDATED)
                {
                    updateListView();
                }
            }
        };

        m_rssFeedListView = new AbianReaderListView();

        m_rssFeedListView.initializeViewAfterPopulation(this);

        if(AbianReaderApplication.getData().getNumberOfItems() == 0)
        {
            AbianReaderDataFetcher abianReaderAppDataFetcher = AbianReaderApplication.getDataFetcher();

            if(abianReaderAppDataFetcher != null)
            {
                abianReaderAppDataFetcher.refreshFeed();
            }
        }

        showSplashScreen();
    }

    public int getPreferredListItemHeight()
    {
        return m_rssFeedListView.getPreferredListItemHeight();
    }

    private void updateListView()
    {
        m_rssFeedListView.updateList();
    }

    @Override
    protected void onPause()
    {
        AbianReaderApplication.getInstance().unregisterHandler(m_activityHandler);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        AbianReaderApplication.getInstance().registerHandler(m_activityHandler);
        updateListView();

        super.onResume();
    }

    public static void openUrlInBrowser(String targetUrl)
    {
        if((targetUrl != null) && (targetUrl.length() > 0))
        {
            if((!(targetUrl.startsWith("http://")) && !(targetUrl.startsWith("https://"))))
            {
                targetUrl = "http://" + targetUrl;
            }

            Intent thisIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            AbianReaderApplication.getInstance().startActivity(thisIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem refreshMenuItem = menu.add(Menu.NONE, AbianReaderActivity.REFRESH_ITEM_ID, Menu.NONE, "Refresh");
        refreshMenuItem.setIcon(R.drawable.refresh);
        refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == REFRESH_ITEM_ID)
        {
            AbianReaderApplication.getData().clear();
            AbianReaderApplication.getData().setPageNumber(1);

            AbianReaderApplication.getDataFetcher().refreshFeed();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSplashScreen()
    {
        if(m_splashScreenDialog != null)
        {
            dismissSplashScreen();
        }

        if(!AbianReaderApplication.getInstance().getSplashScreenHasBeenShown())
        {
            m_splashScreenDialog = new Dialog(this, R.style.SplashScreen);
            m_splashScreenDialog.setContentView(R.layout.abian_reader_splash_screen_layout);
            m_splashScreenDialog.setCancelable(false);
            m_splashScreenDialog.show();

            // Set Runnable to remove splash screen just in case
            final Handler splashScreenHandler = new Handler();

            splashScreenHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    dismissSplashScreen();
                }
            }, 2500);

        }
    }

    private void dismissSplashScreen()
    {
        if(m_splashScreenDialog != null)
        {
            m_splashScreenDialog.dismiss();
            m_splashScreenDialog = null;

            AbianReaderApplication.getInstance().setSplashScreenHasBeenShown();
        }
    }
}
