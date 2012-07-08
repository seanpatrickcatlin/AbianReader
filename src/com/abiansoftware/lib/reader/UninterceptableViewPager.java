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

// This class has been adapted from http://stackoverflow.com/questions/2646028/android-horizontalscrollview-within-scrollview-touch-handling

package com.abiansoftware.lib.reader;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class UninterceptableViewPager extends ViewPager
{
    public UninterceptableViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        // Tell our parent to stop intercepting our events!
        boolean ret = super.onInterceptTouchEvent(ev);

        if(ret)
        {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        return ret;
    }
}
