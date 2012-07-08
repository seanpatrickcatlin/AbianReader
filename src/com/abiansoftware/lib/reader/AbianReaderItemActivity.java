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

import com.abiansoftware.lib.reader.AbianReaderData.AbianReaderItem;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;

@SuppressLint("HandlerLeak")
public class AbianReaderItemActivity extends SherlockFragmentActivity
{
    private static int SHARE_ITEM_ID = 22611;

    private ViewPager m_itemViewPager;
    private TitlePageIndicator m_itemViewPageIndicator;
    private AbianReaderItemViewPagerAdapter m_itemViewPagerAdapter;

    private Handler m_activityHandler;

    private int m_currentPage;

    private AbianReaderItemPageListener m_itemViewPageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_activityHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what == AbianReaderApplication.MSG_DATA_UPDATED)
                {
                    m_itemViewPagerAdapter.notifyDataSetChanged();
                }
            }
        };

        int userChosenArticleNumber = 0;

        Intent callingIntent = getIntent();

        if(callingIntent != null)
        {
            Bundle intentExtras = callingIntent.getExtras();

            if(intentExtras != null)
            {
                userChosenArticleNumber = intentExtras.getInt(AbianReaderApplication.CHOSEN_ARTICLE_NUMBER, 0);
            }
        }

        m_currentPage = userChosenArticleNumber;

        setContentView(R.layout.abian_reader_item_activity);

        m_itemViewPager = (ViewPager)findViewById(R.id.abian_reader_item_view_pager);
        m_itemViewPageIndicator = (TitlePageIndicator)findViewById(R.id.abian_reader_item_view_pager_indicator);

        m_itemViewPagerAdapter = new AbianReaderItemViewPagerAdapter(getSupportFragmentManager());
        m_itemViewPager.setAdapter(m_itemViewPagerAdapter);
        m_itemViewPageIndicator.setViewPager(m_itemViewPager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setSubtitle("All Items");

        m_itemViewPager.setCurrentItem(userChosenArticleNumber);

        m_itemViewPageListener = new AbianReaderItemPageListener();
        m_itemViewPageIndicator.setOnPageChangeListener(m_itemViewPageListener);
    }

    private class AbianReaderItemViewPagerAdapter extends FragmentPagerAdapter
    {
        public AbianReaderItemViewPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0)
        {
            AbianReaderItemViewFragment thisFragment = new AbianReaderItemViewFragment();
            thisFragment.setArticleNumber(arg0);

            return thisFragment;
        }

        @Override
        public int getCount()
        {
            AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

            int currentCount = abianReaderAppData.getNumberOfItems();

            return currentCount;
        }

        @Override
        public void notifyDataSetChanged()
        {
            super.notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return "" + (position + 1) + " of " + getCount();
        }
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item)
    {
        if(item.getItemId() == android.R.id.home)
        {
            Intent intent = new Intent(this, AbianReaderActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            return true;
        }
        else if(item.getItemId() == SHARE_ITEM_ID)
        {
            String shareMessage = getString(R.string.share_message);
            String shareTitle = getString(R.string.share_title);

            AbianReaderItem targetItem = AbianReaderApplication.getData().getItemNumber(m_currentPage);

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareMessage);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, targetItem.getLink());
            startActivity(Intent.createChooser(sharingIntent, shareTitle));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem refreshMenuItem = menu.add(Menu.NONE, SHARE_ITEM_ID, Menu.NONE, "Share");
        refreshMenuItem.setIcon(R.drawable.share);
        refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    protected void onPause()
    {
        AbianReaderApplication.getInstance().unregisterHandler(m_activityHandler);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        AbianReaderApplication.getInstance().registerHandler(m_activityHandler);

        m_itemViewPagerAdapter.notifyDataSetChanged();

        if(AbianReaderApplication.getData().getNumberOfItems() == 0)
        {
            // there are no articles!!!
            // lets get out of here because we have nothing to show
            finish();
        }

        super.onResume();
    }

    private class AbianReaderItemPageListener extends SimpleOnPageChangeListener
    {
        public void onPageSelected(int position)
        {
            m_currentPage = position;
        }
    }
}
