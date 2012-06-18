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

import com.abiansoftware.lib.reader.R;
import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

class AbianReaderListView extends LinearLayout implements OnTouchListener
{
    private static final String TAG = "AbianReaderListView";

    private ListView m_abianReaderListView;
    private AbianReaderListAdapter m_abianReaderListAdapter;

    private RelativeLayout m_headerView;
    private ImageView m_headerImageView;
    private TextView m_headerTextView;
    private TextView m_headerTitleTextView;
    private TextView m_headerCountTextView;
    private ProgressBar m_headerProgressBar;

    private int m_touchStartX;

    private Runnable m_gotoNextFeaturedArticleRunnable;

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

    private void initializeViewBeforePopulation(Context context)
    {
        m_abianReaderListView = null;
        m_abianReaderListAdapter = null;

        m_headerView = null;
        m_headerImageView = null;
        m_headerTextView = null;

        m_gotoNextFeaturedArticleRunnable = new Runnable()
        {
            public void run()
            {
                AbianReaderData theData = AbianReaderActivity.GetData();

                if(theData != null)
                {
                    theData.nextFeaturedArticle();
                    updateList();
                }
            }
        };

        m_touchStartX = 0;
    }

    public void initializeViewAfterPopulation(Context context)
    {
        AbianReaderActivity theSingleton = AbianReaderActivity.GetSingleton();

        m_abianReaderListView = (ListView)theSingleton.findViewById(R.id.abian_reader_list_view_listview);

        m_abianReaderListAdapter = new AbianReaderListAdapter(theSingleton);

        LayoutInflater theLayoutInflater = LayoutInflater.from(context);

        m_headerView = (RelativeLayout)theLayoutInflater.inflate(R.layout.abian_reader_list_header, null);
        m_headerImageView = (ImageView)m_headerView.findViewById(R.id.abian_reader_list_header_image_view);
        m_headerTextView = (TextView)m_headerView.findViewById(R.id.abian_reader_list_header_text_view);
        m_headerProgressBar = (ProgressBar)m_headerView.findViewById(R.id.abian_reader_list_header_progress_bar);
        m_headerTitleTextView = (TextView)m_headerView.findViewById(R.id.abian_reader_list_header_title_text_view);
        m_headerCountTextView = (TextView)m_headerView.findViewById(R.id.abian_reader_list_header_count_text_view);

        m_headerTitleTextView.setText(AbianReaderActivity.GetFeaturedTag().toUpperCase());

        m_headerProgressBar.setIndeterminate(true);

        m_headerProgressBar.setVisibility(View.GONE);
        m_headerView.setVisibility(View.GONE);
        m_headerView.setOnTouchListener(this);

        m_abianReaderListView.addHeaderView(m_headerView);

        ViewGroup.LayoutParams headerTitleTextLayoutParams = m_headerTitleTextView.getLayoutParams();
        headerTitleTextLayoutParams.height = ((headerTitleTextLayoutParams.height * 3) / 8);
        m_headerTitleTextView.setLayoutParams(headerTitleTextLayoutParams);

        ViewGroup.LayoutParams headerCountTextLayoutParams = m_headerCountTextView.getLayoutParams();
        headerCountTextLayoutParams.height = ((headerCountTextLayoutParams.height * 3) / 8);
        m_headerCountTextView.setLayoutParams(headerCountTextLayoutParams);

        ViewGroup.LayoutParams headerImageLayoutParams = m_headerImageView.getLayoutParams();
        headerImageLayoutParams.height = headerImageLayoutParams.height * 2;
        m_headerImageView.setLayoutParams(headerImageLayoutParams);

        ViewGroup.LayoutParams headerTextLayoutParams = m_headerTextView.getLayoutParams();
        headerTextLayoutParams.height = ((headerTextLayoutParams.height * 3) / 4);
        m_headerTextView.setLayoutParams(headerTextLayoutParams);

        m_headerView.setVisibility(View.GONE);

        // have to set the adapter after you add header/footer views
        m_abianReaderListView.setAdapter(m_abianReaderListAdapter);
    }

    @Override
    public void setVisibility(int visibility)
    {
        if(visibility == View.VISIBLE)
        {
            startNextFeatureTimer();
        }
        else
        {
            stopNextFeatureTimer();
        }

        super.setVisibility(visibility);
    }

    private void startNextFeatureTimer()
    {
        stopNextFeatureTimer();
        postDelayed(m_gotoNextFeaturedArticleRunnable, 5000);
    }

    private void stopNextFeatureTimer()
    {
        removeCallbacks(m_gotoNextFeaturedArticleRunnable);
    }

    public void updateList()
    {
        m_abianReaderListAdapter.notifyDataSetChanged();

        AbianReaderData theData = AbianReaderActivity.GetData();

        if(theData.getNumberedOfFeaturedArticles() <= 0)
        {
            m_headerView.setVisibility(View.GONE);
            m_headerImageView.setVisibility(View.GONE);
            m_headerTextView.setVisibility(View.GONE);
            m_headerCountTextView.setVisibility(View.GONE);
            m_headerTitleTextView.setVisibility(View.GONE);
        }
        else
        {
            if(theData != null)
            {
                AbianReaderItem targetItem = theData.getFeaturedItem();

                if(targetItem != null)
                {
                    startNextFeatureTimer();

                    m_headerView.setVisibility(View.VISIBLE);
                    m_headerImageView.setVisibility(View.VISIBLE);
                    m_headerTextView.setVisibility(View.VISIBLE);
                    m_headerCountTextView.setVisibility(View.VISIBLE);
                    m_headerTitleTextView.setVisibility(View.VISIBLE);

                    m_headerTextView.setText(targetItem.getTitle());

                    m_headerCountTextView.setText("" + theData.getFeaturedItemPositionInFeaturedList() + " of " + theData.getNumberedOfFeaturedArticles());

                    if(targetItem.getFeaturedImageBitmap() != null)
                    {
                        m_headerProgressBar.setVisibility(View.GONE);
                        m_headerImageView.setImageBitmap(targetItem.getFeaturedImageBitmap());

                        ViewGroup.LayoutParams headerLayoutParams = m_headerView.getLayoutParams();
                        headerLayoutParams.height = 0;
                        m_headerView.setLayoutParams(headerLayoutParams);
                    }
                    else
                    {
                        m_headerImageView.setImageBitmap(null);
                        m_headerProgressBar.setVisibility(View.VISIBLE);

                        ViewGroup.LayoutParams headerLayoutParams = m_headerView.getLayoutParams();
                        headerLayoutParams.height = (int)(AbianReaderActivity.GetSingleton().getPreferredListItemHeight() * 2.5f);
                        m_headerView.setLayoutParams(headerLayoutParams);
                    }
                }
            }
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
            int countVal = AbianReaderActivity.GetData().getNumberOfItems();

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

            AbianReaderData theData = AbianReaderActivity.GetData();

            if(position == theData.getNumberOfItems())
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

                AbianReaderItem theItem = theData.getItemNumber(position);

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

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(v.getId() == R.id.abian_reader_list_header_layout)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                stopNextFeatureTimer();
                m_touchStartX = (int)event.getX();
            }
            else if((event.getAction() == MotionEvent.ACTION_UP) || (event.getAction() == MotionEvent.ACTION_CANCEL))
            {
                int changeInX = (m_touchStartX - (int)event.getX());

                if(changeInX < -3)
                {
                    AbianReaderActivity.GetData().previousFeaturedArticle();
                    updateList();
                }
                else if(changeInX > 3)
                {
                    AbianReaderActivity.GetData().nextFeaturedArticle();
                    updateList();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    int featuredPosition = AbianReaderActivity.GetData().getFeaturedItemPositionInCompleteList();
                    AbianReaderActivity.GetSingleton().showRssItemContent(featuredPosition);
                }
                else
                {
                    startNextFeatureTimer();
                }
            }

            return true;
        }

        return false;
    }
}
