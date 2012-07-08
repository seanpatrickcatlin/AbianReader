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

/*
 TODO List
 Short Term Goals
 - Cache images (thumbnails and feature image downloads)

 Long Term Goals
 - Have multiple lists that you can swipe between, "Latest", "Features", "Android", etc...
 - View Comments in App - Possible, I think this is in the JSON
 - Add a search feature - Possible, just takes time
 - Leave comments in App, Can't Happen Right now... I think
 */

package com.abiansoftware.lib.reader;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.http.client.params.ClientPNames;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class AbianReaderApplication extends Application
{
    private static final String READ_URL_PREFERENCES = "AbianReaderReadUrlList";
    private static final String KEY_READ_URL_LIST = "readUrlList";
    
    private static AsyncHttpClient s_asyncHttpClient = null;
    private static AbianReaderApplication s_singleton = null;
    private AbianReaderData m_data;
    private AbianReaderDataFetcher m_dataFetcher;

    public static final String CHOSEN_ARTICLE_NUMBER = "AbianReaderChosenArticleNumber";
    public static final float FEATURED_IMAGE_SIZE = 2.5f;
    public static final int MSG_DATA_UPDATED = 22609;
    

    public static int s_width = 100;
    public static int s_height = 100;

    private Vector<Handler> m_handlerVector;

    private boolean m_bSplashScreenHasBeenShown;

    private ArrayList<String> m_readUrlArrayList;

    @Override
    public void onCreate()
    {
        s_singleton = this;

        s_asyncHttpClient = new AsyncHttpClient();
        s_asyncHttpClient.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

        m_data = new AbianReaderData();
        m_dataFetcher = new AbianReaderDataFetcher();

        WindowManager theWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display theDefaultDisplay = theWindowManager.getDefaultDisplay();

        DisplayMetrics theDisplayMetrics = new DisplayMetrics();
        theDefaultDisplay.getMetrics(theDisplayMetrics);
        s_width = theDisplayMetrics.widthPixels;
        s_height = theDisplayMetrics.heightPixels;

        m_handlerVector = new Vector<Handler>();

        m_bSplashScreenHasBeenShown = false;

        m_readUrlArrayList = null;

        loadReadUrlList();

        super.onCreate();
    }

    public static AbianReaderApplication getInstance()
    {
        return s_singleton;
    }

    public static AbianReaderData getData()
    {
        if(s_singleton == null)
        {
            return null;
        }

        if(s_singleton.m_data == null)
        {
            s_singleton.m_data = new AbianReaderData();
        }

        return s_singleton.m_data;
    }

    public static AbianReaderDataFetcher getDataFetcher()
    {
        if(s_singleton == null)
        {
            return null;
        }

        if(s_singleton.m_dataFetcher == null)
        {
            s_singleton.m_dataFetcher = new AbianReaderDataFetcher();
        }

        return s_singleton.m_dataFetcher;
    }

    public static void DoHttpGet(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        s_asyncHttpClient.get(url, params, responseHandler);
    }

    public static void DoHttpBinaryGet(String url, BinaryHttpResponseHandler responseHandler)
    {
        s_asyncHttpClient.get(url, responseHandler);
    }

    public void registerHandler(Handler newHandler)
    {
        m_handlerVector.add(newHandler);
    }

    public void unregisterHandler(Handler oldHandler)
    {
        m_handlerVector.remove(oldHandler);
    }

    public void sendDataUpdatedMessage()
    {
        for(int i = 0; i < m_handlerVector.size(); i++)
        {
            Handler thisHandler = m_handlerVector.get(i);

            if(thisHandler != null)
            {
                thisHandler.sendEmptyMessage(AbianReaderApplication.MSG_DATA_UPDATED);
            }
        }
    }

    public void setSplashScreenHasBeenShown()
    {
        m_bSplashScreenHasBeenShown = true;
    }

    public boolean getSplashScreenHasBeenShown()
    {
        return m_bSplashScreenHasBeenShown;
    }

    private void loadReadUrlList()
    {
        if(m_readUrlArrayList == null)
        {
            m_readUrlArrayList = new ArrayList<String>();
        }

        m_readUrlArrayList.clear();

        SharedPreferences preferences = getSharedPreferences(READ_URL_PREFERENCES, MODE_PRIVATE);
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
    }

    public void saveReadUrlList()
    {
        SharedPreferences preferences = getSharedPreferences(READ_URL_PREFERENCES, MODE_PRIVATE);
        Editor theEditor = preferences.edit();
        theEditor.clear();

        if(m_readUrlArrayList == null)
        {
            m_readUrlArrayList = new ArrayList<String>();
        }

        m_readUrlArrayList.clear();

        for(int i = 0; i < m_data.getNumberOfItems(); i++)
        {
            AbianReaderItem thisItem = m_data.getItemNumber(i);

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
    }

    public ArrayList<String> getReadUrlArrayList()
    {
        return m_readUrlArrayList;
    }
}
