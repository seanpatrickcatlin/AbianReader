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

import com.abiansoftware.lib.reader.R;
import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

class AbianReaderListView extends LinearLayout
{
    private static final String TAG = "AbianReaderListView";

    private ListView m_abianReaderListView;
    private AbianReaderListAdapter m_abianReaderListAdapter;

    private RelativeLayout m_headerViewPagerLayout;
    private ViewPager m_headerViewPager;
    private AbianReaderListHeaderViewPagerAdapter m_headerViewPagerAdapter;

    private CirclePageIndicator m_pageIndicator;

    public AbianReaderListView(Context context)
    {
        super(context);

        initializeViewBeforePopulation(context);
    }

    public AbianReaderListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        initializeViewBeforePopulation(context);
    }

    public ViewPager getHeaderViewPager()
    {
        return m_headerViewPager;
    }

    private void initializeViewBeforePopulation(Context context)
    {
        m_abianReaderListView = null;
        m_abianReaderListAdapter = null;

        m_headerViewPagerLayout = null;
        m_headerViewPager = null;
        m_headerViewPagerAdapter = null;
    }

    public void initializeViewAfterPopulation(Context context)
    {
        AbianReaderActivity theSingleton = AbianReaderActivity.GetSingleton();

        m_abianReaderListView = (ListView)theSingleton.findViewById(R.id.abian_reader_list_view_listview);

        LayoutInflater theLayoutInflater = LayoutInflater.from(context);

        m_headerViewPagerLayout = (RelativeLayout)theLayoutInflater.inflate(R.layout.abian_reader_list_header_view_pager, null);

        m_headerViewPager = (ViewPager)m_headerViewPagerLayout.findViewById(R.id.abian_reader_list_header_viewpager_view);
        m_pageIndicator = (CirclePageIndicator)m_headerViewPagerLayout.findViewById(R.id.abian_reader_list_header_page_indicator);

        m_headerViewPagerAdapter = new AbianReaderListHeaderViewPagerAdapter(AbianReaderActivity.GetSingleton().getSupportFragmentManager());
        m_headerViewPager.setAdapter(m_headerViewPagerAdapter);
        m_pageIndicator.setViewPager(m_headerViewPager);

        //m_pageIndicator.setPageColor(0xFFcbedcc);
        m_pageIndicator.setFillColor(0xFF41c045);
        m_pageIndicator.setStrokeColor(0xFF41c045);
        m_pageIndicator.setSnap(true);

        ViewGroup.LayoutParams headerViewPagerLayoutParams = m_headerViewPager.getLayoutParams();
        headerViewPagerLayoutParams.height = (int)(headerViewPagerLayoutParams.height * 2.0f);
        m_headerViewPager.setLayoutParams(headerViewPagerLayoutParams);

        m_abianReaderListView.addHeaderView(m_headerViewPagerLayout);

        // have to set the adapter after you add header/footer views
        m_abianReaderListAdapter = new AbianReaderListAdapter(context);
        m_abianReaderListView.setAdapter(m_abianReaderListAdapter);
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
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

    private static class AbianReaderListAdapter extends BaseAdapter implements OnClickListener
    {
        private LayoutInflater m_layoutInflater;

        public AbianReaderListAdapter(Context context)
        {
            m_layoutInflater = LayoutInflater.from(context);
        }

        public int getCount()
        {
            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();
            
            int countVal = abianReaderAppData.getNumberOfItems();

            if((countVal < AbianReaderData.MAX_DATA_ITEMS) && (AbianReaderActivity.GetSingleton().getThereAreNoMoreItems() == false))
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
            RSSFeedListItem listItem = null;

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

            if(position == abianReaderAppData.getNumberOfItems())
            {
                AbianReaderActivity theSingleton = AbianReaderActivity.GetSingleton();

                if(theSingleton.getLastConnectionHadError())
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

                    if(theSingleton.isRefreshingFeed() == false)
                    {
                        theSingleton.getMoreFeed();
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

                if(theItem.getThumbnailBitmap() != null)
                {
                    listItem.m_imageView.setImageBitmap(theItem.getThumbnailBitmap());
                }
                else
                {
                    listItem.m_imageView.setImageResource(R.drawable.app_icon);
                }
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
                    AbianReaderActivity.GetSingleton().refreshFeed();

                    Log.e(TAG, "-1 List Item");
                }
                else if(listItem.m_targetIndex == -2)
                {
                    // this is an "updating" item
                }
                else
                {
                    AbianReaderActivity.GetSingleton().showRssItemContent(listItem.m_targetIndex);
                }
            }
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
