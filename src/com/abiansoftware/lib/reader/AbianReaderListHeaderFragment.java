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

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AbianReaderListHeaderFragment extends Fragment implements OnClickListener
{
    private boolean m_bViewHasBeenCreated;
    private RelativeLayout m_headerView;
    private ImageView m_headerImageView;
    private TextView m_headerTextView;
    private ProgressBar m_headerProgressBar;

    private int m_featuredArticleNumber;

    private static Handler s_handler = null;
    private Runnable m_updateRunnable;

    public AbianReaderListHeaderFragment()
    {
        super();

        m_headerView = null;
        m_headerImageView = null;
        m_headerTextView = null;
        m_headerProgressBar = null;
        m_featuredArticleNumber = -1;
        m_bViewHasBeenCreated = false;

        if(s_handler == null)
        {
            s_handler = new Handler();
        }

        m_updateRunnable = new Runnable()
        {
            public void run()
            {
                updateContent();
            }
        };
        
    }

    public void setFeaturedArticleNumber(int featuredArticleNumber)
    {
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();
        
        if((featuredArticleNumber < 0) || (featuredArticleNumber > abianReaderAppData.getNumberedOfFeaturedArticles()))
        {
            featuredArticleNumber = 0;
        }

        m_featuredArticleNumber = featuredArticleNumber;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_headerView = (RelativeLayout)inflater.inflate(R.layout.abian_reader_list_header, container, false);

        m_headerImageView = (ImageView)m_headerView.findViewById(R.id.abian_reader_list_header_image_view);
        m_headerTextView = (TextView)m_headerView.findViewById(R.id.abian_reader_list_header_text_view);
        m_headerProgressBar = (ProgressBar)m_headerView.findViewById(R.id.abian_reader_list_header_progress_bar);

        m_headerProgressBar.setIndeterminate(true);
        m_headerProgressBar.setVisibility(View.GONE);

        ViewGroup.LayoutParams headerTextLayoutParams = m_headerTextView.getLayoutParams();
        headerTextLayoutParams.height = ((headerTextLayoutParams.height * 2) / 4);
        m_headerTextView.setLayoutParams(headerTextLayoutParams);

        m_bViewHasBeenCreated = true;
        
        updateContent();

        m_headerView.setOnClickListener(this);

        return m_headerView;
    }

    public void updateContent()
    {
        if(!m_bViewHasBeenCreated)
        {
            Log.e("...", "View has not been created");
            
            return;
        }

        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();
        
        if((m_featuredArticleNumber >= 0) && (m_featuredArticleNumber < abianReaderAppData.getNumberedOfFeaturedArticles()))
        {
            AbianReaderItem theItemData = abianReaderAppData.getFeaturedItem(m_featuredArticleNumber);

            if(theItemData != null)
            {
                m_headerTextView.setVisibility(View.VISIBLE);
                m_headerTextView.setText(theItemData.getTitle());

                if(theItemData.getFeaturedImageBitmap() == null)
                {
                    m_headerImageView.setImageBitmap(null);
                    m_headerImageView.setVisibility(View.GONE);
                    m_headerProgressBar.setVisibility(View.VISIBLE);

                    s_handler.postDelayed(m_updateRunnable, 500);
                }
                else
                {
                    m_headerImageView.setImageBitmap(theItemData.getFeaturedImageBitmap());
                    m_headerProgressBar.setVisibility(View.GONE);
                    m_headerImageView.setVisibility(View.VISIBLE);
                }
            }
            else
            {
                Log.e("...", "Item Data is null");
            }
        }        
    }

    @Override
    public void onResume()
    {
        super.onResume();

        updateContent();
    }

    @Override
    public void onClick(View v)
    {
        AbianReaderData abianReaderAppData = AbianReaderApplication.getData();

        int articlePosition = abianReaderAppData.getFeaturedArticlePosition(m_featuredArticleNumber); 

        AbianReaderActivity.GetSingleton().showRssItemContent(articlePosition);
    }
}
