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

import java.io.IOException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;

import com.abiansoftware.lib.reader.R;
import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.widget.LinearLayout;

class AbianReaderItemView extends LinearLayout
{
    private static final String TAG = "AbianReaderItemView";

    private WebView m_webView;
    private int m_targetRssItemNumber;

    private HtmlCleaner m_htmlCleaner;
    private CleanerProperties m_cleanerProps;
    private SimpleHtmlSerializer m_htmlSerializer;

    public AbianReaderItemView(Context context)
    {
        super(context);

        initializeViewBeforePopulation(context);
    }

    public AbianReaderItemView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        initializeViewBeforePopulation(context);
    }

    private void initializeViewBeforePopulation(Context context)
    {
        m_webView = null;
        m_targetRssItemNumber = 0;

        m_cleanerProps = new CleanerProperties();
        m_htmlCleaner = new HtmlCleaner(m_cleanerProps);
        m_htmlSerializer = new SimpleHtmlSerializer(m_cleanerProps);
    }

    public void initializeViewAfterPopulation(Context context)
    {
        m_webView = (WebView)AbianReaderActivity.GetSingleton().findViewById(R.id.abian_reader_item_view_webview);
        m_webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        // m_webView.getSettings().setJavaScriptEnabled(true);
        m_webView.getSettings().setPluginState(PluginState.ON_DEMAND);
        // m_webView.getSettings().setLoadWithOverviewMode(true);
        // m_webView.getSettings().setUseWideViewPort(true);
        m_webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        m_webView.setScrollbarFadingEnabled(true);
    }

    public void clearWebView()
    {
        m_webView.loadData("<html><body></body></html>", "text/html", "UTF-8");
    }

    public int getTargetRssItem()
    {
        return m_targetRssItemNumber;
    }

    public void setTargetRssItem(int itemPosition)
    {
        m_targetRssItemNumber = itemPosition;

        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        if(abianReaderAppData == null)
        {
            Log.e(getClass().getName(), "Data is null!!!");
            return;
        }

        AbianReaderItem theItem = abianReaderAppData.getItemNumber(itemPosition);

        if(theItem != null)
        {
            theItem.setArticleHasBeenRead();

            int nWid = AbianReaderActivity.s_width;
            int nHei = AbianReaderActivity.s_height;

            float thisScale = m_webView.getScale();

            float nScaledWid = (nWid / thisScale);
            float nScaledHei = (nHei / thisScale);

            float nMaxWid = (nScaledWid * 0.9f);
            float nMaxHei = (nScaledHei * 0.9f);

            if(nWid > nHei)
            {
                nMaxHei = (nScaledHei * 0.75f);
            }

            String maxWidStr = Integer.toString((int)nMaxWid);
            String maxHeiStr = Integer.toString((int)nMaxHei);

            String constraints = "{ ";
            constraints += "max-width: " + maxWidStr + "; ";
            constraints += "max-height: " + maxHeiStr + "; ";
            constraints += "width: auto; ";
            constraints += "height: auto; ";
            constraints += "display: block; ";
            constraints += "margin-left: auto; ";
            constraints += "margin-right: auto; ";
            constraints += "}";

            String ourHeadNode = "<head>";
            // use this to tell webview not to scale the webpage
            // ourHeadNode +=
            // "<meta name=\"viewport\" content=\"target-densitydpi=device-dpi\" />";
            ourHeadNode += "<style>";
            ourHeadNode += "img " + constraints;
            ourHeadNode += "\niframe " + constraints;
            ourHeadNode += "\ndiv " + constraints;
            ourHeadNode += "</style>";
            ourHeadNode += "</head>";

            String ourHeader = "<html>" + ourHeadNode + "<body><h2>" + theItem.getTitle() + "</h2>";
            ourHeader += "<small>By " + theItem.getCreator() + " posted " + theItem.getPubDate() + "</small>";

            if(theItem.getFeaturedImageLink().length() != 0)
            {
                ourHeader += "<br /><br />";
                ourHeader += "<a href=\"";
                ourHeader += theItem.getFeaturedImageLink();
                ourHeader += "\">";
                ourHeader += "<img src=\"";
                ourHeader += theItem.getFeaturedImageLink();
                ourHeader += "\" /> </a>";
            }

            // ourHeader += "<br />";

            String ourFooter = "";
            ourFooter += "<br /><a href=\"" + theItem.getLink() + "\" target=\"_blank\">Open the full article in your browser</a><br />";
            ourFooter += "</body></html>";

            String ourHtml = ourHeader + theItem.getContent() + ourFooter;

            TagNode theCleanTagNode = m_htmlCleaner.clean(ourHtml);

            TagNode imgNodes[] = theCleanTagNode.getElementsByName("img", true);

            for(int i = 0; i < imgNodes.length; i++)
            {
                imgNodes[i].removeAttribute("width");
                imgNodes[i].removeAttribute("height");
            }

            TagNode iFrameNodes[] = theCleanTagNode.getElementsByName("iframe", true);

            for(int i = 0; i < iFrameNodes.length; i++)
            {
                iFrameNodes[i].removeAttribute("width");
                iFrameNodes[i].removeAttribute("height");
            }

            try
            {
                ourHtml = m_htmlSerializer.getAsString(theCleanTagNode);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            m_webView.loadDataWithBaseURL(null, ourHtml, "text/html", "UTF-8", null);
            // m_webView.loadDataWithBaseURL(theItem.getLink(), ourHtml,
            // "text/html", "UTF-8", null);
        }
        else
        {
            Log.e(TAG, "TheItem is null");
        }
    }
}
