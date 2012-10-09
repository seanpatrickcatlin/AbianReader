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
import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AbianReaderListHeaderFragment extends SherlockFragment implements OnClickListener, ImageLoadingListener
{
    private boolean m_bViewHasBeenCreated;
    private RelativeLayout m_headerView;
    private ImageView m_headerImageView;
    private TextView m_headerTextView;
    private ProgressBar m_headerProgressBar;

    private int m_featuredArticleNumber;

    private static Handler s_handler = null;
    private Runnable m_updateRunnable;

    private DisplayImageOptions m_featureDisplayOptions;
    
    public AbianReaderListHeaderFragment()
    {
        super();

        m_headerView = null;
        m_headerImageView = null;
        m_headerTextView = null;
        m_headerProgressBar = null;
        m_featuredArticleNumber = -1;
        m_bViewHasBeenCreated = false;

        m_featureDisplayOptions = new DisplayImageOptions.Builder()
        .showStubImage(R.drawable.app_header_logo)
        .showImageForEmptyUri(R.drawable.app_header_logo)
        .cacheInMemory()
        .cacheOnDisc()
        .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
        .displayer(new RoundedBitmapDisplayer(0))
        .build();
        
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
        headerTextLayoutParams.height = ((headerTextLayoutParams.height * 3) / 4);
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

        if(Looper.myLooper() != Looper.getMainLooper())
        {
            // we are not on the main thread, call the runnable
            Log.e("TAG", "updateContent is not running on the main thread");
            s_handler.postDelayed(m_updateRunnable, 500);
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

                m_headerProgressBar.setVisibility(View.GONE);

                ImageLoader theImageLoader = ImageLoader.getInstance();
                theImageLoader.displayImage(theItemData.getFeaturedImageLink(), m_headerImageView, m_featureDisplayOptions, this);

                //s_handler.postDelayed(m_updateRunnable, 500);

                /*
                //if(theItemData.getFeaturedImageBitmap() == null)
                if(theItemData.getFeaturedImageLink().length() > 0)
                {
                    m_headerImageView.setImageURI(Uri.parse(theItemData.getFeaturedImageLink()));
                    m_headerProgressBar.setVisibility(View.GONE);
                    m_headerImageView.setVisibility(View.VISIBLE);
                }
                else
                {
                    m_headerImageView.setImageBitmap(null);
                    m_headerImageView.setVisibility(View.GONE);
                    m_headerProgressBar.setVisibility(View.VISIBLE);

                    s_handler.postDelayed(m_updateRunnable, 500);
                }
                */
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

        Intent itemIntent = new Intent(m_headerView.getContext(), AbianReaderItemActivity.class);

        itemIntent.putExtra(AbianReaderApplication.CHOSEN_ARTICLE_NUMBER, articlePosition);

        startActivity(itemIntent);
    }

    @Override
    public void onLoadingCancelled()
    {
        //Log.e(getClass().getName(), "onLoadingCancelled");
    }

    @Override
    public void onLoadingComplete(Bitmap arg0)
    {
        //Log.e(getClass().getName(), "onLoadingComplete");
    }

    @Override
    public void onLoadingFailed(FailReason arg0)
    {
        //Log.e(getClass().getName(), "onLoadingFailed");
    }

    @Override
    public void onLoadingStarted()
    {
        //Log.e(getClass().getName(), "onLoadingStarted");
    }
}
