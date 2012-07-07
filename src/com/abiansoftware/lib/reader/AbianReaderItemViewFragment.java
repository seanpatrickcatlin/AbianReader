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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class AbianReaderItemViewFragment extends Fragment
{
    private AbianReaderItemView m_itemView;
    private int m_articleNumber;

    public AbianReaderItemViewFragment()
    {
        super();

        m_itemView = null;
        m_articleNumber = 0;
    }

    public void setArticleNumber(int articleNumber)
    {
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        if((articleNumber < 0) || (articleNumber > abianReaderAppData.getNumberOfItems()))
        {
            articleNumber = 0;
        }

        m_articleNumber = articleNumber;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_itemView = (AbianReaderItemView)inflater.inflate(R.layout.abian_reader_item_view, container, false);
        WebView theWebView = (WebView)m_itemView.findViewById(R.id.abian_reader_item_view_webview);

        m_itemView.setWebView(theWebView);

        m_itemView.setTargetRssItem(m_articleNumber);

        return m_itemView;
    }
}
