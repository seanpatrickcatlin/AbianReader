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
 Long Term Goals
 - View Comments in App - Possible, I think this is in the JSON
 - Swipe between Articles when reading one - Possible, just takes time
 - Have multiple lists that you can swipe between, "Latest", "Features", "Android", etc...
 - Add a search feature - Possible, just takes time
 - Leave comments in App, Can't Happen Right now... I think
 */

package com.abiansoftware.lib.reader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.abiansoftware.lib.reader.R;

public class AbianReaderActivity extends FragmentActivity implements OnClickListener
{
    private static final String TAG = "AbianReaderActivity";
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

    private static final int MSG_UPDATE_LIST = 22609;
    private static final int MSG_HIDE_SPLASH_SCREEN = 22610;

    private static final String KEY_READ_URL_LIST = "readUrlList";

    private static boolean s_bTryJson = false;
    private static boolean s_bUseJsonContent = false;

    private static String s_featuredTag;

    public static int s_width = 100;
    public static int s_height = 100;

    private static AbianReaderActivity s_singleton = null;

    private RefreshFeedTask m_refreshFeedTask;

    private AbianReaderListView m_rssFeedListView;
    private AbianReaderItemView m_rssItemView;

    private ImageView m_refreshImageView;
    private ImageView m_headerImageView;
    private ImageView m_settingsImageView;

    private boolean m_bIsRefreshingFeed;

    private int m_preferredListItemHeight;

    private int m_numberOfItemsInFirstPage;

    private boolean m_bNoMoreItemsToFetch;
    private boolean m_bLastConnectionHadError;

    private static Handler s_appHandler = null;

    private Vector<AbianReaderItem> m_stagingVector;

    private ImageView m_splashScreenView;
    private LinearLayout m_mainAppView;

    private ArrayList<String> m_readUrlArrayList;

    public static String GetFeaturedTag()
    {
        return s_featuredTag;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_refreshFeedTask = null;
        m_stagingVector = new Vector<AbianReaderItem>();

        m_bNoMoreItemsToFetch = false;
        m_bLastConnectionHadError = false;

        m_preferredListItemHeight = 40;

        m_numberOfItemsInFirstPage = 0;

        s_singleton = this;

        if(s_appHandler == null)
        {
            s_appHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    if(msg.what == MSG_UPDATE_LIST)
                    {
                        updateListView();
                    }
                    else if(msg.what == MSG_HIDE_SPLASH_SCREEN)
                    {
                        m_splashScreenView.setVisibility(View.GONE);
                        m_mainAppView.setVisibility(View.VISIBLE);
    
                        showMainListView();
                    }
                }
            };
        }

        s_featuredTag = getString(R.string.featured_tag);

        String tryJsonStr = getString(R.string.try_json);

        if((tryJsonStr != null) && (tryJsonStr.length() > 0))
        {
            s_bTryJson = true;
        }

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // set the view
        setContentView(R.layout.abian_reader_activity);

        m_splashScreenView = (ImageView)findViewById(R.id.app_splash_screen_layout);
        m_mainAppView = (LinearLayout)findViewById(R.id.app_main_layout);
        m_rssItemView = (AbianReaderItemView)findViewById(R.id.abian_reader_item_view);
        m_rssFeedListView = (AbianReaderListView)findViewById(R.id.abian_reader_list_view);

        LinearLayout appHeaderLinearLayout = (LinearLayout)findViewById(R.id.app_header_linear_layout);

        ViewGroup.LayoutParams appHeaderLayoutParams = appHeaderLinearLayout.getLayoutParams();
        m_preferredListItemHeight = appHeaderLayoutParams.height;

        m_refreshImageView = (ImageView)findViewById(R.id.refresh_button);
        m_headerImageView = (ImageView)findViewById(R.id.header_button);
        m_settingsImageView = (ImageView)findViewById(R.id.settings_button);

        m_refreshImageView.setOnClickListener(this);
        m_headerImageView.setOnClickListener(this);
        m_settingsImageView.setOnClickListener(this);

        m_rssItemView.initializeViewAfterPopulation(this);
        m_rssFeedListView.initializeViewAfterPopulation(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        s_width = metrics.widthPixels;
        s_height = metrics.heightPixels;

        refreshFeed();

        m_mainAppView.setVisibility(View.GONE);
        m_splashScreenView.setVisibility(View.VISIBLE);

        if(AbianReaderActivity.s_appHandler != null)
        {
            AbianReaderActivity.s_appHandler.sendEmptyMessageDelayed(MSG_HIDE_SPLASH_SCREEN, 2000);
        }
    }

    static public void UpdateListPlease()
    {
        if(AbianReaderActivity.s_appHandler != null)
        {
            AbianReaderActivity.s_appHandler.sendEmptyMessage(MSG_UPDATE_LIST);
        }
    }

    static public boolean GetTryJson()
    {
        return s_bTryJson;
    }

    static public AbianReaderActivity GetSingleton()
    {
        return s_singleton;
    }

    public int getPreferredListItemHeight()
    {
        return m_preferredListItemHeight;
    }

    public boolean isRefreshingFeed()
    {
        return m_bIsRefreshingFeed;
    }

    private void updateListView()
    {
        m_rssFeedListView.updateList();
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

                String feedUrl = s_singleton.getString(R.string.feed_url_str);

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

    public void showRssItemContent(int position)
    {
        m_rssItemView.setTargetRssItem(position);
        m_rssFeedListView.setVisibility(View.GONE);
        m_rssItemView.setVisibility(View.VISIBLE);

        m_refreshImageView.setImageResource(R.drawable.share);
        m_settingsImageView.setImageResource(R.drawable.comments);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0))
        {
            if(showMainListView())
            {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean isNetworkAvailable()
    {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    if(s_bTryJson && s_bUseJsonContent)
                    {
                        parseJsonFeed(is);
                    }
                    else
                    {
                        SAXParserFactory theSaxParserFactory = SAXParserFactory.newInstance();
                        SAXParser theSaxParser = theSaxParserFactory.newSAXParser();
                        XMLReader theXmlReader = theSaxParser.getXMLReader();

                        theXmlReader.setContentHandler(new RSSFeedHandler());

                        theXmlReader.parse(new InputSource(is));
                    }
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

            updateListView();

            if(!isNetworkAvailable())
            {
                cancel(true);

                String toastText = s_singleton.getString(R.string.no_network_str);

                Toast.makeText(s_singleton, toastText, Toast.LENGTH_SHORT).show();

                m_bLastConnectionHadError = true;
                updateListView();
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

                if(m_readUrlArrayList != null)
                {
                    for(int arrayListPosition = 0; arrayListPosition < m_readUrlArrayList.size(); arrayListPosition++)
                    {
                        if(m_readUrlArrayList.get(arrayListPosition).equalsIgnoreCase(thisItem.getLink()))
                        {
                            thisItem.setArticleHasBeenRead();
                            break;
                        }
                    }
                }

                abianReaderAppData.addItem(thisItem);
                updateListView();

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

            updateListView();
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

    private void parseJsonFeed(InputStream is)
    {
        try
        {
            // just get all of the data and store it in a string
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr, (1024 * 2));
            StringBuilder sb = new StringBuilder();

            String line = null;
            while((line = br.readLine()) != null)
            {
                sb.append(line + "\n");
            }

            is.close();

            JSONObject rootJsonObject = new JSONObject(sb.toString());

            JSONArray postArray = rootJsonObject.getJSONArray("posts");

            for(int i = 0; i < postArray.length(); i++)
            {
                JSONObject thisPostObject = postArray.getJSONObject(i);

                AbianReaderItem newItem = new AbianReaderItem();

                newItem.setTitle(thisPostObject.getString("title_plain"));
                newItem.setDescription(thisPostObject.getString("excerpt"));
                newItem.setLink(thisPostObject.getString("url"));
                newItem.setContent(thisPostObject.getString("content"));
                newItem.setJsonDate(thisPostObject.getString("date"));

                JSONObject thisPostAuthor = thisPostObject.getJSONObject("author");
                newItem.setCreator(thisPostAuthor.getString("name"));
                newItem.setCommentsLink(newItem.getLink());

                JSONArray thisCommentArray = thisPostObject.getJSONArray("comments");
                newItem.setCommentCount(thisCommentArray.length());

                newItem.setThumbnailLink(thisPostObject.getString("thumbnail"));

                m_stagingVector.add(newItem);
            }

        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString());
        }
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

    @Override
    public void onClick(View v)
    {
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        if(abianReaderAppData == null)
        {
            Log.e(getClass().getName(), "Data is null!!!");
            return;
        }

        if(v.getId() == R.id.refresh_button)
        {
            if(m_rssItemView.getVisibility() == View.VISIBLE)
            {
                int itemPosition = m_rssItemView.getTargetRssItem();

                AbianReaderItem targetItem = abianReaderAppData.getItemNumber(itemPosition);

                if(targetItem != null)
                {
                    String shareMessage = getString(R.string.share_message);
                    String shareTitle = getString(R.string.share_title);

                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareMessage);
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, targetItem.getLink());
                    startActivity(Intent.createChooser(sharingIntent, shareTitle));
                }
            }
            else
            {   
                abianReaderAppData.clear();
                abianReaderAppData.setPageNumber(1);
                updateListView();

                refreshFeed();
            }
        }
        else if(v.getId() == R.id.settings_button)
        {
            if(m_rssItemView.getVisibility() == View.VISIBLE)
            {
                int itemPosition = m_rssItemView.getTargetRssItem();

                AbianReaderItem targetItem = abianReaderAppData.getItemNumber(itemPosition);

                if(targetItem != null)
                {
                    AbianReaderActivity.openUrlInBrowser(targetItem.getCommentsLink());
                }
            }
        }
    }

    public static void openUrlInBrowser(String targetUrl)
    {
        if((targetUrl != null) && (targetUrl.length() > 0) && (s_singleton != null))
        {
            if((!(targetUrl.startsWith("http://")) && !(targetUrl.startsWith("https://"))))
            {
                targetUrl = "http://" + targetUrl;
            }

            Intent thisIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            s_singleton.startActivity(thisIntent);
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

    private boolean showMainListView()
    {
        if(m_rssItemView.getVisibility() == View.VISIBLE)
        {
            m_rssItemView.clearWebView();
            m_rssItemView.setVisibility(View.GONE);
            m_rssFeedListView.setVisibility(View.VISIBLE);

            m_refreshImageView.setImageResource(R.drawable.refresh);
            m_settingsImageView.setImageResource(R.drawable.settings);

            updateListView();

            return true;
        }

        return false;
    }

    @Override
    protected void onPause()
    {
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
        if(m_readUrlArrayList == null)
        {
            m_readUrlArrayList = new ArrayList<String>();
        }

        m_readUrlArrayList.clear();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Map<String,?> prefMap = preferences.getAll();
        
        Set<String> mapKeys = prefMap.keySet();

        for(String thisKey : mapKeys)
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
}
