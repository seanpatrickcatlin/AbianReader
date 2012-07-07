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

import com.viewpagerindicator.LinePageIndicator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class AbianReaderItemActivity extends FragmentActivity
{
    private ViewPager m_itemViewPager;
    private LinePageIndicator m_itemViewPageIndicator;
    private AbianReaderItemViewPagerAdapter m_itemViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.abian_reader_item_activity);

        m_itemViewPager = (ViewPager)findViewById(R.id.abian_reader_item_view_pager);
        m_itemViewPageIndicator = (LinePageIndicator)findViewById(R.id.abian_reader_item_view_pager_indicator);

        m_itemViewPagerAdapter = new AbianReaderItemViewPagerAdapter(getSupportFragmentManager());
        m_itemViewPager.setAdapter(m_itemViewPagerAdapter);
        m_itemViewPageIndicator.setViewPager(m_itemViewPager);
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
    }
}
