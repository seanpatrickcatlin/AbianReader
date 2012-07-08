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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

public class AbianReaderItemActivity extends SherlockFragmentActivity
{
    private static int SHARE_ITEM_ID = 22611;
    
    private ViewPager m_itemViewPager;
    private TitlePageIndicator m_itemViewPageIndicator;
    private AbianReaderItemViewPagerAdapter m_itemViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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

        setContentView(R.layout.abian_reader_item_activity);

        m_itemViewPager = (ViewPager)findViewById(R.id.abian_reader_item_view_pager);
        m_itemViewPageIndicator = (TitlePageIndicator)findViewById(R.id.abian_reader_item_view_pager_indicator);

        m_itemViewPagerAdapter = new AbianReaderItemViewPagerAdapter(getSupportFragmentManager());
        m_itemViewPager.setAdapter(m_itemViewPagerAdapter);
        m_itemViewPageIndicator.setViewPager(m_itemViewPager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setSubtitle("All Items");

        m_itemViewPager.setCurrentItem(userChosenArticleNumber);
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
            return "" + (position+1) + " of " + getCount();
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
            Toast newToast = Toast.makeText(getApplicationContext(), "Share Coming Soon", Toast.LENGTH_SHORT);
            newToast.show();

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
}
