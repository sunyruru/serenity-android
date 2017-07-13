/**
 * The MIT License (MIT)
 * Copyright (c) 2013 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity.ui.browser.tv;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import net.ganin.darv.DpadAwareRecyclerView;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.core.model.SeriesContentInfo;
import us.nineworlds.serenity.jobs.TVShowRetrievalJob;
import us.nineworlds.serenity.ui.adapters.AbstractPosterImageGalleryAdapter;
import us.nineworlds.serenity.ui.util.ImageUtils;

public class TVShowRecyclerAdapter extends AbstractPosterImageGalleryAdapter {

    @Inject
    JobManager jobManager;

    private static final int BANNER_PIXEL_HEIGHT = 140;
    private static final int BANNER_PIXEL_WIDTH = 758;

    protected List<SeriesContentInfo> tvShowList = new ArrayList<>();

    private final String key;

    public TVShowRecyclerAdapter(String key, String category) {
        super(key, category);
        this.key = key;
        fetchData();
    }

    protected void fetchData() {
        TVShowRetrievalJob tvShowRetrievalJob = new TVShowRetrievalJob(key, category);
        jobManager.addJobInBackground(tvShowRetrievalJob);
    }

    public int getItemCount() {
        return tvShowList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0) {
            position = 0;
        }
        return tvShowList.get(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.poster_tvshow_indicator_view, parent, false);
        return new TVShowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SeriesContentInfo pi = tvShowList.get(position);
        createImage(holder.itemView, pi, BANNER_PIXEL_WIDTH, BANNER_PIXEL_HEIGHT);
        toggleWatchedIndicator(holder.itemView, pi);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected void createImage(View galleryCellView, SeriesContentInfo pi, int imageWidth, int imageHeight) {
        int width = ImageUtils.getDPI(imageWidth, (Activity) galleryCellView.getContext());
        int height = ImageUtils.getDPI(imageHeight, (Activity) galleryCellView.getContext());

        initPosterMetaData(galleryCellView, pi, width, height, false);

        galleryCellView.setLayoutParams(new DpadAwareRecyclerView.LayoutParams(width,
                height));
    }

    protected void initPosterMetaData(View galleryCellView, SeriesContentInfo pi, int width, int height, boolean isPoster) {
        ImageView mpiv = (ImageView) galleryCellView.findViewById(R.id.posterImageView);
        mpiv.setBackgroundResource(R.drawable.gallery_item_background);
        mpiv.setScaleType(ImageView.ScaleType.FIT_XY);
        mpiv.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
        mpiv.setMaxHeight(height);
        mpiv.setMaxWidth(width);

        if (isPoster) {
            Glide.with(mpiv.getContext()).load(pi.getThumbNailURL()).into(mpiv);
        } else {
            Glide.with(mpiv.getContext()).load(pi.getImageURL()).into(mpiv);
        }
    }

    protected void toggleWatchedIndicator(View galleryCellView, SeriesContentInfo pi) {
        int watched = 0;
        if (pi.getShowsWatched() != null) {
            watched = Integer.parseInt(pi.getShowsWatched());
        }

        ImageView watchedView = (ImageView) galleryCellView.findViewById(R.id.posterWatchedIndicator);
        watchedView.setVisibility(View.INVISIBLE);

        if (pi.isPartiallyWatched()) {
            toggleProgressIndicator(galleryCellView, watched, pi.totalShows(),
                    watchedView);
        }

        TextView badgeCount = (TextView) galleryCellView.findViewById(R.id.badge_count);
        badgeCount.setText(pi.getShowsUnwatched());

        if (pi.isWatched()) {
            watchedView.setImageResource(R.drawable.overlaywatched);
            watchedView.setVisibility(View.VISIBLE);
            badgeCount.setVisibility(View.GONE);
        } else {
            badgeCount.setVisibility(View.VISIBLE);
        }
    }

    protected void toggleProgressIndicator(View galleryCellView, int dividend, int divisor, ImageView watchedView) {
        final float percentWatched = Float.valueOf(dividend) / Float.valueOf(divisor);

        final ProgressBar view = (ProgressBar) galleryCellView
                .findViewById(R.id.posterInprogressIndicator);
        int progress = Float.valueOf(percentWatched * 100).intValue();
        if (progress < 10) {
            progress = 10;
        }
        view.setProgress(progress);
        view.setVisibility(View.VISIBLE);
        watchedView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void fetchDataFromService() {

    }

    public void updateSeries(List<SeriesContentInfo> series) {
        tvShowList = series;
        notifyDataSetChanged();
    }


    public class TVShowViewHolder extends RecyclerView.ViewHolder {

        public TVShowViewHolder(View itemView) {
            super(itemView);
        }
    }
}