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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

public class AbianReaderDataFetcher
{
    private static final String TAG = "AbianReaderDataFetcher";

    private static final String RSS_ITEM = "item";
    private static final String RSS_TITLE = "title";
    private static final String RSS_LINK = "link";
    private static final String RSS_DESCRIPTION = "description";
    private static final String RSS_CONTENT = "content:encoded";
    private static final String RSS_PUB_DATE = "pubDate";
    private static final String RSS_CREATOR = "dc:creator";
    private static final String RSS_COMMENTS = "comments";
    private static final String RSS_COMMENT_COUNT = "slash:comments";

    private static final String XML_PAGE = "abianReaderXmlPage";
    private static final String XML_TIME = "abianReaderXmlTime";
    private static final String XML_CREATOR = "abianReaderXmlCreator";
    private static final String XML_CONTENT = "abianReaderXmlContent";
    private static final String XML_COMMENT_COUNT = "abianReaderXmlCommentCount";
    private static final String XML_THUMBNAIL_LINK = "abianReaderXmlThumbnailLink";
    private static final String XML_FEATURED_IMAGE_LINK = "abianReaderXmlFeaturedLink";
    private static final String XML_IS_FEATURED = "abianReaderXmlIsFeatured";

    private static boolean s_bTryJson = false;
    private static String s_featuredTag;

    private RefreshFeedTask m_refreshFeedTask;
    private boolean m_bIsRefreshingFeed;
    private int m_numberOfItemsInFirstPage;
    private boolean m_bNoMoreItemsToFetch;
    private boolean m_bLastConnectionHadError;
    private Vector<AbianReaderItem> m_stagingVector;

    public AbianReaderDataFetcher()
    {
        m_refreshFeedTask = null;
        m_bIsRefreshingFeed = false;
        m_numberOfItemsInFirstPage = 0;
        m_bNoMoreItemsToFetch = false;
        m_bLastConnectionHadError = false;

        m_stagingVector = new Vector<AbianReaderItem>();

        s_featuredTag = AbianReaderApplication.getInstance().getString(R.string.featured_tag);

        String tryJsonStr = AbianReaderApplication.getInstance().getString(R.string.try_json);

        if((tryJsonStr != null) && (tryJsonStr.length() > 0))
        {
            s_bTryJson = true;
        }
    }

    public static String GetFeaturedTag()
    {
        return s_featuredTag;
    }

    static public boolean GetTryJson()
    {
        return s_bTryJson;
    }

    public boolean isRefreshingFeed()
    {
        return m_bIsRefreshingFeed;
    }

    private static boolean isNetworkAvailable()
    {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager)AbianReaderApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();

        for(NetworkInfo ni: netInfo)
        {
            if(ni.getTypeName().equalsIgnoreCase("WIFI"))
            {
                if(ni.isConnected())
                {
                    haveConnectedWifi = true;
                }
            }

            if(ni.getTypeName().equalsIgnoreCase("MOBILE"))
            {
                if(ni.isConnected())
                {
                    haveConnectedMobile = true;
                }
            }
        }

        return haveConnectedWifi || haveConnectedMobile;
    }

    public void getMoreFeed()
    {
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        if(abianReaderAppData != null)
        {
            abianReaderAppData.setPageNumber(abianReaderAppData.getPageNumber() + 1);
        }

        refreshFeed();
    }
    
    public void refreshFeed()
    {
        if((m_refreshFeedTask == null) || (m_refreshFeedTask.getStatus() == AsyncTask.Status.FINISHED) || m_refreshFeedTask.isCancelled())
        {
            try
            {
                m_refreshFeedTask = new RefreshFeedTask();

                String feedUrl = AbianReaderApplication.getInstance().getString(R.string.feed_url_str);

                if(feedUrl.contains("?"))
                {
                    feedUrl += "&";
                }
                else
                {
                    feedUrl += "/?";
                }

                int requestedPageNumber = 1;

                AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

                if(abianReaderAppData != null)
                {
                    if(abianReaderAppData.getNumberOfItems() == 0)
                    {
                        abianReaderAppData.setPageNumber(1);
                    }

                    requestedPageNumber = abianReaderAppData.getPageNumber();
                }

                feedUrl += "paged=" + requestedPageNumber;

                m_refreshFeedTask.execute(feedUrl);
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString());
            }
        }
    }

    private class RefreshFeedTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            String urlToConnectTo = params[0];

            try
            {
                URL urlObject = new URL(urlToConnectTo);
                HttpURLConnection httpConnection = (HttpURLConnection)urlObject.openConnection();
                httpConnection.setConnectTimeout(30 * 1000);
                httpConnection.setReadTimeout(30 * 1000);

                if(httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
                {
                    Log.d(TAG, "getInputStream() HTTP returning null.  Received HTTP response code: " + httpConnection.getResponseCode() + ", Url: " + urlToConnectTo);
                    return null;
                }

                InputStream is = httpConnection.getInputStream();

                if(is != null)
                {
                    SAXParserFactory theSaxParserFactory = SAXParserFactory.newInstance();
                    SAXParser theSaxParser = theSaxParserFactory.newSAXParser();
                    XMLReader theXmlReader = theSaxParser.getXMLReader();

                    theXmlReader.setContentHandler(new RSSFeedHandler());

                    theXmlReader.parse(new InputSource(is));
                }
                else
                {
                    m_bLastConnectionHadError = true;
                }
            }
            catch(Exception e)
            {
                Log.w(getClass().getName(), "Exception thrown: " + e.toString() + ", Url: " + urlToConnectTo);
            }

            return null;
        }

        @Override
        protected void onPreExecute()
        {
            m_stagingVector.clear();
            m_bIsRefreshingFeed = true;
            m_bLastConnectionHadError = false;
            m_bNoMoreItemsToFetch = false;

            AbianReaderApplication.getInstance().sendDataUpdatedMessage();

            if(!isNetworkAvailable())
            {
                cancel(true);

                String toastText = AbianReaderApplication.getInstance().getString(R.string.no_network_str);

                Toast.makeText(AbianReaderApplication.getInstance(), toastText, Toast.LENGTH_SHORT).show();

                m_bLastConnectionHadError = true;
                AbianReaderApplication.getInstance().sendDataUpdatedMessage();
            }
        }

        @Override
        protected void onPostExecute(Void param)
        {
            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

            if(abianReaderAppData == null)
            {
                Log.e(getClass().getName(), "There is no application data!!!");
                return;
            }

            int numberOfFetchedItems = m_stagingVector.size();

            boolean bIsFirstPage = (abianReaderAppData.getPageNumber() == 1);

            if(bIsFirstPage)
            {
                m_numberOfItemsInFirstPage = numberOfFetchedItems;
            }

            for(int i = 0; i < m_stagingVector.size(); i++)
            {
                AbianReaderItem thisItem = m_stagingVector.get(i);

                ArrayList<String> readUrlArrayList = AbianReaderApplication.getInstance().getReadUrlArrayList();

                if(readUrlArrayList != null)
                {
                    for(int arrayListPosition = 0; arrayListPosition < readUrlArrayList.size(); arrayListPosition++)
                    {
                        if(readUrlArrayList.get(arrayListPosition).equalsIgnoreCase(thisItem.getLink()))
                        {
                            thisItem.setArticleHasBeenRead(false);
                            break;
                        }
                    }
                }

                abianReaderAppData.addItem(thisItem);
                AbianReaderApplication.getInstance().sendDataUpdatedMessage();

                if(s_bTryJson)
                {
                    m_stagingVector.get(i).getExtraJsonData(bIsFirstPage);
                }
            }

            m_stagingVector.clear();

            m_bIsRefreshingFeed = false;

            if(m_bLastConnectionHadError)
            {
                // TODO, show a toast message?
            }
            else if(numberOfFetchedItems != m_numberOfItemsInFirstPage)
            {
                m_bNoMoreItemsToFetch = true;
            }

            AbianReaderApplication.getInstance().sendDataUpdatedMessage();
        }
    }

    public boolean getLastConnectionHadError()
    {
        return m_bLastConnectionHadError;
    }

    public boolean getThereAreNoMoreItems()
    {
        return m_bNoMoreItemsToFetch;
    }

    private class RSSFeedHandler extends DefaultHandler
    {
        private AbianReaderData m_abianReaderData = null;
        private StringBuffer m_currentCharactersStringBuffer;

        private String m_title;
        private String m_link;
        private String m_description;
        private String m_content;
        private String m_pubDate;
        private String m_creator;
        private String m_comments;
        private String m_thumbnailLink;
        private String m_featuredImageLink;
        private boolean m_bIsFeatured;
        private int m_commentCount;

        @Override
        public void startDocument() throws SAXException
        {
        }

        @Override
        public void endDocument() throws SAXException
        {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if(qName.compareTo(RSS_ITEM) == 0)
            {
                m_title = "";
                m_description = "";
                m_link = "";
                m_content = "";
                m_pubDate = "";
                m_creator = "";
                m_comments = "";
                m_thumbnailLink = "";
                m_featuredImageLink = "";
                m_bIsFeatured = false;
                m_commentCount = 0;

                m_currentCharactersStringBuffer = new StringBuffer();
            }
            else if((qName.compareTo(RSS_TITLE) == 0) || (qName.compareTo(RSS_LINK) == 0) || (qName.compareTo(RSS_DESCRIPTION) == 0) || (qName.compareTo(RSS_CONTENT) == 0) || (qName.compareTo(RSS_PUB_DATE) == 0)
                    || (qName.compareTo(RSS_CREATOR) == 0) || (qName.compareTo(RSS_COMMENTS) == 0) || (qName.compareTo(RSS_COMMENT_COUNT) == 0) || (qName.compareTo(XML_PAGE) == 0) || (qName.compareTo(XML_TIME) == 0)
                    || (qName.compareTo(XML_CONTENT) == 0) || (qName.compareTo(XML_THUMBNAIL_LINK) == 0) || (qName.compareTo(XML_FEATURED_IMAGE_LINK) == 0) || (qName.compareTo(XML_IS_FEATURED) == 0) || (qName.compareTo(XML_CREATOR) == 0)
                    || (qName.compareTo(XML_COMMENT_COUNT) == 0))
            {
                m_currentCharactersStringBuffer = new StringBuffer();
            }
            else
            {
                m_currentCharactersStringBuffer = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if(m_currentCharactersStringBuffer != null)
            {
                m_currentCharactersStringBuffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            String thisText = "";

            if(m_currentCharactersStringBuffer != null)
            {
                thisText = m_currentCharactersStringBuffer.toString();
            }

            if(qName.compareTo(RSS_ITEM) == 0)
            {
                AbianReaderItem newItem = new AbianReaderItem();

                newItem.setTitle(m_title);
                newItem.setDescription(m_description);
                newItem.setLink(m_link);
                newItem.setContent(m_content);
                newItem.setPubDate(m_pubDate);
                newItem.setCreator(m_creator);
                newItem.setCommentsLink(m_comments);
                newItem.setCommentCount(m_commentCount);
                newItem.setThumbnailLink(m_thumbnailLink);
                newItem.setFeaturedImageLink(m_featuredImageLink, m_bIsFeatured);

                m_stagingVector.add(newItem);

                m_title = "";
                m_description = "";
                m_link = "";
                m_content = "";
                m_pubDate = "";
                m_creator = "";
                m_comments = "";
                m_thumbnailLink = "";
                m_featuredImageLink = "";
                m_bIsFeatured = false;
                m_commentCount = 0;
            }
            else if(qName.compareTo(RSS_TITLE) == 0)
            {
                m_title = thisText;
            }
            else if(qName.compareTo(RSS_LINK) == 0)
            {
                m_link = thisText;
            }
            else if(qName.compareTo(RSS_DESCRIPTION) == 0)
            {
                m_description = thisText;
            }
            else if((qName.compareTo(RSS_CONTENT) == 0) || (qName.compareTo(XML_CONTENT) == 0))
            {
                m_content = thisText;
            }
            else if(qName.compareTo(RSS_PUB_DATE) == 0)
            {
                m_pubDate = thisText;
            }
            else if((qName.compareTo(RSS_CREATOR) == 0) || (qName.compareTo(XML_CREATOR) == 0))
            {
                m_creator = thisText;
            }
            else if(qName.compareTo(RSS_COMMENTS) == 0)
            {
                m_comments = thisText;
            }
            else if(qName.compareTo(XML_THUMBNAIL_LINK) == 0)
            {
                m_thumbnailLink = thisText;
            }
            else if(qName.compareTo(XML_FEATURED_IMAGE_LINK) == 0)
            {
                m_featuredImageLink = thisText;
            }
            else if(qName.compareTo(XML_IS_FEATURED) == 0)
            {
                m_bIsFeatured = Boolean.valueOf(thisText).booleanValue();
            }
            else if((qName.compareTo(RSS_COMMENT_COUNT) == 0) || (qName.compareTo(XML_COMMENT_COUNT) == 0))
            {
                try
                {
                    m_commentCount = Integer.parseInt(thisText);
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString());
                }
            }
            else if(qName.compareTo(XML_PAGE) == 0)
            {
                try
                {
                    m_abianReaderData.setPageNumber(Integer.parseInt(thisText));
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString());
                }
            }
            else if(qName.compareTo(XML_TIME) == 0)
            {
                try
                {
                    m_abianReaderData.setLastUpdateTime(Long.parseLong(thisText));
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString());
                }
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException
        {
            Log.e(TAG, "SAX error: " + e.toString());
        }

        @Override
        public void warning(SAXParseException e) throws SAXException
        {
            Log.w(TAG, "SAX warning: " + e.toString());
        }
    }

    public static int GetNumberOfSameCharacters(String str1, String str2)
    {
        int retVal = 0;

        if((str1 != null) && (str2 != null))
        {
            for(int i = 0; ((i < str1.length()) && (i < str2.length())); i++)
            {
                if(str1.charAt(i) == str2.charAt(i))
                {
                    retVal++;
                }
                else
                {
                    break;
                }
            }
        }

        return retVal;
    }
}
