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

import java.util.Vector;

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

class AbianReaderListView
{
    private static final String TAG = "AbianReaderListView";

    private int m_preferredListItemHeight;

    private ListView m_abianReaderListView;
    private AbianReaderListAdapter m_abianReaderListAdapter;

    private RelativeLayout m_headerViewPagerLayout;
    private UninterceptableViewPager m_headerViewPager;
    private AbianReaderListHeaderViewPagerAdapter m_headerViewPagerAdapter;

    private CirclePageIndicator m_pageIndicator;
    
    public AbianReaderListView()
    {
        m_preferredListItemHeight = -1;

        m_abianReaderListView = null;
        m_abianReaderListAdapter = null;

        m_headerViewPagerLayout = null;
        m_headerViewPager = null;
        m_headerViewPagerAdapter = null;
    }

    public void initializeViewAfterPopulation(FragmentActivity parentActivity)
    {
        m_abianReaderListView = (ListView)parentActivity.findViewById(R.id.abian_reader_list_view_listview);

        LayoutInflater theLayoutInflater = LayoutInflater.from(parentActivity);

        m_headerViewPagerLayout = (RelativeLayout)theLayoutInflater.inflate(R.layout.abian_reader_list_header_view_pager, null);

        m_headerViewPager = (UninterceptableViewPager)m_headerViewPagerLayout.findViewById(R.id.abian_reader_list_header_viewpager_view);
        m_pageIndicator = (CirclePageIndicator)m_headerViewPagerLayout.findViewById(R.id.abian_reader_list_header_page_indicator);

        m_headerViewPagerAdapter = new AbianReaderListHeaderViewPagerAdapter(parentActivity.getSupportFragmentManager());
        m_headerViewPager.setAdapter(m_headerViewPagerAdapter);
        m_pageIndicator.setViewPager(m_headerViewPager);

        // m_pageIndicator.setPageColor(0xFFcbedcc);

        int indicatorColor = AbianReaderApplication.getInstance().getResources().getColor(R.color.view_page_indicator_color);
        
        m_pageIndicator.setFillColor(indicatorColor);
        m_pageIndicator.setStrokeColor(indicatorColor);
        m_pageIndicator.setSnap(true);

        ViewGroup.LayoutParams headerViewPagerLayoutParams = m_headerViewPager.getLayoutParams();
        m_preferredListItemHeight = headerViewPagerLayoutParams.height;
        headerViewPagerLayoutParams.height = (int)(m_preferredListItemHeight * AbianReaderApplication.FEATURED_IMAGE_SIZE);
        m_headerViewPager.setLayoutParams(headerViewPagerLayoutParams);

        m_abianReaderListView.addHeaderView(m_headerViewPagerLayout);

        // have to set the adapter after you add header/footer views
        m_abianReaderListAdapter = new AbianReaderListAdapter(parentActivity);
        m_abianReaderListView.setAdapter(m_abianReaderListAdapter);

        m_headerViewPagerLayout.setVisibility(View.GONE);
        m_headerViewPager.setVisibility(View.GONE);
        m_pageIndicator.setVisibility(View.GONE);

        AbianReaderApplication.getInstance().registerAdapter(m_abianReaderListAdapter);
    }

    public int getPreferredListItemHeight()
    {
        return m_preferredListItemHeight;
    }

    public void updateList()
    {   
        m_abianReaderListAdapter.notifyDataSetChanged();
        m_headerViewPagerAdapter.notifyDataSetChanged();

        if(m_headerViewPagerAdapter.getCount() > 0)
        {
            m_headerViewPagerLayout.setVisibility(View.VISIBLE);
            m_headerViewPager.setVisibility(View.VISIBLE);
            m_pageIndicator.setVisibility(View.VISIBLE);
        }
        else
        {
            m_headerViewPagerLayout.setVisibility(View.GONE);
            m_headerViewPager.setVisibility(View.GONE);
            m_pageIndicator.setVisibility(View.GONE);
        }
    }

    private static class RSSFeedListItem
    {
        TextView m_titleText;
        TextView m_detailsText;
        ImageView m_imageView;
        ProgressBar m_progressBar;

        int m_targetIndex;

        public RSSFeedListItem()
        {
            resetData();
        }

        public void resetData()
        {
            m_targetIndex = 0;
        }
    }

    private static class AbianReaderListAdapter extends BaseAdapter implements OnClickListener, ImageLoadingListener
    {
        private LayoutInflater m_layoutInflater;
        private DisplayImageOptions m_thumbnailDisplayOptions;

        public AbianReaderListAdapter(Context context)
        {
            m_layoutInflater = LayoutInflater.from(context);

            m_thumbnailDisplayOptions = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.app_icon)
                .cacheInMemory()
                .cacheOnDisc()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new RoundedBitmapDisplayer(0))
                .build();
        }
        
        public int getCount()
        {
            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

            int countVal = abianReaderAppData.getNumberOfItems();

            if(countVal < AbianReaderData.MAX_DATA_ITEMS)
            {
                countVal++;
            }

            return countVal;
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            final RSSFeedListItem listItem;

            if(convertView == null)
            {
                convertView = m_layoutInflater.inflate(R.layout.abian_reader_list_item, null);

                listItem = new RSSFeedListItem();
                listItem.m_titleText = (TextView)convertView.findViewById(R.id.abian_reader_list_item_title_text_view);
                listItem.m_detailsText = (TextView)convertView.findViewById(R.id.abian_reader_list_item_details_text_view);
                listItem.m_imageView = (ImageView)convertView.findViewById(R.id.abian_reader_list_item_icon_image_view);
                listItem.m_progressBar = (ProgressBar)convertView.findViewById(R.id.abian_reader_list_item_progress_bar);

                convertView.setTag(listItem);
            }
            else
            {
                listItem = (RSSFeedListItem)convertView.getTag();
                listItem.resetData();
            }

            convertView.setBackgroundResource(R.drawable.list_item_unread);

            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

            AbianReaderDataFetcher abianReaderAppDataFetcher = AbianReaderApplication.getDataFetcher();

            if(position == abianReaderAppData.getNumberOfItems())
            {
                if(abianReaderAppDataFetcher.getLastConnectionHadError())
                {
                    listItem.m_titleText.setVisibility(View.VISIBLE);
                    listItem.m_detailsText.setVisibility(View.VISIBLE);
                    listItem.m_imageView.setVisibility(View.VISIBLE);
                    listItem.m_progressBar.setVisibility(View.GONE);

                    listItem.m_targetIndex = -1;
                    listItem.m_titleText.setText("Unable to retrieve articles");
                    listItem.m_detailsText.setText("Tap here to try again");
                    listItem.m_imageView.setImageResource(R.drawable.refresh_dark);

                    convertView.setBackgroundResource(R.drawable.list_item_read);
                }
                else
                {
                    listItem.m_progressBar.setVisibility(View.VISIBLE);
                    listItem.m_titleText.setVisibility(View.VISIBLE);
                    listItem.m_detailsText.setVisibility(View.GONE);
                    listItem.m_imageView.setVisibility(View.GONE);
                    listItem.m_titleText.setText("Updating...");
                    convertView.setBackgroundResource(R.drawable.list_item_read);

                    listItem.m_targetIndex = -2;

                    if(abianReaderAppDataFetcher.isRefreshingFeed() == false)
                    {
                        abianReaderAppDataFetcher.getMoreFeed();
                    }
                }
            }
            else
            {
                listItem.m_progressBar.setVisibility(View.GONE);
                listItem.m_titleText.setVisibility(View.VISIBLE);
                listItem.m_detailsText.setVisibility(View.VISIBLE);
                listItem.m_imageView.setVisibility(View.VISIBLE);

                AbianReaderItem theItem = abianReaderAppData.getItemNumber(position);

                if(theItem.getHasArticleBeenRead())
                {
                    convertView.setBackgroundResource(R.drawable.list_item_read);
                }
                else
                {
                    convertView.setBackgroundResource(R.drawable.list_item_unread);
                }

                listItem.m_targetIndex = position;
                listItem.m_titleText.setText(theItem.getTitle());

                String detailsText = "By ";
                detailsText += theItem.getCreator();
                detailsText += "\n";
                detailsText += theItem.getPubDateOnly();
                detailsText += ", ";

                if(theItem.getCommentsCount() == 0)
                {
                    detailsText += "No";
                }
                else
                {
                    detailsText += Integer.toString(theItem.getCommentsCount());
                }

                detailsText += " Comment";

                if(theItem.getCommentsCount() != 1)
                {
                    detailsText += "s";
                }

                listItem.m_detailsText.setText(detailsText);

                ImageLoader theImageLoader = ImageLoader.getInstance();
                theImageLoader.displayImage(theItem.getThumbnailLink(), listItem.m_imageView, m_thumbnailDisplayOptions, this);
            }

            convertView.setOnClickListener(this);
            convertView.setTag(listItem);

            return convertView;
        }

        public void onClick(View v)
        {
            RSSFeedListItem listItem = (RSSFeedListItem)v.getTag();

            if(listItem != null)
            {
                if(listItem.m_targetIndex == -1)
                {
                    // this is a retry
                    AbianReaderDataFetcher abianReaderAppDataFetcher = AbianReaderApplication.getDataFetcher();

                    if(abianReaderAppDataFetcher != null)
                    {
                        abianReaderAppDataFetcher.refreshFeed();
                    }

                    Log.e(TAG, "-1 List Item");
                }
                else if(listItem.m_targetIndex == -2)
                {
                    // this is an "updating" item
                }
                else
                {
                    Intent itemIntent = new Intent(m_layoutInflater.getContext(), AbianReaderItemActivity.class);

                    itemIntent.putExtra(AbianReaderApplication.CHOSEN_ARTICLE_NUMBER, listItem.m_targetIndex);

                    m_layoutInflater.getContext().startActivity(itemIntent);
                }
            }
        }

        @Override
        public void onLoadingCancelled()
        {
            //Log.e(this.getClass().toString(), "onLoadingCancelled");
            //
        }

        @Override
        public void onLoadingComplete(Bitmap arg0)
        {
            //notifyDataSetChanged();
            //Log.e(this.getClass().toString(), "onLoadingComplete");
        }

        @Override
        public void onLoadingFailed(FailReason arg0)
        {
            //
            //Log.e(this.getClass().toString(), "onLoadingFailed");
        }

        @Override
        public void onLoadingStarted()
        {
            //
            //Log.e(this.getClass().toString(), "onLoadingStarted");
        }
    }

    private class AbianReaderListHeaderViewPagerAdapter extends FragmentPagerAdapter
    {
        private Vector<AbianReaderListHeaderFragment> m_fragmentVector;

        public AbianReaderListHeaderViewPagerAdapter(FragmentManager fm)
        {
            super(fm);

            m_fragmentVector = new Vector<AbianReaderListHeaderFragment>();
        }

        @Override
        public Fragment getItem(int arg0)
        {
            AbianReaderListHeaderFragment thisFragment = new AbianReaderListHeaderFragment();
            thisFragment.setFeaturedArticleNumber(arg0);

            m_fragmentVector.add(thisFragment);

            return thisFragment;
        }

        @Override
        public int getCount()
        {
            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

            int currentCount = abianReaderAppData.getNumberedOfFeaturedArticles();

            return currentCount;
        }

        @Override
        public void notifyDataSetChanged()
        {
            for(int i = 0; i < m_fragmentVector.size(); i++)
            {
                AbianReaderListHeaderFragment thisFragment = m_fragmentVector.get(i);

                if(thisFragment != null)
                {
                    if(thisFragment.isVisible())
                    {
                        thisFragment.updateContent();
                    }

                    if(thisFragment.isDetached())
                    {
                        m_fragmentVector.remove(i);
                        i = -1;
                    }
                }
                else
                {
                    m_fragmentVector.remove(i);
                    i = -1;
                }
            }

            super.notifyDataSetChanged();
        }
    }    
}
