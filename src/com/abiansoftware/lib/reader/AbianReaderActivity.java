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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.abiansoftware.lib.reader.R;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AbianReaderActivity extends SherlockFragmentActivity
{
    // private static final String TAG = "AbianReaderActivity";

    public static final int MSG_UPDATE_LIST = 22609;

    public static final int REFRESH_ITEM_ID = 22610;

    private static final String KEY_READ_URL_LIST = "readUrlList";

    private AbianReaderListView m_rssFeedListView;

    private Handler m_activityHandler;

    private ArrayList<String> m_readUrlArrayList;

    /** Called when the activity is first created. */
    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_activityHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what == MSG_UPDATE_LIST)
                {
                    updateListView();
                }
            }
        };

        // set the view
        setContentView(R.layout.abian_reader_activity);

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
        
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        if(abianReaderAppData == null)
        {
            Log.e(getClass().getName(), "Data is null!!!");
            return;
        }

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Editor theEditor = preferences.edit();
        theEditor.clear();

        if(m_readUrlArrayList == null)
        {
            m_readUrlArrayList = new ArrayList<String>();
        }

        m_readUrlArrayList.clear();

        for(int i = 0; i < abianReaderAppData.getNumberOfItems(); i++)
        {
            AbianReaderItem thisItem = abianReaderAppData.getItemNumber(i);

            if(thisItem.getHasArticleBeenRead())
            {
                m_readUrlArrayList.add(thisItem.getLink());
            }
        }

        if(m_readUrlArrayList.size() > 0)
        {
            for(int i = 0; i < m_readUrlArrayList.size(); i++)
            {
                theEditor.putString(m_readUrlArrayList.get(i), KEY_READ_URL_LIST);
            }
        }

        theEditor.commit();

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        AbianReaderApplication.getInstance().registerHandler(m_activityHandler);

        if(m_readUrlArrayList == null)
        {
            m_readUrlArrayList = new ArrayList<String>();
        }

        m_readUrlArrayList.clear();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Map<String, ?> prefMap = preferences.getAll();

        Set<String> mapKeys = prefMap.keySet();

        for(String thisKey: mapKeys)
        {
            if(prefMap.get(thisKey) instanceof String)
            {
                String thisValue = (String)prefMap.get(thisKey);

                if(thisValue.equalsIgnoreCase(KEY_READ_URL_LIST))
                {
                    m_readUrlArrayList.add(thisKey);
                }
            }
        }

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
}
