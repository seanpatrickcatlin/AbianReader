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

import org.apache.http.client.params.ClientPNames;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Application;

public class AbianReaderApplication extends Application
{
    private static AsyncHttpClient s_asyncHttpClient = null;
    private static AbianReaderApplication s_singleton = null;
    private AbianReaderData m_data;

    @Override
    public void onCreate()
    {
        s_singleton = this;

        s_asyncHttpClient = new AsyncHttpClient();
        s_asyncHttpClient.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

        m_data = new AbianReaderData();
        
        super.onCreate();
    }

    public static AbianReaderApplication getInstance()
    {
        return s_singleton;
    }

    public static AbianReaderData getData()
    {
        if(s_singleton.m_data == null)
        {
            s_singleton.m_data = new AbianReaderData();
        }

        return s_singleton.m_data;
    }

    public static void DoHttpGet(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        s_asyncHttpClient.get(url, params, responseHandler);
    }

    public static void DoHttpBinaryGet(String url, BinaryHttpResponseHandler responseHandler)
    {
        s_asyncHttpClient.get(url, responseHandler);
    }
    
}
