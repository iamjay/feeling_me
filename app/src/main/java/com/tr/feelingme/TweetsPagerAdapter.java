package com.tr.feelingme;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TweetsPagerAdapter extends PagerAdapter {

    private Context context;
    private String[] tweets = {};

    public TweetsPagerAdapter(Context context) {
        this.context = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.tweet_text,
                container, false);
        TextView text = (TextView)layout.findViewById(R.id.textView);
        text.setText(tweets[position]);
        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View)view);
    }

    @Override
    public int getCount() {
        return tweets.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public String[] getTweets() {
        return tweets;
    }

    public void setTweets(String[] tweets) {
        this.tweets = tweets;
        notifyDataSetChanged();
    }
}
