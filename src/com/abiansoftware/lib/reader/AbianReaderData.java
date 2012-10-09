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

/*
import java.io.File;
import java.io.FileOutputStream;
*/
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.text.DateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.loopj.android.http.*;

import android.os.Looper;
import android.text.format.Time;
import android.util.Log;

public class AbianReaderData
{
    private static final String TAG = "AbianReaderData";

    public static final int MAX_DATA_ITEMS = 100;

    //private static Object SYNC_OBJ = new Object();

    static public class AbianReaderItem
    {
        private String m_itemTitle;
        private String m_itemLink;
        private String m_itemContent;
        private String m_itemDescription;
        private Date m_itemPubDate;
        private String m_itemCreator;
        private String m_itemCommentsLink;
        private int m_itemCommentCount;
        private String m_thumbnailLink;
        private Bitmap m_thumbnailBitmap;
        private String m_featuredImageLink;
        private Bitmap m_featuredImageBitmap;
        private boolean m_bIsFeatured;
        @SuppressWarnings("unused")
        private boolean m_bIsGettingThumbnail;
        @SuppressWarnings("unused")
        private boolean m_bIsGettingFeatureImage;
        @SuppressWarnings("unused")
        private boolean m_bIsGettingExtraJsonData;

        private boolean m_bHasBeenRead;

        public AbianReaderItem()
        {
            m_itemTitle = "";
            m_itemLink = "";
            m_itemContent = "";
            m_itemDescription = "";
            m_itemPubDate = new Date();
            m_itemCreator = "";
            m_itemCommentsLink = "";
            m_itemCommentCount = 0;
            m_thumbnailLink = "";
            m_thumbnailBitmap = null;
            m_featuredImageLink = "";
            m_bIsFeatured = false;
            m_bHasBeenRead = false;

            m_bIsGettingThumbnail = false;
            m_bIsGettingFeatureImage = false;
            m_bIsGettingExtraJsonData = false;
        }

        public String getTitle()
        {
            return m_itemTitle;
        }

        public String getLink()
        {
            return m_itemLink;
        }

        public String getContent()
        {
            return m_itemContent;
        }

        public String getDescription()
        {
            return m_itemDescription;
        }

        public String getPubDate()
        {
            String dateStr = DateFormat.getDateInstance(DateFormat.LONG).format(m_itemPubDate);
            String timeStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(m_itemPubDate);
            return dateStr + " at " + timeStr;
        }

        public String getPubDateOnly()
        {
            return DateFormat.getDateInstance(DateFormat.LONG).format(m_itemPubDate);
        }

        public String getPubDateLong()
        {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

            return sdf.format(m_itemPubDate);
        }

        public String getCreator()
        {
            return m_itemCreator;
        }

        public String getCommentsLink()
        {
            return m_itemCommentsLink;
        }

        public int getCommentsCount()
        {
            return m_itemCommentCount;
        }

        public void setTitle(String itemTitle)
        {
            if(itemTitle == null)
            {
                itemTitle = "";
            }

            m_itemTitle = itemTitle;
        }

        public void setLink(String itemLink)
        {
            if(itemLink == null)
            {
                itemLink = "";
            }

            m_itemLink = itemLink;
        }

        public void setContent(String itemContent)
        {
            if(itemContent == null)
            {
                itemContent = "";
            }

            m_itemContent = itemContent;
        }

        public void setDescription(String itemDescription)
        {
            if(itemDescription == null)
            {
                itemDescription = "";
            }

            m_itemDescription = itemDescription;
        }

        public void setPubDate(String itemPubDate)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

            if(itemPubDate == null)
            {
                itemPubDate = "";
            }

            try
            {
                m_itemPubDate = sdf.parse(itemPubDate);
            }
            catch(ParseException e)
            {
                Log.e(TAG, e.toString());
            }
        }

        public void setJsonDate(String itemPubDate)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            if(itemPubDate == null)
            {
                itemPubDate = "";
            }

            try
            {
                m_itemPubDate = sdf.parse(itemPubDate);
            }
            catch(ParseException e)
            {
                Log.e(TAG, e.toString());
            }
        }

        public void setCreator(String itemCreator)
        {
            if(itemCreator == null)
            {
                itemCreator = "";
            }

            m_itemCreator = itemCreator;
        }

        public void setCommentsLink(String itemCommentsLink)
        {
            if(itemCommentsLink == null)
            {
                itemCommentsLink = "";
            }

            m_itemCommentsLink = itemCommentsLink;
        }

        public void setCommentCount(int itemCommentCount)
        {
            if(itemCommentCount < 0)
            {
                itemCommentCount = 0;
            }

            m_itemCommentCount = itemCommentCount;
        }

        public void setThumbnailLink(String thumbnailLink)
        {
            if(thumbnailLink == null)
            {
                thumbnailLink = "";
            }

            m_thumbnailLink = thumbnailLink;

            AbianReaderApplication.getInstance().sendDataUpdatedMessage();

            /*
            if(thumbnailLink.length() > 0)
            {
                // check to see if this image has been cached already
                File thumbnailFile = new File(getThumbnailImageCacheFilePath());

                if(thumbnailFile.exists())
                {
                    loadThumbnailImageFromCache();
                }
                else
                {
                    String[] allowedContentTypes = new String[] { "image/png", "image/jpeg", "image/gif" };

                    m_bIsGettingThumbnail = true;

                    AbianReaderApplication.DoHttpBinaryGet(thumbnailLink, new BinaryHttpResponseHandler(allowedContentTypes)
                    {
                        @Override
                        public void onSuccess(byte[] fileData)
                        {
                            File thumbnailFile = new File(getThumbnailImageCacheFilePath());

                            try
                            {
                                FileOutputStream thumbnailFileOutputStream = new FileOutputStream(thumbnailFile);
                                thumbnailFileOutputStream.write(fileData);
                                thumbnailFileOutputStream.flush();
                                thumbnailFileOutputStream.close();

                                loadThumbnailImageFromCache();
                            }
                            catch(Exception e)
                            {
                                Log.e(TAG, e.toString());
                            }

                            m_bIsGettingThumbnail = false;
                        }

                        public void onFailure(Throwable e, byte[] imageData)
                        {
                            Log.e(TAG, "AsyncHttp failed thumb");

                            m_bIsGettingThumbnail = false;
                        }
                    });
                }
            }
            */
        }

        /*
        private String getThumbnailImageCacheFilePath()
        {
            File appCacheDirectory = AbianReaderApplication.getInstance().getCacheDir();

            String thumbnailImageFileName = m_thumbnailLink.substring(m_thumbnailLink.lastIndexOf('/') + 1, m_thumbnailLink.length());

            String thumbnailImageFilePath = appCacheDirectory.getAbsolutePath() + '/' + thumbnailImageFileName;

            return thumbnailImageFilePath;
        }

        private String getFeatureImageCacheFilePath()
        {
            File appCacheDirectory = AbianReaderApplication.getInstance().getCacheDir();

            String featureImageFileName = m_featuredImageLink.substring(m_featuredImageLink.lastIndexOf('/') + 1, m_featuredImageLink.length());

            String featureImageFilePath = appCacheDirectory.getAbsolutePath() + '/' + featureImageFileName;

            return featureImageFilePath;            
        }

        private void loadThumbnailImageFromCache()
        {
            File thumbnailImageFile = new File(getThumbnailImageCacheFilePath());

            if(thumbnailImageFile.exists() && thumbnailImageFile.isFile())
            {
                synchronized(SYNC_OBJ)
                {
                    int imgWid = AbianReaderApplication.s_width/4;
                    int imgHei = AbianReaderApplication.s_height/4;
                    
                    m_thumbnailBitmap = AbianReaderData.decodeSampledBitmapFromFile(thumbnailImageFile.getAbsolutePath(), imgWid, imgHei);
                }

                if(m_thumbnailBitmap == null)
                {
                    Log.e(TAG, "AsyncHttp thumb bitmap decode failed");
                }

                AbianReaderApplication.getInstance().sendDataUpdatedMessage();
            }
        }

        private void loadFeaturedImageFromCache()
        {
            File featuredImageFile = new File(getFeatureImageCacheFilePath());

            if(featuredImageFile.exists() && featuredImageFile.isFile())
            {
                synchronized(SYNC_OBJ)
                {
                    int imgWid = AbianReaderApplication.s_width;
                    int imgHei = AbianReaderApplication.s_height/4;
                    
                    m_featuredImageBitmap = AbianReaderData.decodeSampledBitmapFromFile(featuredImageFile.getAbsolutePath(), imgWid, imgHei);
                }

                if(m_featuredImageBitmap == null)
                {
                    Log.e(TAG, "AsyncHttp featured bitmap decode failed, " + featuredImageFile.getAbsolutePath());
                }

                AbianReaderApplication.getInstance().sendDataUpdatedMessage();
            }
        }
        */

        public String getThumbnailLink()
        {
            if(m_thumbnailLink == null)
            {
                m_thumbnailLink = "";
            }

            return m_thumbnailLink;
        }

        public void setFeaturedImageLink(String featuredImageLink, boolean bIsFeatured)
        {
            if(featuredImageLink == null)
            {
                featuredImageLink = "";
            }

            m_featuredImageLink = featuredImageLink;

            m_bIsFeatured = bIsFeatured;

            AbianReaderApplication.getInstance().sendDataUpdatedMessage();

            /*
            if((featuredImageLink.length() > 0) && m_bIsFeatured)
            {
                // check to see if this image has been cached already
                File featuredImageFile = new File(getFeatureImageCacheFilePath());

                if(featuredImageFile.exists())
                {
                    loadFeaturedImageFromCache();
                }
                else
                {
                    String[] allowedContentTypes = new String[] { "image/png", "image/jpeg", "image/gif" };

                    m_bIsGettingFeatureImage = true;

                    AbianReaderApplication.DoHttpBinaryGet(featuredImageLink, new BinaryHttpResponseHandler(allowedContentTypes)
                    {
                        @Override
                        public void onSuccess(byte[] fileData)
                        {
                            File featureFile = new File(getFeatureImageCacheFilePath());

                            try
                            {
                                FileOutputStream featureFileOutputStream = new FileOutputStream(featureFile);
                                featureFileOutputStream.write(fileData);
                                featureFileOutputStream.flush();
                                featureFileOutputStream.close();

                                loadFeaturedImageFromCache();
                            }
                            catch(Exception e)
                            {
                                Log.e(TAG, e.toString());
                            }

                            m_bIsGettingFeatureImage = false;
                        }

                        public void onFailure(Throwable e, byte[] imageData)
                        {
                            Log.e(TAG, "AsyncHttp failed");

                            m_bIsGettingFeatureImage = false;
                        }
                    });
                }
            }
            */
        }

        public String getFeaturedImageLink()
        {
            if(m_featuredImageLink == null)
            {
                m_featuredImageLink = "";
            }

            return m_featuredImageLink;
        }

        public boolean getIsFeatured()
        {
            return m_bIsFeatured;
        }

        public Bitmap getThumbnailBitmap()
        {
            return m_thumbnailBitmap;
        }

        public void setThumbnailBitmap(Bitmap thumbnailBitmap)
        {
            m_thumbnailBitmap = thumbnailBitmap;
        }

        public Bitmap getFeaturedImageBitmap()
        {
            return m_featuredImageBitmap;
        }

        public void setFeaturedImageBitmap(Bitmap featuredImageBitmap)
        {
            m_featuredImageBitmap = featuredImageBitmap;
        }

        public void getExtraJsonData(final boolean bGetFeature)
        {
            m_bIsGettingExtraJsonData = true;

            String extraJsonDataUrl = m_itemLink;
            extraJsonDataUrl += "/?json=1&include=thumbnail,attachments";

            if(bGetFeature)
            {
                extraJsonDataUrl += ",tags";
            }

            extraJsonDataUrl += "&no_redirect=true";

            AbianReaderApplication.DoHttpGet(extraJsonDataUrl, null, new AsyncHttpResponseHandler()
            {
                @Override
                public void onSuccess(String response)
                {
                    try
                    {
                        JSONObject rootObject = new JSONObject(response);

                        JSONObject thisPostObject = rootObject.getJSONObject("post");

                        boolean bThisPostIsFeatured = false;
                        String thisPostThumbnailLink = thisPostObject.getString("thumbnail");
                        String thisFeatureImageLink = "";

                        if(thisPostObject.has("attachments"))
                        {
                            JSONArray attachmentArray = thisPostObject.getJSONArray("attachments");

                            for(int attachmentPos = 0; attachmentPos < attachmentArray.length(); attachmentPos++)
                            {
                                JSONObject thisAttachment = attachmentArray.getJSONObject(attachmentPos);

                                if(thisAttachment.has("images"))
                                {
                                    JSONObject imagesObject = thisAttachment.getJSONObject("images");

                                    JSONObject fullImageObject = imagesObject.getJSONObject("full");
                                    String thisUrl = fullImageObject.getString("url");

                                    if(thisFeatureImageLink.length() == 0)
                                    {
                                        thisFeatureImageLink = thisUrl;
                                    }
                                    else
                                    {
                                        int matchingCurrentFeature = AbianReaderDataFetcher.GetNumberOfSameCharacters(thisPostThumbnailLink, thisFeatureImageLink);
                                        int matchingThisFeature = AbianReaderDataFetcher.GetNumberOfSameCharacters(thisPostThumbnailLink, thisUrl);

                                        if(matchingThisFeature > matchingCurrentFeature)
                                        {
                                            thisFeatureImageLink = thisUrl;
                                        }
                                    }
                                }
                            }
                        }

                        if(thisPostObject.has("tags") && bGetFeature)
                        {
                            JSONArray tagArray = thisPostObject.getJSONArray("tags");

                            for(int tagPos = 0; tagPos < tagArray.length(); tagPos++)
                            {
                                JSONObject thisTag = tagArray.getJSONObject(tagPos);

                                String tagName = thisTag.getString("title");

                                if(tagName.equalsIgnoreCase(AbianReaderDataFetcher.GetFeaturedTag()))
                                {
                                    bThisPostIsFeatured = true;
                                    break;
                                }
                            }
                        }

                        setThumbnailLink(thisPostThumbnailLink);
                        setFeaturedImageLink(thisFeatureImageLink, bThisPostIsFeatured);
                    }
                    catch(Exception e)
                    {

                    }
                }
            });
        }

        public boolean getHasArticleBeenRead()
        {
            return m_bHasBeenRead;
        }

        public void setArticleHasBeenRead(boolean bSaveToDisk)
        {
            m_bHasBeenRead = true;

            if(bSaveToDisk)
            {
                AbianReaderApplication.getInstance().saveReadUrlList();
            }
        }

        public void shareItem()
        {
            AbianReaderApplication theSingleton = AbianReaderApplication.getInstance();

            String shareMessage = theSingleton.getString(R.string.share_message);
            String shareTitle = theSingleton.getString(R.string.share_title);

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareMessage);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getLink());
            theSingleton.startActivity(Intent.createChooser(sharingIntent, shareTitle));
        }
    }

    private Vector<AbianReaderItem> m_itemVector;
    private Vector<AbianReaderItem> m_newItemVector;
    private Time m_lastUpdateTime;
    private int m_pageNumber;
    private int m_autoUpdateTimeInMinutes;

    public AbianReaderData()
    {
        m_itemVector = new Vector<AbianReaderItem>();
        m_newItemVector = new Vector<AbianReaderItem>();
        m_itemVector.clear();
        m_lastUpdateTime = new Time(Time.getCurrentTimezone());
        m_lastUpdateTime.set(0);
        m_pageNumber = 1;
        m_autoUpdateTimeInMinutes = 0;
    }

    public synchronized void addItem(AbianReaderItem newItem)
    {
        //Log.e(getClass().getName(), "Enter addItem");
        
        if(Looper.myLooper() != Looper.getMainLooper())
        {
            //Log.e(getClass().getName(), "not on main thread, adding to the new item vector");
            
            m_newItemVector.add(newItem);
        }
        else
        {
            //Log.e(getClass().getName(), "on main thread, adding to the item vector");
            
            m_itemVector.add(newItem);
        }

        //Log.e(getClass().getName(), "Leave addItem");
    }

    public synchronized void syncItems()
    {
        if(Looper.myLooper() == Looper.getMainLooper())
        {
            if(m_newItemVector.size() > 0)
            {
                //Log.e(getClass().getName(), "Enter syncItems on main thread");

                //Log.e(getClass().getName(), "on main thread adding vectors");

                for(int i=0; i< m_newItemVector.size(); i++)
                {
                    m_itemVector.add(m_newItemVector.get(i));
                }

                m_newItemVector.clear();

                //Log.e(getClass().getName(), "Exit syncItems");
            }
        }
        else
        {
            Log.e(getClass().getName(), "syncItems not on main thread");
        }
    }

    public int getNumberOfItems()
    {
        return m_itemVector.size();
    }

    public AbianReaderItem getItemNumber(int itemNumber)
    {
        if((itemNumber >= 0) && (itemNumber < getNumberOfItems()))
        {
            return m_itemVector.elementAt(itemNumber);
        }

        return null;
    }

    public synchronized void clear()
    {
        m_itemVector.clear();
        m_lastUpdateTime.set(0);
        m_newItemVector.clear();
    }

    public void setLastUpdateTimeToNow()
    {
        m_lastUpdateTime.setToNow();
    }

    public void setLastUpdateTime(long lastUpdateTimeInMillis)
    {
        m_lastUpdateTime.set(lastUpdateTimeInMillis);
    }

    public Time getLastUpdateTime()
    {
        return m_lastUpdateTime;
    }

    public void setPageNumber(int newPageNumber)
    {
        if(newPageNumber < 1)
        {
            newPageNumber = 1;
        }

        m_pageNumber = newPageNumber;
    }

    public int getPageNumber()
    {
        return m_pageNumber;
    }

    public int getAutoUpdateTimeInMinutes()
    {
        return m_autoUpdateTimeInMinutes;
    }

    public void setAutoUpdateTimeInMinutes(int minutes)
    {
        if(minutes < 0)
        {
            minutes = 0;
        }

        m_autoUpdateTimeInMinutes = minutes;
    }

    public int getNumberedOfFeaturedArticles()
    {
        int retVal = 0;

        for(int i = 0; i < this.m_itemVector.size(); i++)
        {
            if(m_itemVector.get(i).getIsFeatured())
            {
                retVal++;
            }
        }

        return retVal;
    }

    public int getFeaturedArticlePosition(int featuredArticleNumber)
    {
        int featuredArticleCount = 0;

        for(int i = 0; i < m_itemVector.size(); i++)
        {
            if(m_itemVector.get(i).getIsFeatured())
            {
                if(featuredArticleNumber == featuredArticleCount)
                {
                    return i;
                }

                featuredArticleCount++;
            }
        }

        return 0;
    }

    public AbianReaderItem getFeaturedItem(int itemNumber)
    {
        if(getNumberedOfFeaturedArticles() <= 0)
        {
            return null;
        }

        if((itemNumber < 0) || (itemNumber >= getNumberedOfFeaturedArticles()))
        {
            itemNumber = 0;
        }

        return getItemNumber(getFeaturedArticlePosition(itemNumber));
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth)
        {
            if(width > height)
            {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            }
            else
            {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
}
